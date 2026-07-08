package ro.betrio.backend.client.apifootball;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import ro.betrio.backend.client.apifootball.dto.ApiFootballFixturesResponse;
import ro.betrio.backend.client.apifootball.dto.ApiFootballLeagueResponse;
import ro.betrio.backend.client.apifootball.dto.ApiFootballTeamsResponse;
import ro.betrio.backend.client.apifootball.dto.ApiFootballSquadsResponse;
import ro.betrio.backend.config.ApiFootballProperties;
import tools.jackson.databind.JsonNode;

@Component
public class ApiFootballClient {

    private final RestClient restClient;
    private final ApiFootballProperties properties;

    public ApiFootballClient(RestClient apiFootballRestClient, ApiFootballProperties properties) {
        this.restClient = apiFootballRestClient;
        this.properties = properties;
    }

    // ===== LEAGUE DETAILS =====

    public ApiFootballLeagueResponse getLeagueDetails() {
        return getLeagueDetails(properties.getLeagueId(), properties.getCurrentSeason());
    }

    public ApiFootballLeagueResponse getLeagueDetails(long leagueId) {
        return getLeagueDetails(leagueId, properties.getCurrentSeason());
    }

    public ApiFootballLeagueResponse getLeagueDetails(long leagueId, int season) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/leagues")
                        .queryParam("id", leagueId)
                        .queryParam("season", season)
                        .build())
                .retrieve()
                .body(ApiFootballLeagueResponse.class);
    }

    // ===== TEAMS =====

    public ApiFootballTeamsResponse getTeamsBySeason(int season) {
        return getTeamsBySeason(properties.getLeagueId(), season);
    }

    public ApiFootballTeamsResponse getTeamsBySeason(long leagueId, int season) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/teams")
                        .queryParam("league", leagueId)
                        .queryParam("season", season)
                        .build())
                .retrieve()
                .body(ApiFootballTeamsResponse.class);
    }

    // ===== FIXTURES =====

    public ApiFootballFixturesResponse getFixturesBySeason(int season) {
        return getFixturesBySeason(properties.getLeagueId(), season);
    }

    public ApiFootballFixturesResponse getFixturesBySeason(long leagueId, int season) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/fixtures")
                        .queryParam("league", leagueId)
                        .queryParam("season", season)
                        .build())
                .retrieve()
                .body(ApiFootballFixturesResponse.class);
    }

    // ===== SQUADS =====

    public ApiFootballSquadsResponse getSquadByTeam(long teamId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/players/squads")
                        .queryParam("team", teamId)
                        .build())
                .retrieve()
                .body(ApiFootballSquadsResponse.class);
    }

    // ===== LINEUPS =====

    public JsonNode getLineupsByFixture(long fixtureId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/fixtures/lineups")
                        .queryParam("fixture", fixtureId)
                        .build())
                .retrieve()
                .body(JsonNode.class);
    }

    // ===== INJURIES =====

    public JsonNode getInjuriesByFixture(long fixtureId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/injuries")
                        .queryParam("fixture", fixtureId)
                        .build())
                .retrieve()
                .body(JsonNode.class);
    }

    // ===== FIXTURE STATISTICS =====

    public JsonNode getFixtureStatistics(long fixtureId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/fixtures/statistics")
                        .queryParam("fixture", fixtureId)
                        .build())
                .retrieve()
                .body(JsonNode.class);
    }

    // ===== ODDS =====

    public JsonNode getOddsByFixture(long fixtureId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/odds")
                        .queryParam("fixture", fixtureId)
                        .build())
                .retrieve()
                .body(JsonNode.class);
    }

    // ===== PROVIDER PREDICTION =====

    public JsonNode getProviderPredictionByFixture(long fixtureId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/predictions")
                        .queryParam("fixture", fixtureId)
                        .build())
                .retrieve()
                .body(JsonNode.class);
    }
}