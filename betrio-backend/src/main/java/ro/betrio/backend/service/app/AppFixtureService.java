package ro.betrio.backend.service.app;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import ro.betrio.backend.api.dto.app.FixtureListItemDto;
import ro.betrio.backend.api.dto.app.FixtureOverviewDto;
import ro.betrio.backend.api.dto.app.PagedResponseDto;
import ro.betrio.backend.domain.entity.Fixture;
import ro.betrio.backend.domain.entity.ManualActionRun;
import ro.betrio.backend.domain.entity.PredictionEvaluation;
import ro.betrio.backend.domain.entity.PredictionRun;
import ro.betrio.backend.repository.FixtureRepository;
import ro.betrio.backend.repository.ManualActionRunRepository;
import ro.betrio.backend.repository.PredictionEvaluationRepository;
import ro.betrio.backend.repository.PredictionRunRepository;

@Service
public class AppFixtureService {

    private final FixtureRepository fixtureRepository;
    private final PredictionRunRepository predictionRunRepository;
    private final PredictionEvaluationRepository predictionEvaluationRepository;
    private final ManualActionRunRepository manualActionRunRepository;

    public AppFixtureService(
            FixtureRepository fixtureRepository,
            PredictionRunRepository predictionRunRepository,
            PredictionEvaluationRepository predictionEvaluationRepository,
            ManualActionRunRepository manualActionRunRepository) {
        this.fixtureRepository = fixtureRepository;
        this.predictionRunRepository = predictionRunRepository;
        this.predictionEvaluationRepository = predictionEvaluationRepository;
        this.manualActionRunRepository = manualActionRunRepository;
    }
    
    @Transactional(readOnly = true)
    public PagedResponseDto<FixtureListItemDto> getFixturesPage(
            String query,
            String status,
            Long teamId,
            Long competitionId,
            String round,
            OffsetDateTime fromDate,
            OffsetDateTime toDate,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        Sort sort = buildSort(sortBy, sortDir);

        Specification<Fixture> spec = Specification
                .where(FixtureSpecifications.queryContains(query))
                .and(FixtureSpecifications.hasStatus(status))
                .and(FixtureSpecifications.hasTeamId(teamId))
                .and(FixtureSpecifications.hasCompetitionId(competitionId))
                .and(FixtureSpecifications.hasRound(round))
                .and(FixtureSpecifications.kickoffFrom(fromDate))
                .and(FixtureSpecifications.kickoffTo(toDate));

        Page<Fixture> fixturePage = fixtureRepository.findAll(
                spec,
                PageRequest.of(page, size, sort)
        );

        List<FixtureListItemDto> items = fixturePage.getContent()
                .stream()
                .map(this::toListItemDto)
                .toList();

        return new PagedResponseDto<>(
                items,
                fixturePage.getNumber(),
                fixturePage.getSize(),
                fixturePage.getTotalElements(),
                fixturePage.getTotalPages(),
                fixturePage.hasNext(),
                fixturePage.hasPrevious(),
                normalizeSortBy(sortBy),
                "asc".equalsIgnoreCase(sortDir) ? "asc" : "desc"
        );
    }
    
    private Sort buildSort(String sortBy, String sortDir) {
        String normalizedSortBy = normalizeSortBy(sortBy);
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return Sort.by(direction, normalizedSortBy);
    }

    private String normalizeSortBy(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "kickoffAt";
        }

