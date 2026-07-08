package ro.betrio.backend.api.dto.app;

import java.time.OffsetDateTime;

public record FixtureListItemDto(
        Long fixtureId,
        OffsetDateTime kickoffAt,
        String statusShort,
        String statusLong,
        String leagueRound,
        Long homeTeamId,
        String homeTeamName,
        Integer homeGoals,
        Long awayTeamId,
        String awayTeamName,
        Integer awayGoals
) {
}