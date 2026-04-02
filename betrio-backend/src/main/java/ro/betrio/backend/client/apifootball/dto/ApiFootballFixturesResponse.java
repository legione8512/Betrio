package ro.betrio.backend.client.apifootball.dto;

import java.util.List;

public class ApiFootballFixturesResponse {
    private List<ApiFootballFixtureItem> response;

    public List<ApiFootballFixtureItem> getResponse() { return response; }
    public void setResponse(List<ApiFootballFixtureItem> response) { this.response = response; }
}