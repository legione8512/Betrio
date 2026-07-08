package ro.betrio.backend.service.app;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.betrio.backend.api.dto.app.FixtureRecommendationDto;
import ro.betrio.backend.api.dto.app.UpcomingPicksDto;
import ro.betrio.backend.domain.entity.Fixture;
import ro.betrio.backend.domain.entity.OddsSnapshot;
import ro.betrio.backend.domain.entity.PredictionExactScore;
import ro.betrio.backend.domain.entity.PredictionRun;
import ro.betrio.backend.repository.FixtureRepository;
import ro.betrio.backend.repository.OddsSnapshotRepository;
import ro.betrio.backend.repository.PredictionExactScoreRepository;
import ro.betrio.backend.repository.PredictionRunRepository;

@Service
public class AppUpcomingPicksService {

    private final FixtureRepository fixtureRepository;
    private final PredictionRunRepository predictionRunRepository;
    private final OddsSnapshotRepository oddsSnapshotRepository;
    private final PredictionExactScoreRepository
            predictionExactScoreRepository;
    private final AppRecommendationService appRecommendationService;

    public AppUpcomingPicksService(
            FixtureRepository fixtureRepository,
            PredictionRunRepository predictionRunRepository,
            OddsSnapshotRepository oddsSnapshotRepository,
            PredictionExactScoreRepository predictionExactScoreRepository,
            AppRecommendationService appRecommendationService) {

        this.fixtureRepository = fixtureRepository;
        this.predictionRunRepository = predictionRunRepository;
        this.oddsSnapshotRepository = oddsSnapshotRepository;
        this.predictionExactScoreRepository =
                predictionExactScoreRepository;
        this.appRecommendationService = appRecommendationService;
    }

    @Transactional(readOnly = true)
    public UpcomingPicksDto getUpcomingPicks(
            Long competitionId,
            int limit) {

        int safeLimit = Math.max(1, Math.min(limit, 50));

        List<Fixture> upcomingFixtures =
                competitionId == null
                        ? fixtureRepository.findUpcomingWithTeams(
                                PageRequest.of(0, safeLimit))
                        : fixtureRepository
                                .findUpcomingWithTeamsByCompetition(
                                        competitionId,
                                        PageRequest.of(0, safeLimit));

        if (upcomingFixtures.isEmpty()) {
            return emptyResponse(safeLimit);
        }

        /*
         * 1. Extragem ID-urile fixture-urilor.
         */
        List<Long> fixtureIds = upcomingFixtures.stream()
                .map(Fixture::getId)
                .toList();

        /*
         * 2. Încărcăm ultima predicție pentru toate fixture-urile
         * printr-o singură interogare.
         */
        List<PredictionRun> latestPredictions =
                predictionRunRepository
                        .findLatestForFixtureIds(fixtureIds);

        Map<Long, PredictionRun> predictionByFixtureId =
                latestPredictions.stream()
                        .collect(Collectors.toMap(
                                prediction ->
                                        prediction.getFixture().getId(),
                                Function.identity(),
                                this::newerPrediction
                        ));

        /*
         * 3. Extragem ID-urile prediction run-urilor disponibile.
         */
        List<Long> predictionRunIds = latestPredictions.stream()
                .map(PredictionRun::getId)
                .toList();

        /*
         * 4. Încărcăm toate scorurile exacte într-o singură
         * interogare și păstrăm scorul cu probabilitatea cea mai mare.
         */
        Map<Long, PredictionExactScore> exactScoreByPredictionRunId =
                loadTopExactScores(predictionRunIds);

        /*
         * 5. Încărcăm toate cotele într-o singură interogare
         * și le grupăm după fixture.
         */
        List<OddsSnapshot> allOddsSnapshots =
                oddsSnapshotRepository
                        .findByFixtureIdInOrderByCapturedAtDesc(
                                fixtureIds);

        Map<Long, List<OddsSnapshot>> oddsByFixtureId =
                allOddsSnapshots.stream()
                        .collect(Collectors.groupingBy(
                                snapshot ->
                                        snapshot.getFixture().getId()
                        ));

        /*
         * 6. Construim recomandările folosind datele deja încărcate.
         * În această buclă nu mai executăm interogări individuale.
         */
        List<UpcomingPicksDto.PickItem> picks =
                new ArrayList<>();

        for (Fixture fixture : upcomingFixtures) {
            try {
                PredictionRun latestPrediction =
                        predictionByFixtureId.get(fixture.getId());

                PredictionExactScore topExactScore =
                        latestPrediction == null
                                ? null
                                : exactScoreByPredictionRunId.get(
                                        latestPrediction.getId());

                List<OddsSnapshot> fixtureOdds =
                        oddsByFixtureId.getOrDefault(
                                fixture.getId(),
                                List.of()
                        );

                FixtureRecommendationDto recommendation =
                        appRecommendationService.getRecommendation(
                                fixture,
                                latestPrediction,
                                fixtureOdds,
                                topExactScore
                        );

                picks.add(toPickItem(
                        fixture,
                        recommendation
                ));
            } catch (Exception ex) {
                logFailure(fixture.getId(), ex);

                picks.add(toUnavailablePickItem(
                        fixture,
                        "Recommendation failed for fixtureId="
                                + fixture.getId()
                ));
            }
        }

        int availableCount = (int) picks.stream()
                .filter(UpcomingPicksDto.PickItem::available)
                .count();

        return new UpcomingPicksDto(
                OffsetDateTime.now(),
                safeLimit,
                picks.size(),
                availableCount,
                picks
        );
    }

