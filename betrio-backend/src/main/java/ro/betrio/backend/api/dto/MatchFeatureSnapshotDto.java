package ro.betrio.backend.api.dto;

public record MatchFeatureSnapshotDto(
        Long fixtureId,
        String homeTeamName,
        String awayTeamName,
        String kickoffAt,
        TeamFormSnapshotDto homeForm,
        TeamFormSnapshotDto awayForm,
        TeamFormSnapshotDto homeVenueForm,
        TeamFormSnapshotDto awayVenueForm,
        TeamScheduleContextDto homeScheduleContext,
        TeamScheduleContextDto awayScheduleContext,
        TeamStrengthSnapshotDto homeStrength,
        TeamStrengthSnapshotDto awayStrength,
        HeadToHeadSnapshotDto headToHead,
        LeagueBaselineSnapshotDto leagueBaseline,
        double expectedHomeGoals,
        double expectedAwayGoals
) {
}