package ro.betrio.backend.api.dto.report;

import java.time.OffsetDateTime;

public record FixtureEvaluationReportDto(
        Long fixtureId,
        String homeTeamName,
        String awayTeamName,
        OffsetDateTime kickoffAt,
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