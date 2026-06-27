package ro.betrio.backend.api.dto.report;

public record ModelDashboardDto(
        long totalEvaluatedPredictions,
        double accuracy1x2,
        double accuracyOver25,
        double accuracyBtts,
        double exactScoreHitRate,
        double averageBrierScore1x2,
        double averageLogLoss1x2
) {
}