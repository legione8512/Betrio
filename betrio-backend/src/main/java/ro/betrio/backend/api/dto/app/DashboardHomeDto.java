package ro.betrio.backend.api.dto.app;

import java.time.OffsetDateTime;

public record DashboardHomeDto(
        OffsetDateTime generatedAt,
        Integer picksLimit,
        Integer formLimit,
        Integer evaluationsLimit,
        Object summary,
        Object upcomingPicks,
        Object currentStandings,
        Object currentFormTable,
        Object recentEvaluations
) {
}