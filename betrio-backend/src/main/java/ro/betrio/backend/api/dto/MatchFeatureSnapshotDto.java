package ro.betrio.backend.api.dto;

public record MatchFeatureSnapshotDto(
        Long fixtureId,
        String homeTeamName,
        String awayTeamName,
        String kickoffAt,
        TeamFormSnapshotDto homeForm,
        TeamFormSnapshotDto awayForm,
        HeadToHeadSnapshotDto headToHead,
        double expectedHomeGoals,
        double expectedAwayGoals
) {
}