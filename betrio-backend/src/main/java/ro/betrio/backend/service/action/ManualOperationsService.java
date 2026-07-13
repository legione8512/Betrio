package ro.betrio.backend.service.action;

import java.time.OffsetDateTime;
import ro.betrio.backend.service.sync.FixtureResultSyncService;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.betrio.backend.api.dto.action.ManualActionHistoryDto;
import ro.betrio.backend.api.dto.action.ManualActionResultDto;
import ro.betrio.backend.config.ApiFootballProperties;
import ro.betrio.backend.domain.entity.Fixture;
import ro.betrio.backend.domain.entity.ManualActionRun;
import ro.betrio.backend.repository.FixtureRepository;
import ro.betrio.backend.repository.ManualActionRunRepository;
import ro.betrio.backend.service.prediction.PredictionEvaluationService;
import ro.betrio.backend.service.prediction.PredictionPersistenceService;
import ro.betrio.backend.service.sync.FixtureStatisticsSyncService;
import ro.betrio.backend.service.sync.OddsSnapshotSyncService;
import ro.betrio.backend.service.sync.SquadAndAvailabilitySyncService;

@Service
public class ManualOperationsService {

    private final ManualActionRunRepository manualActionRunRepository;
    private final FixtureRepository fixtureRepository;
    private final ApiFootballProperties apiFootballProperties;
    private final SquadAndAvailabilitySyncService squadAndAvailabilitySyncService;
    private final FixtureStatisticsSyncService fixtureStatisticsSyncService;
    private final FixtureResultSyncService fixtureResultSyncService;
    private final PredictionPersistenceService predictionPersistenceService;
    private final PredictionEvaluationService predictionEvaluationService;
    private final OddsSnapshotSyncService oddsSnapshotSyncService;

    public ManualOperationsService(
            ManualActionRunRepository manualActionRunRepository,
            FixtureRepository fixtureRepository,
            ApiFootballProperties apiFootballProperties,
            SquadAndAvailabilitySyncService squadAndAvailabilitySyncService,
            FixtureStatisticsSyncService fixtureStatisticsSyncService,
            FixtureResultSyncService fixtureResultSyncService,
            PredictionPersistenceService predictionPersistenceService,
            PredictionEvaluationService predictionEvaluationService,
            OddsSnapshotSyncService oddsSnapshotSyncService) {
        this.manualActionRunRepository = manualActionRunRepository;
        this.fixtureRepository = fixtureRepository;
        this.apiFootballProperties = apiFootballProperties;
        this.squadAndAvailabilitySyncService = squadAndAvailabilitySyncService;
        this.fixtureStatisticsSyncService = fixtureStatisticsSyncService;
        this.fixtureResultSyncService = fixtureResultSyncService;
        this.predictionPersistenceService = predictionPersistenceService;
        this.predictionEvaluationService = predictionEvaluationService;
        this.oddsSnapshotSyncService = oddsSnapshotSyncService;
    }

    public ManualActionResultDto baseRefresh() {
        return execute("BASE_REFRESH", null, () -> {
            squadAndAvailabilitySyncService.syncCurrentSquads(apiFootballProperties.getCurrentSeason());
            return "Base refresh completed.";
        });
    }

    public ManualActionResultDto smartUpdate(Long fixtureId) {
        return execute("SMART_UPDATE", fixtureId, () -> {
        	Fixture fixture = fixtureResultSyncService.refreshFixtureById(fixtureId);

        	squadAndAvailabilitySyncService.syncFixtureContextByFixtureId(fixtureId);
        	fixtureStatisticsSyncService.syncFixtureStatisticsByFixtureId(fixtureId);

            if (isFinished(fixture)) {
                try {
                    Long evaluationId = predictionEvaluationService.evaluateLatestPredictionForFixture(fixtureId);
                    return "Smart update completed for finished fixture. Evaluation id = " + evaluationId;
                } catch (Exception ex) {
                    return "Smart update completed for finished fixture. No evaluation created: " + ex.getMessage();
                }
            }

            oddsSnapshotSyncService.captureOddsForFixture(fixtureId);
            Long runId = predictionPersistenceService.generateAndStorePrediction(fixtureId);

            return "Smart update completed for non-finished fixture. Prediction run id = " + runId;
        });
    }

    @Transactional(readOnly = true)
    public List<ManualActionHistoryDto> getHistory(int limit) {
        return manualActionRunRepository.findAllByOrderByStartedAtDesc(PageRequest.of(0, limit))
                .stream()
                .map(this::toHistoryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ManualActionHistoryDto> getFixtureHistory(Long fixtureId, int limit) {
        return manualActionRunRepository.findByFixtureIdOrderByStartedAtDesc(fixtureId, PageRequest.of(0, limit))
                .stream()
                .map(this::toHistoryDto)
                .toList();
    }

    private ManualActionResultDto execute(String actionKey, Long fixtureId, Operation operation) {
        Fixture fixture = null;
        if (fixtureId != null) {
            fixture = fixtureRepository.findById(fixtureId)
                    .orElseThrow(() -> new IllegalStateException("Fixture not found: " + fixtureId));
        }

        ManualActionRun run = new ManualActionRun();
        run.setActionKey(actionKey);
        run.setStatus("IN_PROGRESS");
        run.setFixture(fixture);
        run.setMessage("Started");
        run = manualActionRunRepository.save(run);

        try {
            String message = operation.run();
            run.setStatus("SUCCESS");
            run.setFinishedAt(OffsetDateTime.now());
            run.setMessage(message);
            run = manualActionRunRepository.save(run);
            return toResultDto(run);
        } catch (Exception ex) {
            run.setStatus("FAILED");
            run.setFinishedAt(OffsetDateTime.now());
            run.setMessage(ex.getMessage());
            run = manualActionRunRepository.save(run);
            throw ex;
        }
    }

    private boolean isFinished(Fixture fixture) {
        String status = fixture.getStatusShort();
        return "FT".equals(status) || "AET".equals(status) || "PEN".equals(status);
    }

    private ManualActionHistoryDto toHistoryDto(ManualActionRun run) {
        return new ManualActionHistoryDto(
                run.getId(),
                run.getActionKey(),
                run.getStatus(),
                run.getFixture() != null ? run.getFixture().getId() : null,
                run.getStartedAt(),
                run.getFinishedAt(),
                run.getMessage()
        );
    }

    private ManualActionResultDto toResultDto(ManualActionRun run) {
        return new ManualActionResultDto(
                run.getId(),
                run.getActionKey(),
                run.getStatus(),
                run.getFixture() != null ? run.getFixture().getId() : null,
                run.getStartedAt(),
                run.getFinishedAt(),
                run.getMessage()
        );
    }

    @FunctionalInterface
    private interface Operation {
        String run();
    }
}