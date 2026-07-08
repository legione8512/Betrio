package ro.betrio.backend.api.dto.app;

import java.time.OffsetDateTime;

public record FixtureMarketComparisonDto(
        Long fixtureId,
        String homeTeamName,
        String awayTeamName,
        OffsetDateTime kickoffAt,
        ModelProbabilities model,
        MarketOdds market,
        EdgeSummary edges,
        String bestEdgeSelection,
        OffsetDateTime oddsCapturedAt
) {
    public record ModelProbabilities(
            Double homeWinProbability,
            Double drawProbability,
            Double awayWinProbability
    ) {
    }

    public record MarketOdds(
            Double homeOdds,
            Double drawOdds,
            Double awayOdds,
            Double homeImpliedProbabilityRaw,
            Double drawImpliedProbabilityRaw,
            Double awayImpliedProbabilityRaw,
            Double homeImpliedProbabilityNormalized,
            Double drawImpliedProbabilityNormalized,
            Double awayImpliedProbabilityNormalized
    ) {
    }

    public record EdgeSummary(
            Double homeEdge,
            Double drawEdge,
            Double awayEdge
    ) {
    }
}