package ro.betrio.backend.client.apifootball;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import ro.betrio.backend.client.apifootball.dto.ApiFootballFixturesResponse;
import ro.betrio.backend.client.apifootball.dto.ApiFootballLeagueResponse;
import ro.betrio.backend.client.apifootball.dto.ApiFootballTeamsResponse;
import ro.betrio.backend.config.ApiFootballProperties;

import tools.jackson.databind.JsonNode;

import ro.betrio.backend.client.apifootball.dto.ApiFootballSquadsResponse;

@Component
public class ApiFootballClient {

    private final RestClient restClient;
    private final ApiFootballProperties properties;

    public ApiFootballClient(RestClient apiFootballRestClient, ApiFootballProperties properties) {
        this.restClient = apiFootballRestClient;
        this.properties = properties;
    }

    public ApiFootballLeagueResponse getLeagueDetails() {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/leagues")
                        .queryParam("id", properties.getLeagueId())
                        .build())
                .retrieve()
                .body(ApiFootballLeagueResponse.class);
    }

    public ApiFootballTeamsResponse getTeamsBySeason(int season) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/teams")
                        .queryParam("league", properties.getLeagueId())
                        .queryParam("season", season)
                        .build())
                .retrieve()
                .body(ApiFootballTeamsResponse.class);
    }

    public ApiFootballFixturesResponse getFixturesBySeason(int season) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/fixtures")
                        .queryParam("league", properties.getLeagueId())
                        .queryParam("season", season)
                        .build())
                .retrieve()
                .body(ApiFootballFixturesResponse.class);
    }
    public ApiFootballSquadsResponse getSquadByTeam(long teamId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/players/squads")
                        .queryParam("team", teamId)
                        .build())
                .retrieve()
                .body(ApiFootballSquadsResponse.class);
    }

    public JsonNode getLineupsByFixture(long fixtureId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/fixtures/lineups")
                        .queryParam("fixture", fixtureId)
                        .build())
                .retrieve()
                .body(JsonNode.class);
    }

    public JsonNode getInjuriesByFixture(long fixtureId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/injuries")
                        .queryParam("fixture", fixtureId)
                        .build())
                .retrieve()
                .body(JsonNode.class);
    }
    
    public JsonNode getFixtureStatistics(long fixtureId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/fixtures/statistics")
                        .queryParam("fixture", fixtureId)
                        .build())
                .retrieve()
                .body(JsonNode.class);
    }
    public JsonNode getOddsByFixture(long fixtureId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/odds")
                        .queryParam("fixture", fixtureId)
                        .build())
                .retrieve()
                .body(JsonNode.class);
    }

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