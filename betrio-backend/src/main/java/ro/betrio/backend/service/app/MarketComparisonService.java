package ro.betrio.backend.service.app;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.betrio.backend.api.dto.app.FixtureMarketComparisonDto;
import ro.betrio.backend.domain.entity.Fixture;
import ro.betrio.backend.domain.entity.OddsSnapshot;
import ro.betrio.backend.domain.entity.PredictionRun;
import ro.betrio.backend.repository.FixtureRepository;
import ro.betrio.backend.repository.OddsSnapshotRepository;
import ro.betrio.backend.repository.PredictionRunRepository;

@Service
public class MarketComparisonService {

    private final FixtureRepository fixtureRepository;
    private final PredictionRunRepository predictionRunRepository;
    private final OddsSnapshotRepository oddsSnapshotRepository;

    public MarketComparisonService(
            FixtureRepository fixtureRepository,
            PredictionRunRepository predictionRunRepository,
            OddsSnapshotRepository oddsSnapshotRepository) {
        this.fixtureRepository = fixtureRepository;
        this.predictionRunRepository = predictionRunRepository;
        this.oddsSnapshotRepository = oddsSnapshotRepository;
    }

    @Transactional(readOnly = true)
    public FixtureMarketComparisonDto getMarketComparison(Long fixtureId) {
        Fixture fixture = fixtureRepository.findDetailedById(fixtureId)
                .orElseThrow(() -> new IllegalStateException("Fixture not found: " + fixtureId));

        PredictionRun latestPrediction = predictionRunRepository
                .findTopByFixtureIdOrderByGeneratedAtDesc(fixtureId)
                .orElseThrow(() -> new IllegalStateException("No prediction run found for fixture: " + fixtureId));

        List<OddsSnapshot> allSnapshots = oddsSnapshotRepository.findByFixtureIdOrderByCapturedAtDesc(fixtureId);

        if (allSnapshots.isEmpty()) {
            return new FixtureMarketComparisonDto(
                    fixture.getId(),
                    fixture.getHomeTeam().getTeamName(),
                    fixture.getAwayTeam().getTeamName(),
                    fixture.getKickoffAt(),
                    new FixtureMarketComparisonDto.ModelProbabilities(
                            latestPrediction.getHomeWinProbability(),
                            latestPrediction.getDrawProbability(),
                            latestPrediction.getAwayWinProbability()
                    ),
                    null,
                    null,
                    null,
                    null
            );
        }

        OffsetDateTime latestCapturedAt = allSnapshots.get(0).getCapturedAt();

        List<OddsSnapshot> latestBatch = allSnapshots.stream()
                .filter(s -> latestCapturedAt.equals(s.getCapturedAt()))
                .filter(this::isOneXTwoMarket)
                .toList();

        Map<String, OddsSnapshot> selectionMap = latestBatch.stream()
                .collect(Collectors.toMap(
                        this::normalizeOutcome,
                        s -> s,
                        (a, b) -> a
                ));

        OddsSnapshot homeSnapshot = selectionMap.get("HOME");
        OddsSnapshot drawSnapshot = selectionMap.get("DRAW");
        OddsSnapshot awaySnapshot = selectionMap.get("AWAY");

        if (homeSnapshot == null || drawSnapshot == null || awaySnapshot == null) {
            return new FixtureMarketComparisonDto(
                    fixture.getId(),
                    fixture.getHomeTeam().getTeamName(),
                    fixture.getAwayTeam().getTeamName(),
                    fixture.getKickoffAt(),
                    new FixtureMarketComparisonDto.ModelProbabilities(
                            latestPrediction.getHomeWinProbability(),
                            latestPrediction.getDrawProbability(),
                            latestPrediction.getAwayWinProbability()
                    ),
                    null,
                    null,
                    null,
                    latestCapturedAt
            );
        }

        double homeOdds = homeSnapshot.getOddValue();
        double drawOdds = drawSnapshot.getOddValue();
        double awayOdds = awaySnapshot.getOddValue();

        double rawHome = 1.0 / homeOdds;
        double rawDraw = 1.0 / drawOdds;
        double rawAway = 1.0 / awayOdds;

        double sum = rawHome + rawDraw + rawAway;

        double normalizedHome = rawHome / sum;
        double normalizedDraw = rawDraw / sum;
        double normalizedAway = rawAway / sum;

        double modelHome = latestPrediction.getHomeWinProbability();
        double modelDraw = latestPrediction.getDrawProbability();
        double modelAway = latestPrediction.getAwayWinProbability();

        double edgeHome = modelHome - normalizedHome;
        double edgeDraw = modelDraw - normalizedDraw;
        double edgeAway = modelAway - normalizedAway;

        String bestEdgeSelection = bestEdge(edgeHome, edgeDraw, edgeAway);

        return new FixtureMarketComparisonDto(
                fixture.getId(),
                fixture.getHomeTeam().getTeamName(),
                fixture.getAwayTeam().getTeamName(),
                fixture.getKickoffAt(),
                new FixtureMarketComparisonDto.ModelProbabilities(
                        modelHome,
                        modelDraw,
                        modelAway
                ),
                new FixtureMarketComparisonDto.MarketOdds(
                        homeOdds,
                        drawOdds,
                        awayOdds,
                        rawHome,
                        rawDraw,
                        rawAway,
                        normalizedHome,
                        normalizedDraw,
                        normalizedAway
                ),
                new FixtureMarketComparisonDto.EdgeSummary(
                        edgeHome,
                        edgeDraw,
                        edgeAway
                ),
                bestEdgeSelection,
                latestCapturedAt
        );
    }

    private boolean isOneXTwoMarket(OddsSnapshot snapshot) {
        String marketName = safe(snapshot.getMarketName());
        return marketName.contains("match winner")
                || marketName.equals("1x2")
                || marketName.equals("match result");
    }

    private String normalizeOutcome(OddsSnapshot snapshot) {
        String outcome = safe(snapshot.getOutcomeName());

        if (outcome.equals("1") || outcome.equals("home")) {
            return "HOME";
        }

        if (outcome.equals("x") || outcome.equals("draw") || outcome.equals("tie")) {
            return "DRAW";
        }

        if (outcome.equals("2") || outcome.equals("away")) {
            return "AWAY";
        }

        return "OTHER";
    }

    private String bestEdge(double home, double draw, double away) {
        double max = Math.max(home, Math.max(draw, away));

        if (max <= 0.0) {
            return null;
        }

        if (max == home) {
            return "HOME";
        }
        if (max == away) {
            return "AWAY";
        }
        return "DRAW";
    }

    private String safe(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}