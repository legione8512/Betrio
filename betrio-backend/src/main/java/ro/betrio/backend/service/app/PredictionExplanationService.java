package ro.betrio.backend.service.app;

import org.springframework.stereotype.Service;

import ro.betrio.backend.api.dto.app.FixtureOverviewDto;
import ro.betrio.backend.api.dto.app.PredictionExplanationDto;

@Service
public class PredictionExplanationService {

    public PredictionExplanationDto build(
            FixtureOverviewDto overview) {

        if (overview == null
                || overview.latestPrediction() == null) {

            return null;
        }

        FixtureOverviewDto.PredictionSummary prediction =
                overview.latestPrediction();

        double home =
                safe(prediction.homeWinProbability());

        double draw =
                safe(prediction.drawProbability());

        double away =
                safe(prediction.awayWinProbability());

        double top =
                Math.max(home, Math.max(draw, away));

        String confidenceTier;

        if (top >= 0.65) {
            confidenceTier = "HIGH";
        } else if (top >= 0.50) {
            confidenceTier = "MEDIUM";
        } else {
            confidenceTier = "LOW";
        }

        String overUnderLean = null;

        if (prediction.over25Probability() != null
                && prediction.under25Probability() != null) {

            overUnderLean =
                    prediction.over25Probability()
                            >= prediction.under25Probability()
                            ? "OVER_25"
                            : "UNDER_25";
        }

        String bttsLean = null;

        if (prediction.bttsYesProbability() != null
                && prediction.bttsNoProbability() != null) {

            bttsLean =
                    prediction.bttsYesProbability()
                            >= prediction.bttsNoProbability()
                            ? "BTTS_YES"
                            : "BTTS_NO";
        }

        String summary = buildSummary(
                prediction.recommendedResultCode(),
                top,
                confidenceTier,
                overUnderLean,
                bttsLean
        );

        return new PredictionExplanationDto(
                prediction.recommendedResultCode(),
                top,
                confidenceTier,
                overUnderLean,
                bttsLean,
                summary
        );
    }

    private String buildSummary(
            String recommendedResultCode,
            double topProbability,
            String confidenceTier,
            String overUnderLean,
            String bttsLean) {

        String resultPart =
                "Model leans "
                        + nullSafe(recommendedResultCode)
                        + " at "
                        + String.format(
                                java.util.Locale.US,
                                "%.1f",
                                topProbability * 100.0
                        )
                        + "% confidence ("
                        + confidenceTier
                        + ").";

        String totalsPart =
                overUnderLean == null
                        ? ""
                        : " Totals lean "
                                + overUnderLean
                                + ".";

        String bttsPart =
                bttsLean == null
                        ? ""
                        : " BTTS leans "
                                + bttsLean
                                + ".";

        return resultPart + totalsPart + bttsPart;
    }

    private double safe(Double value) {
        return value == null ? 0.0 : value;
    }

    private String nullSafe(String value) {
        return value == null ? "UNKNOWN" : value;
    }
}