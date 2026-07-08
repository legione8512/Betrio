package ro.betrio.backend.service.app;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import ro.betrio.backend.api.dto.app.FixtureMarketComparisonDto;
import ro.betrio.backend.api.dto.app.FixtureOverviewDto;
import ro.betrio.backend.api.dto.app.FixtureRecommendationDto;
import ro.betrio.backend.api.dto.app.PredictionExplanationDto;
import ro.betrio.backend.domain.entity.Fixture;
import ro.betrio.backend.domain.entity.OddsSnapshot;
import ro.betrio.backend.domain.entity.PredictionExactScore;
import ro.betrio.backend.domain.entity.PredictionRun;
import ro.betrio.backend.repository.PredictionExactScoreRepository;

@Service
public class AppRecommendationService {

    private final AppFixtureService appFixtureService;
    private final MarketComparisonService marketComparisonService;
    private final PredictionExactScoreRepository predictionExactScoreRepository;
    private final PredictionExplanationService predictionExplanationService;

    public AppRecommendationService(
            AppFixtureService appFixtureService,
            MarketComparisonService marketComparisonService,
            PredictionExactScoreRepository predictionExactScoreRepository,
            PredictionExplanationService predictionExplanationService) {

        this.appFixtureService = appFixtureService;
        this.marketComparisonService = marketComparisonService;
        this.predictionExactScoreRepository =
                predictionExactScoreRepository;
        this.predictionExplanationService =
                predictionExplanationService;
    }

    /*
     * Folosit de endpointul pentru un singur fixture.
     */
    public FixtureRecommendationDto getRecommendation(Long fixtureId) {
        try {
            FixtureOverviewDto overview =
                    appFixtureService.getFixtureOverview(fixtureId);

            if (overview == null || overview.latestPrediction() == null) {
                return unavailableRecommendation(
                        fixtureId,
                        safeHomeTeamName(overview),
                        safeAwayTeamName(overview),
                        "No prediction data available for this fixture."
                );
            }

            PredictionExplanationDto explanation =
                    predictionExplanationService.build(overview);

            FixtureMarketComparisonDto marketComparison =
                    safeMarketComparison(fixtureId);

            FixtureRecommendationDto.ExactScorePick topExactScore =
                    extractTopExactScore(
                            overview.latestPrediction().predictionRunId()
                    );

            return buildRecommendation(
                    fixtureId,
                    overview,
                    explanation,
                    marketComparison,
                    topExactScore
            );
        } catch (Exception ex) {
            logFailure("Single recommendation", fixtureId, ex);

            return unavailableRecommendation(
                    fixtureId,
                    null,
                    null,
                    "Recommendation failed for fixtureId=" + fixtureId
            );
        }
    }

    /*
     * Folosit de Upcoming Picks.
     *
     * Nu mai execută interogări individuale. Primește fixture-ul,
     * predicția, cotele și scorul exact deja încărcate batch.
     */
    public FixtureRecommendationDto getRecommendation(
            Fixture fixture,
            PredictionRun latestPrediction,
            List<OddsSnapshot> oddsSnapshots,
            PredictionExactScore topExactScoreEntity) {

        Long fixtureId = fixture != null ? fixture.getId() : null;

        try {
            if (fixture == null) {
                return unavailableRecommendation(
                        null,
                        null,
                        null,
                        "Fixture data is not available."
                );
            }

            if (latestPrediction == null) {
                return unavailableRecommendation(
                        fixtureId,
                        homeTeamName(fixture),
                        awayTeamName(fixture),
                        "No prediction data available for this fixture."
                );
            }

            FixtureOverviewDto overview =
                    toOverview(fixture, latestPrediction);

            PredictionExplanationDto explanation =
                    predictionExplanationService.build(overview);

            List<OddsSnapshot> safeOddsSnapshots =
                    oddsSnapshots != null
                            ? oddsSnapshots
                            : List.of();

            FixtureMarketComparisonDto marketComparison =
                    marketComparisonService.buildMarketComparison(
                            fixture,
                            latestPrediction,
                            safeOddsSnapshots
                    );

            FixtureRecommendationDto.ExactScorePick topExactScore =
                    toExactScorePick(topExactScoreEntity);

            return buildRecommendation(
                    fixtureId,
                    overview,
                    explanation,
                    marketComparison,
                    topExactScore
            );
        } catch (Exception ex) {
            logFailure("Batch recommendation", fixtureId, ex);

            return unavailableRecommendation(
                    fixtureId,
                    fixture != null ? homeTeamName(fixture) : null,
                    fixture != null ? awayTeamName(fixture) : null,
                    "Recommendation failed for fixtureId=" + fixtureId
            );
        }
    }

