package ro.betrio.backend.api.dto.app;

import java.util.List;

public record LeagueFormTableDto(
        Long seasonId,
        Integer seasonYear,
        String competitionName,
        int limitPerTeam,
        List<Row> rows
) {
    public record Row(
            int position,
            Long teamId,
            String teamName,
            int played,
            int wins,
            int draws,
            int losses,
            int goalsFor,
            int goalsAgainst,
            int goalDifference,
            int points,
            String formSequence
    ) {
    }
}