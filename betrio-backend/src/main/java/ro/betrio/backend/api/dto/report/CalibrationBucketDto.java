package ro.betrio.backend.api.dto.report;

public record CalibrationBucketDto(
        String market,
        String bucket,
        double minProbability,
        double maxProbability,
        long evaluations,
        double averagePredictedProbability,
        double actualHitRate,
        double calibrationGap
) {
}