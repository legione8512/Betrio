package ro.betrio.backend.client.apifootball.dto;

import java.util.List;

public class ApiFootballTeamsResponse {
    private List<ApiFootballTeamItem> response;

    public List<ApiFootballTeamItem> getResponse() { return response; }
    public void setResponse(List<ApiFootballTeamItem> response) { this.response = response; }
}