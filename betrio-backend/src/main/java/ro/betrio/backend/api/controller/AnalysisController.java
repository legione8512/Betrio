package ro.betrio.backend.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ro.betrio.backend.api.dto.MatchFeatureSnapshotDto;
import ro.betrio.backend.api.dto.MatchPredictionDto;
import ro.betrio.backend.service.analysis.FeatureBuilderService;
import ro.betrio.backend.service.prediction.ProbabilityEngineService;

@RestController
@RequestMapping("/api")
public class AnalysisController {

    private final FeatureBuilderService featureBuilderService;
    private final ProbabilityEngineService probabilityEngineService;

    public AnalysisController(
            FeatureBuilderService featureBuilderService,
            ProbabilityEngineService probabilityEngineService) {
        this.featureBuilderService = featureBuilderService;
        this.probabilityEngineService = probabilityEngineService;
    }

    @GetMapping("/analysis/fixture/{fixtureId}/features")
    public MatchFeatureSnapshotDto getFeatures(@PathVariable Long fixtureId) {
        return featureBuilderService.buildForFixture(fixtureId);
    }

    @GetMapping("/predictions/fixture/{fixtureId}")
    public MatchPredictionDto getPrediction(@PathVariable Long fixtureId) {
    	MatchFeatureSnapshotDto features =
    	        featureBuilderService
    	                .buildForFixture(fixtureId);

    	return probabilityEngineService.predict(features);    }
}