package ro.betrio.backend.api.dto.app;

public record CompetitionOptionDto(
        Long competitionId,
        String competitionName,
        String countryName,
        String providerName,
        Long externalLeagueId
) {
}