package ro.betrio.backend.api.dto.app;

import java.time.OffsetDateTime;
import java.util.List;

public record FixtureH2HDto(
        Long fixtureId,
        Long homeTeamId,
        String homeTeamName,
        Long awayTeamId,
        String awayTeamName,
        Summary summary,
        List<H2HMatchItem> matches
) {
    public record Summary(
            int totalMatches,
            int homeTeamWins,
            int draws,
            int awayTeamWins,
            int homeTeamGoals,
            int awayTeamGoals
    ) {
    }

    public record H2HMatchItem(
            Long fixtureId,
            OffsetDateTime kickoffAt,
            String leagueRound,
            String statusShort,
            String homeTeamName,
            String awayTeamName,
            Integer homeGoals,
            Integer awayGoals,
            String resultCode
    ) {
    }
}