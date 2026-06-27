package ro.betrio.backend.api.dto.report;

public record TeamPerformanceReportDto(
        Long teamId,
        String teamName,
        long evaluatedMatches,
        double accuracy1x2,
        double accuracyOver25,
        double accuracyBtts,
        double averageBrierScore1x2,
        double averageLogLoss1x2
) {
}