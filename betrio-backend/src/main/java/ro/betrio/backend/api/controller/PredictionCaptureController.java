package ro.betrio.backend.api.controller;

import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import ro.betrio.backend.domain.entity.Fixture;
import ro.betrio.backend.repository.FixtureRepository;
import ro.betrio.backend.service.prediction.PredictionPersistenceService;
import ro.betrio.backend.service.sync.FixtureResultSyncService;
import ro.betrio.backend.service.sync.OddsSnapshotSyncService;
import ro.betrio.backend.service.sync.SquadAndAvailabilitySyncService;

@RestController
@RequestMapping("/api")
public class PredictionCaptureController {

    private final PredictionPersistenceService
            predictionPersistenceService;

    private final OddsSnapshotSyncService
            oddsSnapshotSyncService;

    private final FixtureRepository fixtureRepository;

    private final FixtureResultSyncService
            fixtureResultSyncService;

    private final SquadAndAvailabilitySyncService
            squadAndAvailabilitySyncService;

    public PredictionCaptureController(
            PredictionPersistenceService predictionPersistenceService,
            OddsSnapshotSyncService oddsSnapshotSyncService,
            FixtureRepository fixtureRepository,
            FixtureResultSyncService fixtureResultSyncService,
            SquadAndAvailabilitySyncService
                    squadAndAvailabilitySyncService) {

        this.predictionPersistenceService =
                predictionPersistenceService;

        this.oddsSnapshotSyncService =
                oddsSnapshotSyncService;

        this.fixtureRepository =
                fixtureRepository;

        this.fixtureResultSyncService =
                fixtureResultSyncService;

        this.squadAndAvailabilitySyncService =
                squadAndAvailabilitySyncService;
    }

    @PostMapping("/predictions/fixture/{fixtureId}/capture")
    public ResponseEntity<String> capturePrediction(
            @PathVariable Long fixtureId) {

        Long runId =
                predictionPersistenceService
                        .generateAndStorePrediction(fixtureId);

        return ResponseEntity.ok(
                "Prediction stored. Run id = " + runId
        );
    }

    @PostMapping("/odds/fixture/{fixtureId}/capture")
    public ResponseEntity<String> captureOdds(
            @PathVariable Long fixtureId) {

        oddsSnapshotSyncService
                .captureOddsForFixture(fixtureId);

        return ResponseEntity.ok(
                "Odds snapshot stored for fixture id "
                        + fixtureId
        );
    }

    @PostMapping("/fixture/{fixtureId}/pre-match-capture")
    public ResponseEntity<String> capturePreMatch(
            @PathVariable Long fixtureId) {

        oddsSnapshotSyncService
                .captureOddsForFixture(fixtureId);

        Long runId =
                predictionPersistenceService
                        .generateAndStorePrediction(fixtureId);

        return ResponseEntity.ok(
                "Pre-match capture completed. "
                        + "Prediction run id = "
                        + runId
        );
    }

