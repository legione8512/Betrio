package ro.betrio.backend.client.apifootball.dto;

import java.util.List;

public class ApiFootballLeagueResponse {
    private List<ApiFootballLeagueItem> response;

    public List<ApiFootballLeagueItem> getResponse() { return response; }
    public void setResponse(List<ApiFootballLeagueItem> response) { this.response = response; }
}