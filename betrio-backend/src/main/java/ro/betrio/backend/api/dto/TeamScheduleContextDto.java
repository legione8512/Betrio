package ro.betrio.backend.api.dto;

public record TeamScheduleContextDto(
        Long teamId,
        String teamName,
        String lastMatchAt,
        int restDays,
        boolean shortRest,
        boolean longBreak,
        double restScore
) {
}