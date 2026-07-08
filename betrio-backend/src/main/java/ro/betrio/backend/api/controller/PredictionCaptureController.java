package ro.betrio.backend.api.controller;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import ro.betrio.backend.domain.entity.Fixture;
import ro.betrio.backend.repository.FixtureRepository;
import ro.betrio.backend.repository.PredictionRunRepository;
import ro.betrio.backend.service.prediction.PredictionPersistenceService;
import ro.betrio.backend.service.sync.OddsSnapshotSyncService;

@RestController
@RequestMapping("/api")
public class PredictionCaptureController {

    private final PredictionPersistenceService predictionPersistenceService;
    private final OddsSnapshotSyncService oddsSnapshotSyncService;
    private final FixtureRepository fixtureRepository;
    private final PredictionRunRepository predictionRunRepository;

    public PredictionCaptureController(
            PredictionPersistenceService predictionPersistenceService,
            OddsSnapshotSyncService oddsSnapshotSyncService,
            FixtureRepository fixtureRepository,
            PredictionRunRepository predictionRunRepository) {
        this.predictionPersistenceService = predictionPersistenceService;
        this.oddsSnapshotSyncService = oddsSnapshotSyncService;
        this.fixtureRepository = fixtureRepository;
        this.predictionRunRepository = predictionRunRepository;
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

    @PostMapping("/competition/{competitionId}/upcoming/pre-match-capture")
    public ResponseEntity<String> captureUpcomingPreMatchForCompetition(
            @PathVariable Long competitionId,
            @RequestParam(defaultValue = "100") int limit) {

        int safeLimit = Math.max(1, Math.min(limit, 200));

        List<Fixture> fixtures = fixtureRepository.findUpcomingWithTeamsByCompetition(
                competitionId,
                PageRequest.of(0, safeLimit)
        );

        int created = 0;
        int skippedExisting = 0;
        int failed = 0;

        StringBuilder createdFixtureIds = new StringBuilder();
        StringBuilder skippedFixtureIds = new StringBuilder();
        StringBuilder failedFixtureIds = new StringBuilder();

        for (Fixture fixture : fixtures) {
            boolean alreadyExists = predictionRunRepository
                    .findTopByFixtureIdOrderByGeneratedAtDesc(fixture.getId())
                    .isPresent();

            if (alreadyExists) {
                skippedExisting++;
                appendCsv(skippedFixtureIds, String.valueOf(fixture.getId()));
                continue;
            }

            try {
                oddsSnapshotSyncService.captureOddsForFixture(fixture.getId());
                Long runId = predictionPersistenceService.generateAndStorePrediction(fixture.getId());

                created++;
                appendCsv(createdFixtureIds, fixture.getId() + "->" + runId);
            } catch (Exception ex) {
                failed++;
                appendCsv(
                        failedFixtureIds,
                        fixture.getId() + ":" + ex.getClass().getSimpleName()
                );
            }
        }

        String result = "Batch pre-match capture completed for competitionId=" + competitionId
                + ". fixturesFound=" + fixtures.size()
                + ", created=" + created
                + ", skippedExisting=" + skippedExisting
                + ", failed=" + failed
                + ", createdFixtureIds=[" + createdFixtureIds + "]"
                + ", skippedFixtureIds=[" + skippedFixtureIds + "]"
                + ", failedFixtureIds=[" + failedFixtureIds + "]";

        return ResponseEntity.ok(result);
    }

    private void appendCsv(StringBuilder sb, String value) {
        if (sb.length() > 0) {
            sb.append(",");
        }
        sb.append(value);
    }
}