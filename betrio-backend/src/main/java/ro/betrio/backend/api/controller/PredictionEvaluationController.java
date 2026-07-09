package ro.betrio.backend.api.controller;

import org.springframework.http.ResponseEntity;
import ro.betrio.backend.api.dto.BatchEvaluationResultDto;
import org.springframework.web.bind.annotation.*;
import ro.betrio.backend.service.prediction.PredictionBatchEvaluationService;
import ro.betrio.backend.api.dto.PredictionEvaluationDto;
import ro.betrio.backend.service.prediction.PredictionEvaluationService;

@RestController
@RequestMapping("/api/evaluation")
public class PredictionEvaluationController {

    private final PredictionEvaluationService predictionEvaluationService;
    private final PredictionBatchEvaluationService predictionBatchEvaluationService;
    
    public PredictionEvaluationController(PredictionEvaluationService predictionEvaluationService,
            PredictionBatchEvaluationService predictionBatchEvaluationService) {
        this.predictionEvaluationService = predictionEvaluationService;
        this.predictionBatchEvaluationService = predictionBatchEvaluationService;

    }

    @PostMapping("/run/{predictionRunId}")
    public ResponseEntity<String> evaluateRun(@PathVariable Long predictionRunId) {
        Long evaluationId = predictionEvaluationService.evaluatePredictionRun(predictionRunId);
        return ResponseEntity.ok("Prediction evaluation stored. Evaluation id = " + evaluationId);
    }
    @PostMapping("/batch/finished")
    public BatchEvaluationResultDto evaluateFinishedBatch(
            @RequestParam(defaultValue = "100") int limit) {
        return predictionBatchEvaluationService
                .evaluateFinishedPreMatchPredictions(limit);
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