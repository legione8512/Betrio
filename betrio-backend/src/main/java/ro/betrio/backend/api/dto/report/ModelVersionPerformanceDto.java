package ro.betrio.backend.api.dto.report;

public record ModelVersionPerformanceDto(
        String modelVersion,
        Long evaluations,
        Double accuracy1x2,
        Double accuracyOver25,
        Double accuracyBtts,
        Double exactScoreHitRate,
        Double avgBrierScore,
        Double avgLogLoss
) {
}