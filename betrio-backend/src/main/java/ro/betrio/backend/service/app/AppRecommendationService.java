package ro.betrio.backend.service.app;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import ro.betrio.backend.api.dto.app.FixtureMatchCenterDto;
import ro.betrio.backend.api.dto.app.FixtureRecommendationDto;
import ro.betrio.backend.api.dto.app.FixtureOverviewDto;
import ro.betrio.backend.api.dto.app.PredictionExplanationDto;

@Service
public class AppRecommendationService {

    private final AppMatchCenterService appMatchCenterService;

    public AppRecommendationService(AppMatchCenterService appMatchCenterService) {
        this.appMatchCenterService = appMatchCenterService;
    }

    public FixtureRecommendationDto getRecommendation(Long fixtureId) {
        try {
            FixtureMatchCenterDto matchCenter = appMatchCenterService.getMatchCenter(fixtureId, 5, 5);

            if (matchCenter == null) {
                return unavailableRecommendation(
                        fixtureId,
                        null,
                        null,
                        "No match center data available for this fixture."
                );
            }

            FixtureOverviewDto overview = matchCenter.overview();
            PredictionExplanationDto explanation = matchCenter.predictionExplanation();

            String homeTeamName = safeHomeTeamName(overview);
            String awayTeamName = safeAwayTeamName(overview);

            if (overview == null || explanation == null || overview.latestPrediction() == null) {
                return unavailableRecommendation(
                        fixtureId,
                        homeTeamName,
                        awayTeamName,
                        "No prediction data available for this fixture."
                );
            }

            String recommendedResultCode = explanation.recommendedResultCode();
            Double confidenceScore = explanation.topProbability();
            String confidenceTier = explanation.confidenceTier();
            String riskTag = toRiskTag(confidenceTier);
            String recommendationType = toRecommendationType(
                    confidenceTier,
                    matchCenter.marketComparison(),
                    recommendedResultCode
            );

            FixtureRecommendationDto.ExactScorePick topExactScore = extractTopExactScore(matchCenter.prediction());

            FixtureRecommendationDto.MarketSignal marketSignal = buildMarketSignal(
                    matchCenter,
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
        } catch (Exception ex) {
            System.out.println("Recommendation failed for fixtureId=" + fixtureId
                    + " -> " + ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace();

            return unavailableRecommendation(
                    fixtureId,
                    null,
                    null,
                    "Recommendation failed for fixtureId=" + fixtureId
            );
        }
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

    private FixtureRecommendationDto.MarketSignal buildMarketSignal(
            FixtureMatchCenterDto matchCenter,
            String recommendedResultCode) {

        if (matchCenter == null || matchCenter.marketComparison() == null) {
            return new FixtureRecommendationDto.MarketSignal(false, null, null, false);
        }

        var marketComparison = matchCenter.marketComparison();

        if (marketComparison.market() == null || marketComparison.edges() == null) {
            return new FixtureRecommendationDto.MarketSignal(false, null, null, false);
        }

        Double edgeOnRecommendedSelection = edgeForSelection(
                marketComparison,
                recommendedResultCode
        );

        boolean valueCandidate = edgeOnRecommendedSelection != null && edgeOnRecommendedSelection > 0.0;

        return new FixtureRecommendationDto.MarketSignal(
                true,
                marketComparison.bestEdgeSelection(),
                edgeOnRecommendedSelection,
                valueCandidate
        );
    }

    private Double edgeForSelection(
            ro.betrio.backend.api.dto.app.FixtureMarketComparisonDto marketComparison,
            String selection) {

        if (selection == null || marketComparison == null || marketComparison.edges() == null) {
            return null;
        }

        return switch (selection) {
            case "HOME" -> marketComparison.edges().homeEdge();
            case "DRAW" -> marketComparison.edges().drawEdge();
            case "AWAY" -> marketComparison.edges().awayEdge();
            default -> null;
        };
    }

    private String toRiskTag(String confidenceTier) {
        if ("HIGH".equals(confidenceTier)) {
            return "LOW_RISK";
        }
        if ("MEDIUM".equals(confidenceTier)) {
            return "MEDIUM_RISK";
        }
        if ("LOW".equals(confidenceTier)) {
            return "HIGH_RISK";
        }
        return "HIGH_RISK";
    }

    private String toRecommendationType(
            String confidenceTier,
            ro.betrio.backend.api.dto.app.FixtureMarketComparisonDto marketComparison,
            String recommendedResultCode) {

        boolean hasPositiveEdge = false;

        if (marketComparison != null && marketComparison.edges() != null && recommendedResultCode != null) {
            Double edge = edgeForSelection(marketComparison, recommendedResultCode);
            hasPositiveEdge = edge != null && edge > 0.0;
        }

        if (hasPositiveEdge) {
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

    private FixtureRecommendationDto.ExactScorePick extractTopExactScore(Object predictionObject) {
        if (!(predictionObject instanceof Map<?, ?> predictionMap)) {
            return null;
        }

        Object topExactScoresObject = predictionMap.get("topExactScores");
        if (!(topExactScoresObject instanceof List<?> topExactScores) || topExactScores.isEmpty()) {
            return null;
        }

        Object firstObject = topExactScores.get(0);
        if (!(firstObject instanceof Map<?, ?> firstMap)) {
            return null;
        }

        Integer homeGoals = asInteger(firstMap.get("homeGoals"));
        Integer awayGoals = asInteger(firstMap.get("awayGoals"));
        Double probability = asDouble(firstMap.get("probability"));

        return new FixtureRecommendationDto.ExactScorePick(
                homeGoals,
                awayGoals,
                probability
        );
    }

    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
    }

    private Double asDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.valueOf(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
    }

    private String buildSummary(
            FixtureOverviewDto overview,
            PredictionExplanationDto explanation,
            String recommendationType,
            FixtureRecommendationDto.ExactScorePick topExactScore,
            FixtureRecommendationDto.MarketSignal marketSignal) {

        String homeTeamName = safeHomeTeamName(overview);
        String awayTeamName = safeAwayTeamName(overview);

        StringBuilder sb = new StringBuilder();

        sb.append("Betrio pick for ")
          .append(homeTeamName != null ? homeTeamName : "Unknown Home")
          .append(" vs ")
          .append(awayTeamName != null ? awayTeamName : "Unknown Away")
          .append(": ")
          .append(explanation.recommendedResultCode() != null ? explanation.recommendedResultCode() : "NO_BET")
          .append(" (")
          .append(recommendationType != null ? recommendationType : "NO_BET")
          .append("). ");

        if (explanation.confidenceTier() != null && explanation.topProbability() != null) {
            sb.append("Confidence is ")
              .append(explanation.confidenceTier())
              .append(" at ")
              .append(String.format(java.util.Locale.US, "%.1f", explanation.topProbability() * 100.0))
              .append("%. ");
        }

        if (explanation.overUnderLean() != null) {
            sb.append("Totals lean ").append(explanation.overUnderLean()).append(". ");
        }

        if (explanation.bttsLean() != null) {
            sb.append("BTTS leans ").append(explanation.bttsLean()).append(". ");
        }

        if (topExactScore != null && topExactScore.homeGoals() != null && topExactScore.awayGoals() != null) {
            sb.append("Top exact score is ")
              .append(topExactScore.homeGoals())
              .append("-")
              .append(topExactScore.awayGoals());

            if (topExactScore.probability() != null) {
                sb.append(" at ")
                  .append(String.format(java.util.Locale.US, "%.1f", topExactScore.probability() * 100.0))
                  .append("%");
            }

            sb.append(". ");
        }

        if (marketSignal != null && marketSignal.marketComparisonAvailable()) {
            if (marketSignal.valueCandidate()) {
                sb.append("Market comparison suggests positive edge on the recommended selection.");
            } else {
                sb.append("No confirmed positive market edge on the recommended selection.");
            }
        } else {
            sb.append("No market comparison available.");
        }

        return sb.toString().trim();
    }
}