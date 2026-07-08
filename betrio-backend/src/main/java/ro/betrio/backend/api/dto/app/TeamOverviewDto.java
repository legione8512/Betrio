package ro.betrio.backend.api.dto.app;

import java.time.OffsetDateTime;
import java.util.List;

public record TeamOverviewDto(
        Long teamId,
        String teamName,
        Summary summary,
        List<MatchItem> recentMatches,
        List<MatchItem> upcomingMatches
) {
    public record Summary(
            int recentMatchesCount,
            int wins,
            int draws,
            int losses,
            int goalsFor,
            int goalsAgainst,
            int points,
            double averageGoalsFor,
            double averageGoalsAgainst
    ) {
    }

    public record MatchItem(
            Long fixtureId,
            OffsetDateTime kickoffAt,
            String statusShort,
            String statusLong,
            String leagueRound,
            boolean home,
            String opponentName,
            Integer teamGoals,
            Integer opponentGoals,
            String resultCode
    ) {
    }
}