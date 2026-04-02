package ro.betrio.backend.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import ro.betrio.backend.service.prediction.PredictionPersistenceService;
import ro.betrio.backend.service.sync.OddsSnapshotSyncService;

@RestController
@RequestMapping("/api")
public class PredictionCaptureController {

    private final PredictionPersistenceService predictionPersistenceService;
    private final OddsSnapshotSyncService oddsSnapshotSyncService;

    public PredictionCaptureController(
            PredictionPersistenceService predictionPersistenceService,
            OddsSnapshotSyncService oddsSnapshotSyncService) {
        this.predictionPersistenceService = predictionPersistenceService;
        this.oddsSnapshotSyncService = oddsSnapshotSyncService;
    }

    @PostMapping("/predictions/fixture/{fixtureId}/capture")
    public ResponseEntity<String> capturePrediction(@PathVariable Long fixtureId) {
        Long runId = predictionPersistenceService.generateAndStorePrediction(fixtureId);
        return ResponseEntity.ok("Prediction stored. Run id = " + runId);
    }

    @PostMapping("/odds/fixture/{fixtureId}/capture")
    public ResponseEntity<String> captureOdds(@PathVariable Long fixtureId) {
        oddsSnapshotSyncService.captureOddsForFixture(fixtureId);
        return ResponseEntity.ok("Odds snapshot stored for fixture id " + fixtureId);
    }

    @PostMapping("/fixture/{fixtureId}/pre-match-capture")
    public ResponseEntity<String> capturePreMatch(@PathVariable Long fixtureId) {
        oddsSnapshotSyncService.captureOddsForFixture(fixtureId);
        Long runId = predictionPersistenceService.generateAndStorePrediction(fixtureId);
        return ResponseEntity.ok("Pre-match capture completed. Prediction run id = " + runId);
    }
}