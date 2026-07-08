package ro.betrio.backend.api.dto.app;

public record PredictionExplanationDto(
        String recommendedResultCode,
        Double topProbability,
        String confidenceTier,
        String overUnderLean,
        String bttsLean,
        String summary
) {
}