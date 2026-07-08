package ro.betrio.backend.service.app;

import java.util.function.Supplier;
import ro.betrio.backend.api.dto.MatchFeatureSnapshotDto;
import ro.betrio.backend.api.dto.MatchPredictionDto;
import ro.betrio.backend.service.analysis.FeatureBuilderService;
import ro.betrio.backend.service.prediction.ProbabilityEngineService;

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
    private final FeatureBuilderService featureBuilderService;
    private final ProbabilityEngineService probabilityEngineService;
    private final PredictionExplanationService
    predictionExplanationService;
    
    public AppMatchCenterService(
            AppFixtureService appFixtureService,
            AppH2HService appH2HService,
            MarketComparisonService marketComparisonService,
            AppTeamService appTeamService,
            FeatureBuilderService featureBuilderService,
            ProbabilityEngineService probabilityEngineService,
            PredictionExplanationService predictionExplanationService) {
        this.appFixtureService = appFixtureService;
        this.appH2HService = appH2HService;
        this.marketComparisonService = marketComparisonService;
        this.appTeamService = appTeamService;
        this.featureBuilderService = featureBuilderService;
        this.probabilityEngineService =
                probabilityEngineService;
        this.predictionExplanationService =
                predictionExplanationService;
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

        MatchFeatureSnapshotDto features =
                safeCall(
                        () -> featureBuilderService
                                .buildForFixture(fixtureId),
                        "features",
                        fixtureId
                );

        MatchPredictionDto prediction =
                features == null
                        ? null
                        : safeCall(
                                () -> probabilityEngineService
                                        .predict(features),
                                "prediction",
                                fixtureId
                        );

        PredictionExplanationDto predictionExplanation =
                safeCall(
                        () -> predictionExplanationService
                                .build(overview),
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

}