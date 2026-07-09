package ro.betrio.backend.api.dto;

public record LeagueBaselineSnapshotDto(
        int sampleSize,
        double avgHomeGoals,
        double avgAwayGoals,
        double avgTotalGoals,
        double homeWinRate,
        double drawRate,
        double awayWinRate,
        double over25Rate,
        double bttsRate
) {
}