    private Map<Long, PredictionExactScore> loadTopExactScores(
            List<Long> predictionRunIds) {

        if (predictionRunIds.isEmpty()) {
            return Map.of();
        }

        List<PredictionExactScore> exactScores =
                predictionExactScoreRepository
                        .findByPredictionRunIdIn(
                                predictionRunIds);

        return exactScores.stream()
                .collect(Collectors.toMap(
                        score ->
                                score.getPredictionRun().getId(),
                        Function.identity(),
                        this::higherProbabilityScore
                ));
    }

    private PredictionRun newerPrediction(
            PredictionRun first,
            PredictionRun second) {

        if (first.getGeneratedAt() == null) {
            return second;
        }

        if (second.getGeneratedAt() == null) {
            return first;
        }

        int dateComparison = first.getGeneratedAt()
                .compareTo(second.getGeneratedAt());

        if (dateComparison > 0) {
            return first;
        }

        if (dateComparison < 0) {
            return second;
        }

        return first.getId() >= second.getId()
                ? first
                : second;
    }

    private PredictionExactScore higherProbabilityScore(
            PredictionExactScore first,
            PredictionExactScore second) {

        if (first.getProbability() == null) {
            return second;
        }

        if (second.getProbability() == null) {
            return first;
        }

        int probabilityComparison =
                first.getProbability()
                        .compareTo(second.getProbability());

        if (probabilityComparison > 0) {
            return first;
        }

        if (probabilityComparison < 0) {
            return second;
        }

        return first.getId() >= second.getId()
                ? first
                : second;
    }

    private UpcomingPicksDto.PickItem toPickItem(
            Fixture fixture,
            FixtureRecommendationDto recommendation) {

        String summary =
                recommendation.summary() != null
                        ? recommendation.summary()
                        : recommendation.reason();

        return new UpcomingPicksDto.PickItem(
                fixture.getId(),
                fixture.getKickoffAt(),
                homeTeamName(fixture),
                awayTeamName(fixture),
                recommendation.available(),
                recommendation.recommendationType(),
                recommendation.recommendedResultCode(),
                recommendation.confidenceScore(),
                recommendation.confidenceTier(),
                recommendation.riskTag(),
                recommendation.overUnderLean(),
                recommendation.bttsLean(),
                summary
        );
    }

    private UpcomingPicksDto.PickItem toUnavailablePickItem(
            Fixture fixture,
            String summary) {

        return new UpcomingPicksDto.PickItem(
                fixture.getId(),
                fixture.getKickoffAt(),
                homeTeamName(fixture),
                awayTeamName(fixture),
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                summary
        );
    }

    private UpcomingPicksDto emptyResponse(int safeLimit) {
        return new UpcomingPicksDto(
                OffsetDateTime.now(),
                safeLimit,
                0,
                0,
                List.of()
        );
    }

    private String homeTeamName(Fixture fixture) {
        return fixture.getHomeTeam() != null
                ? fixture.getHomeTeam().getTeamName()
                : null;
    }

    private String awayTeamName(Fixture fixture) {
        return fixture.getAwayTeam() != null
                ? fixture.getAwayTeam().getTeamName()
                : null;
    }

    private void logFailure(
            Long fixtureId,
            Exception exception) {

        System.out.println(
                "Upcoming pick failed for fixtureId="
                        + fixtureId
                        + " -> "
                        + exception.getClass().getName()
                        + ": "
                        + exception.getMessage()
        );
    }
}