    private FixtureRecommendationDto buildRecommendation(
            Long fixtureId,
            FixtureOverviewDto overview,
            PredictionExplanationDto explanation,
            FixtureMarketComparisonDto marketComparison,
            FixtureRecommendationDto.ExactScorePick topExactScore) {

        String homeTeamName = safeHomeTeamName(overview);
        String awayTeamName = safeAwayTeamName(overview);

        if (overview == null
                || overview.latestPrediction() == null
                || explanation == null) {

            return unavailableRecommendation(
                    fixtureId,
                    homeTeamName,
                    awayTeamName,
                    "No prediction data available for this fixture."
            );
        }

        String recommendedResultCode =
                explanation.recommendedResultCode();

        Double confidenceScore =
                explanation.topProbability();

        String confidenceTier =
                explanation.confidenceTier();

        String riskTag =
                toRiskTag(confidenceTier);

        String recommendationType =
                toRecommendationType(
                        confidenceTier,
                        marketComparison,
                        recommendedResultCode
                );

        FixtureRecommendationDto.MarketSignal marketSignal =
                buildMarketSignal(
                        marketComparison,
                        recommendedResultCode
                );

        String summary = buildSummary(
                overview,
                explanation,
                recommendationType,
                topExactScore,
                marketSignal
        );

        return new FixtureRecommendationDto(
                fixtureId,
                homeTeamName,
                awayTeamName,
                true,
                null,
                recommendationType,
                recommendedResultCode,
                confidenceScore,
                confidenceTier,
                riskTag,
                explanation.overUnderLean(),
                explanation.bttsLean(),
                topExactScore,
                marketSignal,
                summary
        );
    }

    private FixtureOverviewDto toOverview(
            Fixture fixture,
            PredictionRun prediction) {

        boolean finished = isFinished(fixture);

        FixtureOverviewDto.TeamSummary homeTeam =
                new FixtureOverviewDto.TeamSummary(
                        fixture.getHomeTeam().getId(),
                        fixture.getHomeTeam().getTeamName()
                );

        FixtureOverviewDto.TeamSummary awayTeam =
                new FixtureOverviewDto.TeamSummary(
                        fixture.getAwayTeam().getId(),
                        fixture.getAwayTeam().getTeamName()
                );

        FixtureOverviewDto.ScoreSummary score =
                new FixtureOverviewDto.ScoreSummary(
                        fixture.getHomeGoals(),
                        fixture.getAwayGoals(),
                        fixture.getHalftimeHomeGoals(),
                        fixture.getHalftimeAwayGoals(),
                        fixture.getFulltimeHomeGoals(),
                        fixture.getFulltimeAwayGoals()
                );

        FixtureOverviewDto.PredictionSummary predictionSummary =
                new FixtureOverviewDto.PredictionSummary(
                        prediction.getId(),
                        prediction.getGeneratedAt(),
                        prediction.getHomeWinProbability(),
                        prediction.getDrawProbability(),
                        prediction.getAwayWinProbability(),
                        prediction.getOver25Probability(),
                        prediction.getUnder25Probability(),
                        prediction.getBttsYesProbability(),
                        prediction.getBttsNoProbability(),
                        recommendedResultCode(prediction)
                );

        return new FixtureOverviewDto(
                fixture.getId(),
                fixture.getKickoffAt(),
                fixture.getStatusShort(),
                fixture.getStatusLong(),
                fixture.getLeagueRound(),
                fixture.getVenueName(),
                fixture.getVenueCity(),
                homeTeam,
                awayTeam,
                score,
                predictionSummary,
                null,
                null,
                finished,
                true,
                finished
        );
    }

    private FixtureMarketComparisonDto safeMarketComparison(
            Long fixtureId) {

        try {
            return marketComparisonService
                    .getMarketComparison(fixtureId);
        } catch (Exception ex) {
            return null;
        }
    }

    private FixtureRecommendationDto.ExactScorePick extractTopExactScore(
            Long predictionRunId) {

        if (predictionRunId == null) {
            return null;
        }

        PredictionExactScore topScore =
                predictionExactScoreRepository
                        .findByPredictionRunIdOrderByProbabilityDesc(
                                predictionRunId
                        )
                        .stream()
                        .findFirst()
                        .orElse(null);

        return toExactScorePick(topScore);
    }

    private FixtureRecommendationDto.ExactScorePick toExactScorePick(
            PredictionExactScore score) {

        if (score == null) {
            return null;
        }

        return new FixtureRecommendationDto.ExactScorePick(
                score.getHomeGoals(),
                score.getAwayGoals(),
                score.getProbability()
        );
    }

    private FixtureRecommendationDto.MarketSignal buildMarketSignal(
            FixtureMarketComparisonDto marketComparison,
            String recommendedResultCode) {

        if (marketComparison == null
                || marketComparison.market() == null
                || marketComparison.edges() == null) {

            return new FixtureRecommendationDto.MarketSignal(
                    false,
                    null,
                    null,
                    false
            );
        }

        Double edgeOnRecommendedSelection =
                edgeForSelection(
                        marketComparison,
                        recommendedResultCode
                );

        boolean valueCandidate =
                edgeOnRecommendedSelection != null
                        && edgeOnRecommendedSelection > 0.0;

        return new FixtureRecommendationDto.MarketSignal(
                true,
                marketComparison.bestEdgeSelection(),
                edgeOnRecommendedSelection,
                valueCandidate
        );
    }

