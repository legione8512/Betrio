package ro.betrio.backend.api.dto.report;

import java.util.List;

public record ValueBetReportDto(
        double minEdge,
        long evaluatedPredictions,
        long marketEvaluations,
        long virtualBets,
        long winningBets,
        long losingBets,
        double totalStake,
        double profit,
        double roi,
        double winRate,
        double averageEdge,
        double averageOdd,
        List<ValueBetSelectionReportDto> selections
) {
}