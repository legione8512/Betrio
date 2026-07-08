package ro.betrio.backend.api.dto.app;

import java.time.OffsetDateTime;
import java.util.List;

public record UpcomingPicksDto(
        OffsetDateTime generatedAt,
        int requestedLimit,
        int returnedCount,
        int availableCount,
        List<PickItem> picks
) {
    public record PickItem(
            Long fixtureId,
            OffsetDateTime kickoffAt,
            String homeTeamName,
            String awayTeamName,
            boolean available,
            String recommendationType,
            String recommendedResultCode,
            Double confidenceScore,
            String confidenceTier,
            String riskTag,
            String overUnderLean,
            String bttsLean,
            String summary
    ) {
    }
}