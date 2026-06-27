package ro.betrio.backend.api.dto.action;

import java.time.OffsetDateTime;

public record ManualActionHistoryDto(
        Long id,
        String actionKey,
        String status,
        Long fixtureId,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        String message
) {
}