package ro.betrio.backend.api.dto;

public record TeamFormSnapshotDto(
        Long teamId,
        String teamName,
        int sampleSize,
        int wins,
        int draws,
        int losses,
        int points,
        double pointsPerGame,
        double goalsForPerGame,
        double goalsAgainstPerGame,
        double bttsRate,
        double over25Rate,
        double cleanSheetRate,
        double avgPossession,
        double avgShotsOnGoal,
        long knownAbsencesForFixture,
        long missingFixtureAbsences,
        long questionableAbsences,
        double absenceImpactScore
) {
}