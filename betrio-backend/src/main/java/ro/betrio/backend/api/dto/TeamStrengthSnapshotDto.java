package ro.betrio.backend.api.dto;

public record TeamStrengthSnapshotDto(
        Long teamId,
        String teamName,
        int sampleSize,
        double pointsPerGame,
        double goalsForPerGame,
        double goalsAgainstPerGame,
        double goalDifferencePerGame,
        double strengthScore
) {
}