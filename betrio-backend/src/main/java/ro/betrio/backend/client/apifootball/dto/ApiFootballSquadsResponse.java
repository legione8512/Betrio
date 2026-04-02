package ro.betrio.backend.client.apifootball.dto;

import java.util.List;

public class ApiFootballSquadsResponse {
    private List<ApiFootballSquadItem> response;

    public List<ApiFootballSquadItem> getResponse() {
        return response;
    }

    public void setResponse(List<ApiFootballSquadItem> response) {
        this.response = response;
    }
}