        return switch (sortBy) {
            case "id" -> "id";
            case "kickoffAt" -> "kickoffAt";
            case "statusShort" -> "statusShort";
            case "leagueRound" -> "leagueRound";
            case "homeGoals" -> "homeGoals";
            case "awayGoals" -> "awayGoals";
            default -> "kickoffAt";
        };
    }

    @Transactional(readOnly = true)
    public List<FixtureListItemDto> getUpcomingFixtures(int limit) {
        return fixtureRepository.findUpcomingWithTeams(PageRequest.of(0, limit))
                .stream()
                .map(this::toListItemDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FixtureListItemDto> getRecentFixtures(int limit) {
        return fixtureRepository.findRecentWithTeams(PageRequest.of(0, limit))
                .stream()
                .map(this::toListItemDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FixtureListItemDto> searchFixtures(
            String query,
            String status,
            OffsetDateTime fromDate,
            OffsetDateTime toDate,
            int limit) {

        var pageable = PageRequest.of(0, limit);

        List<Fixture> fixtures;

        if (fromDate == null && toDate == null) {
            fixtures = fixtureRepository.searchWithTeamsNoDate(query, status, pageable);
        } else if (fromDate != null && toDate == null) {
            fixtures = fixtureRepository.searchWithTeamsFromDate(query, status, fromDate, pageable);
        } else if (fromDate == null) {
            fixtures = fixtureRepository.searchWithTeamsToDate(query, status, toDate, pageable);
        } else {
            fixtures = fixtureRepository.searchWithTeamsBetweenDates(query, status, fromDate, toDate, pageable);
        }

        return fixtures.stream()
                .map(this::toListItemDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public FixtureOverviewDto getFixtureOverview(Long fixtureId) {
        Fixture fixture = fixtureRepository.findDetailedById(fixtureId)
                .orElseThrow(() -> new IllegalStateException("Fixture not found: " + fixtureId));

        Optional<PredictionRun> latestPredictionOpt =
                predictionRunRepository.findTopByFixtureIdOrderByGeneratedAtDesc(fixtureId);

        Optional<PredictionEvaluation> latestEvaluationOpt = latestPredictionOpt
                .flatMap(run -> predictionEvaluationRepository.findByPredictionRunId(run.getId()));

        Optional<ManualActionRun> latestActionOpt = manualActionRunRepository
                .findByFixtureIdOrderByStartedAtDesc(fixtureId, PageRequest.of(0, 1))
                .stream()
                .findFirst();

        boolean finished = isFinished(fixture);

        return new FixtureOverviewDto(
                fixture.getId(),
                fixture.getKickoffAt(),
                fixture.getStatusShort(),
                fixture.getStatusLong(),
                fixture.getLeagueRound(),
                fixture.getVenueName(),
                fixture.getVenueCity(),
                new FixtureOverviewDto.TeamSummary(
                        fixture.getHomeTeam().getId(),
                        fixture.getHomeTeam().getTeamName()
                ),
                new FixtureOverviewDto.TeamSummary(
                        fixture.getAwayTeam().getId(),
                        fixture.getAwayTeam().getTeamName()
                ),
                new FixtureOverviewDto.ScoreSummary(
                        fixture.getHomeGoals(),
                        fixture.getAwayGoals(),
                        fixture.getHalftimeHomeGoals(),
                        fixture.getHalftimeAwayGoals(),
                        fixture.getFulltimeHomeGoals(),
                        fixture.getFulltimeAwayGoals()
                ),
                latestPredictionOpt.map(this::toPredictionSummary).orElse(null),
                latestEvaluationOpt.map(this::toEvaluationSummary).orElse(null),
                latestActionOpt.map(this::toActionSummary).orElse(null),
                finished,
                true,
                finished
        );
    }

    private FixtureListItemDto toListItemDto(Fixture fixture) {
        return new FixtureListItemDto(
                fixture.getId(),
                fixture.getKickoffAt(),
                fixture.getStatusShort(),
                fixture.getStatusLong(),
                fixture.getLeagueRound(),
                fixture.getHomeTeam().getId(),
                fixture.getHomeTeam().getTeamName(),
                fixture.getHomeGoals(),
                fixture.getAwayTeam().getId(),
                fixture.getAwayTeam().getTeamName(),
                fixture.getAwayGoals()
        );
    }

    private FixtureOverviewDto.PredictionSummary toPredictionSummary(PredictionRun run) {
        return new FixtureOverviewDto.PredictionSummary(
                run.getId(),
                run.getGeneratedAt(),
                run.getHomeWinProbability(),
                run.getDrawProbability(),
                run.getAwayWinProbability(),
                run.getOver25Probability(),
                run.getUnder25Probability(),
                run.getBttsYesProbability(),
                run.getBttsNoProbability(),
                recommendedResultCode(run)
        );
    }

    private FixtureOverviewDto.EvaluationSummary toEvaluationSummary(PredictionEvaluation evaluation) {
        return new FixtureOverviewDto.EvaluationSummary(
                evaluation.getId(),
                evaluation.getPredictedResultCode(),
                evaluation.getActualResultCode(),
                evaluation.getActualHomeGoals(),
                evaluation.getActualAwayGoals(),
                evaluation.getHit1x2(),
                evaluation.getHitOver25(),
                evaluation.getHitBtts(),
                evaluation.getTopExactScoreHit(),
                evaluation.getBrierScore1x2(),
                evaluation.getLogLoss1x2()
        );
    }

    private FixtureOverviewDto.ActionSummary toActionSummary(ManualActionRun run) {
        return new FixtureOverviewDto.ActionSummary(
                run.getId(),
                run.getActionKey(),
                run.getStatus(),
                run.getStartedAt(),
                run.getFinishedAt(),
                run.getMessage()
        );
    }

    private String recommendedResultCode(PredictionRun run) {
        double home = run.getHomeWinProbability();
        double draw = run.getDrawProbability();
        double away = run.getAwayWinProbability();

        if (home >= draw && home >= away) {
            return "HOME";
        }
        if (away >= home && away >= draw) {
            return "AWAY";
        }
        return "DRAW";
    }

    private boolean isFinished(Fixture fixture) {
        String status = fixture.getStatusShort();
        return "FT".equals(status) || "AET".equals(status) || "PEN".equals(status);
    }
}