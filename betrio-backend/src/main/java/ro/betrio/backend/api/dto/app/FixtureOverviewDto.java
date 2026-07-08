package ro.betrio.backend.api.dto.app;

import java.time.OffsetDateTime;

public record FixtureOverviewDto(
        Long fixtureId,
        OffsetDateTime kickoffAt,
        String statusShort,
        String statusLong,
        String leagueRound,
        String venueName,
        String venueCity,
        TeamSummary homeTeam,
        TeamSummary awayTeam,
        ScoreSummary score,
        PredictionSummary latestPrediction,
        EvaluationSummary latestEvaluation,
        ActionSummary latestAction,
        boolean finished,
        boolean canRunSmartUpdate,
        boolean canEvaluate
) {

    public record TeamSummary(
            Long id,
            String name
    ) {
    }

    public record ScoreSummary(
            Integer homeGoals,
            Integer awayGoals,
            Integer halftimeHomeGoals,
            Integer halftimeAwayGoals,
            Integer fulltimeHomeGoals,
            Integer fulltimeAwayGoals
    ) {
    }

    public record PredictionSummary(
            Long predictionRunId,
            OffsetDateTime generatedAt,
            Double homeWinProbability,
            Double drawProbability,
            Double awayWinProbability,
            Double over25Probability,
            Double under25Probability,
            Double bttsYesProbability,
            Double bttsNoProbability,
            String recommendedResultCode
    ) {
    }

    public record EvaluationSummary(
            Long evaluationId,
            String predictedResultCode,
            String actualResultCode,
            Integer actualHomeGoals,
            Integer actualAwayGoals,
            Boolean hit1x2,
            Boolean hitOver25,
            Boolean hitBtts,
            Boolean topExactScoreHit,
            Double brierScore1x2,
            Double logLoss1x2
    ) {
    }

    public record ActionSummary(
            Long actionRunId,
            String actionKey,
            String status,
            OffsetDateTime startedAt,
            OffsetDateTime finishedAt,
            String message
    ) {
    }
}