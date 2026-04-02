package ro.betrio.backend.api.dto;

public record ExactScoreDto(
        int homeGoals,
        int awayGoals,
        double probability
) {
}