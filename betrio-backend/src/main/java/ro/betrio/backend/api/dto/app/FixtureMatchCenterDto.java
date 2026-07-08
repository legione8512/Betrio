package ro.betrio.backend.api.dto.app;

import ro.betrio.backend.api.dto.MatchFeatureSnapshotDto;
import ro.betrio.backend.api.dto.MatchPredictionDto;

public record FixtureMatchCenterDto(
        Long fixtureId,
        FixtureOverviewDto overview,
        FixtureH2HDto h2h,
        FixtureMarketComparisonDto marketComparison,
        TeamFormDto homeTeamForm,
        TeamFormDto awayTeamForm,
        MatchFeatureSnapshotDto features,
        MatchPredictionDto prediction,
        PredictionExplanationDto predictionExplanation
) {
}