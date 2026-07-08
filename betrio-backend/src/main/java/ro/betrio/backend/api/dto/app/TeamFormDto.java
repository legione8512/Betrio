package ro.betrio.backend.api.dto.app;

import java.time.OffsetDateTime;
import java.util.List;

public record TeamFormDto(
        Long teamId,
        String teamName,
        int sampleSize,
        FormSummary overall,
        VenueSummary home,
        VenueSummary away,
        StreakSummary streak,
        String formSequence,
        List<MatchFormItem> matches
) {

    public record FormSummary(
            int played,
            int wins,
            int draws,
            int losses,
            int points,
            int goalsFor,
            int goalsAgainst,
            int goalDifference,
            double averageGoalsFor,
            double averageGoalsAgainst,
            int cleanSheets,
            int failedToScore,
            int bttsYesCount,
            int over25Count
    ) {
    }

    public record VenueSummary(
            int played,
            int wins,
            int draws,
            int losses,
            int points,
            int goalsFor,
            int goalsAgainst
    ) {
    }

    public record StreakSummary(
            String type,
            int length
    ) {
    }

    public record MatchFormItem(
            Long fixtureId,
            OffsetDateTime kickoffAt,
            boolean home,
            String opponentName,
            Integer teamGoals,
            Integer opponentGoals,
            String resultCode,
            boolean cleanSheet,
            boolean failedToScore,
            boolean bttsYes,
            boolean over25
    ) {
    }
}