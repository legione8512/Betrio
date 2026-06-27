package ro.betrio.backend.api.dto.action;

import java.time.OffsetDateTime;

public record ManualActionResultDto(
        Long actionRunId,
        String actionKey,
        String status,
        Long fixtureId,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        String message
) {
}