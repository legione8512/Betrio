package ro.betrio.backend.api.dto;

public record PredictionEvaluationDto(
        Long predictionRunId,
        String predictedResultCode,
        String actualResultCode,
        int actualHomeGoals,
        int actualAwayGoals,
        boolean actualOver25,
        boolean actualBttsYes,
        boolean hit1x2,
        boolean hitOver25,
        boolean hitBtts,
        boolean topExactScoreHit,
        double brierScore1x2,
        double logLoss1x2,
        Double marketHomeImpliedProbability,
        Double marketDrawImpliedProbability,
        Double marketAwayImpliedProbability,
        Double modelEdgeHome,
        Double modelEdgeDraw,
        Double modelEdgeAway
) {
}