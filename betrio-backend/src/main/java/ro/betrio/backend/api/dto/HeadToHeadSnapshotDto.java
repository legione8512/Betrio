package ro.betrio.backend.api.dto;

public record HeadToHeadSnapshotDto(
        int sampleSize,
        int homeTeamWins,
        int draws,
        int awayTeamWins,
        double homeTeamPointsPerGame,
        double awayTeamPointsPerGame
) {
}