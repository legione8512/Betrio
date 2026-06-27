package ro.betrio.backend.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import ro.betrio.backend.api.dto.PredictionEvaluationDto;
import ro.betrio.backend.service.prediction.PredictionEvaluationService;

@RestController
@RequestMapping("/api/evaluation")
public class PredictionEvaluationController {

    private final PredictionEvaluationService predictionEvaluationService;

    public PredictionEvaluationController(PredictionEvaluationService predictionEvaluationService) {
        this.predictionEvaluationService = predictionEvaluationService;
    }

    @PostMapping("/run/{predictionRunId}")
    public ResponseEntity<String> evaluateRun(@PathVariable Long predictionRunId) {
        Long evaluationId = predictionEvaluationService.evaluatePredictionRun(predictionRunId);
        return ResponseEntity.ok("Prediction evaluation stored. Evaluation id = " + evaluationId);
    }

    @PostMapping("/fixture/{fixtureId}/latest")
    public ResponseEntity<String> evaluateLatestForFixture(@PathVariable Long fixtureId) {
        Long evaluationId = predictionEvaluationService.evaluateLatestPredictionForFixture(fixtureId);
        return ResponseEntity.ok("Latest prediction evaluation stored. Evaluation id = " + evaluationId);
    }

    @GetMapping("/run/{predictionRunId}")
    public PredictionEvaluationDto getEvaluation(@PathVariable Long predictionRunId) {
        return predictionEvaluationService.getEvaluationByPredictionRunId(predictionRunId);
    }
}