    @PostMapping(
            "/competition/{competitionId}/upcoming/pre-match-capture"
    )
    public ResponseEntity<String>
            captureUpcomingPreMatchForCompetition(
                    @PathVariable Long competitionId,
                    @RequestParam(defaultValue = "100")
                    int limit) {

        int safeLimit =
                Math.max(1, Math.min(limit, 200));

        ZoneId applicationZone =
                ZoneId.of("Europe/Bucharest");

        ZonedDateTime now =
                ZonedDateTime.now(applicationZone);

        ZonedDateTime nextMondayStart =
                now.toLocalDate()
                        .with(
                                TemporalAdjusters.nextOrSame(
                                        DayOfWeek.SUNDAY
                                )
                        )
                        .plusDays(1)
                        .atStartOfDay(applicationZone);

        OffsetDateTime windowStart =
                now.toOffsetDateTime();

        OffsetDateTime windowEndExclusive =
                nextMondayStart.toOffsetDateTime();

        List<Fixture> fixtures =
                fixtureRepository
                        .findUpcomingWithTeamsByCompetitionInWindow(
                                competitionId,
                                windowStart,
                                windowEndExclusive,
                                PageRequest.of(0, safeLimit)
                        );

        int created = 0;
        int skippedUnchanged = 0;
        int skippedNotEligible = 0;
        int lineupsRefreshed = 0;
        int lineupsSkippedTooEarly = 0;
        int failed = 0;

        StringBuilder createdFixtureIds =
                new StringBuilder();

        StringBuilder unchangedFixtureIds =
                new StringBuilder();

        StringBuilder notEligibleFixtureIds =
                new StringBuilder();

        StringBuilder failedFixtureIds =
                new StringBuilder();

        for (Fixture fixture : fixtures) {
            try {
                Fixture refreshedFixture =
                        fixtureResultSyncService
                                .refreshFixtureById(
                                        fixture.getId()
                                );

                if (!isCaptureEligible(
                        refreshedFixture,
                        windowStart,
                        windowEndExclusive)) {

                    skippedNotEligible++;

                    appendCsv(
                            notEligibleFixtureIds,
                            String.valueOf(fixture.getId())
                    );

                    continue;
                }

                squadAndAvailabilitySyncService
                .syncFixtureInjuriesByFixtureId(
                        fixture.getId()
                );

        OffsetDateTime lineupWindowEnd =
                windowStart.plusHours(3);

        if (!refreshedFixture.getKickoffAt()
                .isAfter(lineupWindowEnd)) {

            squadAndAvailabilitySyncService
                    .syncFixtureLineupsByFixtureId(
                            fixture.getId()
                    );

            lineupsRefreshed++;
        } else {
            lineupsSkippedTooEarly++;
        }

        oddsSnapshotSyncService
                .captureOddsForFixture(
                        fixture.getId()
                );

                Optional<Long> runId =
                        predictionPersistenceService
                                .generateAndStorePredictionIfChanged(
                                        fixture.getId()
                                );

                if (runId.isPresent()) {
                    created++;

                    appendCsv(
                            createdFixtureIds,
                            fixture.getId()
                                    + "->"
                                    + runId.get()
                    );
                } else {
                    skippedUnchanged++;

                    appendCsv(
                            unchangedFixtureIds,
                            String.valueOf(fixture.getId())
                    );
                }
            } catch (Exception ex) {
                failed++;

                appendCsv(
                        failedFixtureIds,
                        fixture.getId()
                                + ":"
                                + ex.getClass()
                                        .getSimpleName()
                );
            }
        }

        String result =
                "Weekly smart pre-match capture completed "
                        + "for competitionId="
                        + competitionId
                        + ". windowStart="
                        + windowStart
                        + ", windowEndExclusive="
                        + windowEndExclusive
                        + ", fixturesFound="
                        + fixtures.size()
                        + ", created="
                        + created
                        + ", skippedUnchanged="
                        + skippedUnchanged
                        + ", skippedNotEligible="
                        + skippedNotEligible
                        + ", lineupsRefreshed="
                        + lineupsRefreshed
                        + ", lineupsSkippedTooEarly="
                        + lineupsSkippedTooEarly
                        + ", failed="
                        + failed
                        + ", createdFixtureIds=["
                        + createdFixtureIds
                        + "]"
                        + ", unchangedFixtureIds=["
                        + unchangedFixtureIds
                        + "]"
                        + ", notEligibleFixtureIds=["
                        + notEligibleFixtureIds
                        + "]"
                        + ", failedFixtureIds=["
                        + failedFixtureIds
                        + "]";

        return ResponseEntity.ok(result);
    }

    private boolean isCaptureEligible(
            Fixture fixture,
            OffsetDateTime windowStart,
            OffsetDateTime windowEndExclusive) {

        OffsetDateTime kickoffAt =
                fixture.getKickoffAt();

        if (kickoffAt == null) {
            return false;
        }

        boolean insideWindow =
                !kickoffAt.isBefore(windowStart)
                        && kickoffAt.isBefore(
                                windowEndExclusive
                        );

        if (!insideWindow) {
            return false;
        }

        String status =
                fixture.getStatusShort();

        return status == null
                || "NS".equals(status)
                || "TBD".equals(status);
    }

    private void appendCsv(
            StringBuilder builder,
            String value) {

        if (builder.length() > 0) {
            builder.append(",");
        }

        builder.append(value);
    }
}