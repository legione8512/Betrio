package ro.betrio.backend.service.app;

import java.util.function.Supplier;

import org.springframework.stereotype.Service;

import ro.betrio.backend.api.dto.app.FixtureH2HDto;
import ro.betrio.backend.api.dto.app.FixtureMarketComparisonDto;
import ro.betrio.backend.api.dto.app.FixtureMatchCenterDto;
import ro.betrio.backend.api.dto.app.FixtureOverviewDto;
import ro.betrio.backend.api.dto.app.PredictionExplanationDto;
import ro.betrio.backend.api.dto.app.TeamFormDto;

@Service
public class AppMatchCenterService {

    private final AppFixtureService appFixtureService;
    private final AppH2HService appH2HService;
    private final MarketComparisonService marketComparisonService;
    private final AppTeamService appTeamService;
    private final AppInternalApiClient appInternalApiClient;

    public AppMatchCenterService(
            AppFixtureService appFixtureService,
            AppH2HService appH2HService,
            MarketComparisonService marketComparisonService,
            AppTeamService appTeamService,
            AppInternalApiClient appInternalApiClient) {
        this.appFixtureService = appFixtureService;
        this.appH2HService = appH2HService;
        this.marketComparisonService = marketComparisonService;
        this.appTeamService = appTeamService;
        this.appInternalApiClient = appInternalApiClient;
    }

    public FixtureMatchCenterDto getMatchCenter(Long fixtureId, int h2hLimit, int formLimit) {
        int safeH2hLimit = Math.max(1, Math.min(h2hLimit, 20));
        int safeFormLimit = Math.max(1, Math.min(formLimit, 20));

        FixtureOverviewDto overview = safeCall(
                () -> appFixtureService.getFixtureOverview(fixtureId),
                "overview",
                fixtureId
        );

        FixtureH2HDto h2h = safeCall(
                () -> appH2HService.getFixtureH2H(fixtureId, safeH2hLimit),
                "h2h",
                fixtureId
        );

        FixtureMarketComparisonDto marketComparison = safeCall(
                () -> marketComparisonService.getMarketComparison(fixtureId),
                "marketComparison",
                fixtureId
        );

        TeamFormDto homeTeamForm = null;
        TeamFormDto awayTeamForm = null;

        if (overview != null && overview.homeTeam() != null && overview.homeTeam().id() != null) {
            Long homeTeamId = overview.homeTeam().id();
            homeTeamForm = safeCall(
                    () -> appTeamService.getTeamForm(homeTeamId, safeFormLimit),
                    "homeTeamForm",
                    fixtureId
            );
        }

        if (overview != null && overview.awayTeam() != null && overview.awayTeam().id() != null) {
            Long awayTeamId = overview.awayTeam().id();
            awayTeamForm = safeCall(
                    () -> appTeamService.getTeamForm(awayTeamId, safeFormLimit),
                    "awayTeamForm",
                    fixtureId
            );
        }

        Object features = safeCall(
                () -> appInternalApiClient.getFeatures(fixtureId),
                "features",
                fixtureId
        );

        Object prediction = safeCall(
                () -> appInternalApiClient.getPrediction(fixtureId),
                "prediction",
                fixtureId
        );

        PredictionExplanationDto predictionExplanation = safeCall(
                () -> buildPredictionExplanation(overview),
                "predictionExplanation",
                fixtureId
        );

        return new FixtureMatchCenterDto(
                fixtureId,
                overview,
                h2h,
                marketComparison,
                homeTeamForm,
                awayTeamForm,
                features,
                prediction,
                predictionExplanation
        );
    }

    private <T> T safeCall(Supplier<T> supplier, String label, Long fixtureId) {
        try {
            return supplier.get();
        } catch (Exception ex) {
            System.out.println("MatchCenter " + label + " failed for fixtureId="
                    + fixtureId + " -> " + ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace();
            return null;
        }
    }

    private PredictionExplanationDto buildPredictionExplanation(FixtureOverviewDto overview) {
        if (overview == null || overview.latestPrediction() == null) {
            return null;
        }

        FixtureOverviewDto.PredictionSummary p = overview.latestPrediction();

        double home = safe(p.homeWinProbability());
        double draw = safe(p.drawProbability());
        double away = safe(p.awayWinProbability());

        double top = Math.max(home, Math.max(draw, away));

        String confidenceTier;
        if (top >= 0.65) {
            confidenceTier = "HIGH";
        } else if (top >= 0.50) {
            confidenceTier = "MEDIUM";
        } else {
            confidenceTier = "LOW";
        }

        String overUnderLean = null;
        if (p.over25Probability() != null && p.under25Probability() != null) {
            overUnderLean = p.over25Probability() >= p.under25Probability() ? "OVER_25" : "UNDER_25";
        }

        String bttsLean = null;
        if (p.bttsYesProbability() != null && p.bttsNoProbability() != null) {
            bttsLean = p.bttsYesProbability() >= p.bttsNoProbability() ? "BTTS_YES" : "BTTS_NO";
        }

        String summary = buildSummary(
                p.recommendedResultCode(),
                top,
                confidenceTier,
                overUnderLean,
                bttsLean
        );

        return new PredictionExplanationDto(
                p.recommendedResultCode(),
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

        String resultPart = "Model leans " + nullSafe(recommendedResultCode)
                + " at " + String.format(java.util.Locale.US, "%.1f", topProbability * 100.0)
                + "% confidence (" + confidenceTier + ").";

        String totalsPart = overUnderLean == null
                ? ""
                : " Totals lean " + overUnderLean + ".";

        String bttsPart = bttsLean == null
                ? ""
                : " BTTS leans " + bttsLean + ".";

        return resultPart + totalsPart + bttsPart;
    }

    private double safe(Double value) {
        return value == null ? 0.0 : value;
    }

    private String nullSafe(String value) {
        return value == null ? "UNKNOWN" : value;
    }
}