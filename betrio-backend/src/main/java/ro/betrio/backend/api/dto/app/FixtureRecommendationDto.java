package ro.betrio.backend.api.dto.app;

public record FixtureRecommendationDto(
        Long fixtureId,
        String homeTeamName,
        String awayTeamName,
        boolean available,
        String reason,
        String recommendationType,
        String recommendedResultCode,
        Double confidenceScore,
        String confidenceTier,
        String riskTag,
        String overUnderLean,
        String bttsLean,
        ExactScorePick topExactScore,
        MarketSignal marketSignal,
        String summary
) {
    public record ExactScorePick(
            Integer homeGoals,
            Integer awayGoals,
            Double probability
    ) {
    }

    public record MarketSignal(
            boolean marketComparisonAvailable,
            String bestEdgeSelection,
            Double edgeOnRecommendedSelection,
            boolean valueCandidate
    ) {
    }
}