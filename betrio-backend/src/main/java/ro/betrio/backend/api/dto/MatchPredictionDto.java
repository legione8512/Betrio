package ro.betrio.backend.api.dto;

import java.util.List;

public record MatchPredictionDto(
        Long fixtureId,
        double homeWinProbability,
        double drawProbability,
        double awayWinProbability,
        double over25Probability,
        double under25Probability,
        double bttsYesProbability,
        double bttsNoProbability,
        List<ExactScoreDto> topExactScores
) {
}