package ro.betrio.backend.api.dto.sync;

public record LeagueBootstrapRequestDto(
        Long leagueId,
        Integer currentSeason,
        Integer previousSeason
) {
}