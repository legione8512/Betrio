package ro.betrio.backend.api.dto.meta;

public record AppDashboardSummaryDto(
        long totalFixtures,
        long upcomingFixtures,
        long finishedFixtures,
        long evaluatedPredictions
) {
}