    private Double edgeForSelection(
            FixtureMarketComparisonDto marketComparison,
            String selection) {

        if (selection == null
                || marketComparison == null
                || marketComparison.edges() == null) {

            return null;
        }

        return switch (selection) {
            case "HOME" -> marketComparison.edges().homeEdge();
            case "DRAW" -> marketComparison.edges().drawEdge();
            case "AWAY" -> marketComparison.edges().awayEdge();
            default -> null;
        };
    }

    private String toRecommendationType(
            String confidenceTier,
            FixtureMarketComparisonDto marketComparison,
            String recommendedResultCode) {

        Double edge = edgeForSelection(
                marketComparison,
                recommendedResultCode
        );

        if (edge != null && edge > 0.0) {
            return "VALUE_PLAY";
        }

        if ("HIGH".equals(confidenceTier)) {
            return "PRIMARY_LEAN";
        }

        if ("MEDIUM".equals(confidenceTier)) {
            return "SMALL_LEAN";
        }

        return "NO_BET";
    }

    private String toRiskTag(String confidenceTier) {
        if ("HIGH".equals(confidenceTier)) {
            return "LOW_RISK";
        }

        if ("MEDIUM".equals(confidenceTier)) {
            return "MEDIUM_RISK";
        }

        return "HIGH_RISK";
    }

    private String recommendedResultCode(PredictionRun prediction) {
        double home = prediction.getHomeWinProbability();
        double draw = prediction.getDrawProbability();
        double away = prediction.getAwayWinProbability();

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

        return "FT".equals(status)
                || "AET".equals(status)
                || "PEN".equals(status);
    }

    private String safeHomeTeamName(FixtureOverviewDto overview) {
        if (overview == null || overview.homeTeam() == null) {
            return null;
        }

        return overview.homeTeam().name();
    }

    private String safeAwayTeamName(FixtureOverviewDto overview) {
        if (overview == null || overview.awayTeam() == null) {
            return null;
        }

        return overview.awayTeam().name();
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

    private FixtureRecommendationDto unavailableRecommendation(
            Long fixtureId,
            String homeTeamName,
            String awayTeamName,
            String reason) {

        return new FixtureRecommendationDto(
                fixtureId,
                homeTeamName,
                awayTeamName,
                false,
                reason,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private String buildSummary(
            FixtureOverviewDto overview,
            PredictionExplanationDto explanation,
            String recommendationType,
            FixtureRecommendationDto.ExactScorePick topExactScore,
            FixtureRecommendationDto.MarketSignal marketSignal) {

        String homeTeamName = safeHomeTeamName(overview);
        String awayTeamName = safeAwayTeamName(overview);

        StringBuilder summary = new StringBuilder();

        summary.append("Betrio pick for ")
                .append(homeTeamName != null
                        ? homeTeamName
                        : "Unknown Home")
                .append(" vs ")
                .append(awayTeamName != null
                        ? awayTeamName
                        : "Unknown Away")
                .append(": ")
                .append(explanation.recommendedResultCode() != null
                        ? explanation.recommendedResultCode()
                        : "NO_BET")
                .append(" (")
                .append(recommendationType != null
                        ? recommendationType
                        : "NO_BET")
                .append("). ");

        if (explanation.confidenceTier() != null
                && explanation.topProbability() != null) {

            summary.append("Confidence is ")
                    .append(explanation.confidenceTier())
                    .append(" at ")
                    .append(String.format(
                            Locale.US,
                            "%.1f",
                            explanation.topProbability() * 100.0
                    ))
                    .append("%. ");
        }

        if (explanation.overUnderLean() != null) {
            summary.append("Totals lean ")
                    .append(explanation.overUnderLean())
                    .append(". ");
        }

        if (explanation.bttsLean() != null) {
            summary.append("BTTS leans ")
                    .append(explanation.bttsLean())
                    .append(". ");
        }

        if (topExactScore != null
                && topExactScore.homeGoals() != null
                && topExactScore.awayGoals() != null) {

            summary.append("Top exact score is ")
                    .append(topExactScore.homeGoals())
                    .append("-")
                    .append(topExactScore.awayGoals());

            if (topExactScore.probability() != null) {
                summary.append(" at ")
                        .append(String.format(
                                Locale.US,
                                "%.1f",
                                topExactScore.probability() * 100.0
                        ))
                        .append("%");
            }

            summary.append(". ");
        }

        if (marketSignal != null
                && marketSignal.marketComparisonAvailable()) {

            if (marketSignal.valueCandidate()) {
                summary.append(
                        "Market comparison suggests positive edge "
                                + "on the recommended selection."
                );
            } else {
                summary.append(
                        "No confirmed positive market edge "
                                + "on the recommended selection."
                );
            }
        } else {
            summary.append("No market comparison available.");
        }

        return summary.toString().trim();
    }

    private void logFailure(
            String operation,
            Long fixtureId,
            Exception exception) {

        System.out.println(
                operation
                        + " failed for fixtureId="
                        + fixtureId
                        + " -> "
                        + exception.getClass().getName()
                        + ": "
                        + exception.getMessage()
        );
    }
}