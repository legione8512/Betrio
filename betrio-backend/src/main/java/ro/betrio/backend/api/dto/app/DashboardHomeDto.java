package ro.betrio.backend.api.dto.app;

import java.time.OffsetDateTime;
import ro.betrio.backend.api.dto.meta.AppDashboardSummaryDto;
import java.util.List;

public record DashboardHomeDto(
        OffsetDateTime generatedAt,
        Integer picksLimit,
        Integer formLimit,
        Integer evaluationsLimit,
        AppDashboardSummaryDto summary,
        UpcomingPicksDto upcomingPicks,
        LeagueStandingsDto currentStandings,
        LeagueFormTableDto currentFormTable,
        List<RecentEvaluationDto> recentEvaluations
) {
}