package ro.betrio.backend.api.dto.app;

public record FixtureMatchCenterDto(
        Long fixtureId,
        FixtureOverviewDto overview,
        FixtureH2HDto h2h,
        FixtureMarketComparisonDto marketComparison,
        TeamFormDto homeTeamForm,
        TeamFormDto awayTeamForm,
        Object features,
        Object prediction,
        PredictionExplanationDto predictionExplanation
) {
}