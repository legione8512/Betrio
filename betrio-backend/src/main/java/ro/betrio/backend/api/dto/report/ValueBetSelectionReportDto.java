package ro.betrio.backend.api.dto.report;

public record ValueBetSelectionReportDto(
        String selection,
        long virtualBets,
        long winningBets,
        long losingBets,
        double totalStake,
        double profit,
        double roi,
        double winRate,
        double averageEdge,
        double averageOdd
) {
}