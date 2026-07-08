package ro.betrio.backend.service.app;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AppInternalApiClient {

    private final RestClient restClient;

    public AppInternalApiClient(@Value("${app.internal-api.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public Object getFeatures(Long fixtureId) {
        return getBody("/api/analysis/fixture/{fixtureId}/features", fixtureId);
    }

    public Object getPrediction(Long fixtureId) {
        return getBody("/api/predictions/fixture/{fixtureId}", fixtureId);
    }

    public Object getDashboardSummary() {
        return getBodyNoArgs("/api/app/dashboard/summary");
    }

    public Object getUpcomingPicks(Long competitionId, int limit) {
        String uri = "/api/app/recommendations/upcoming?limit=" + limit;
        if (competitionId != null) {
            uri += "&competitionId=" + competitionId;
        }
        return getBodyNoArgs(uri);
    }

    public Object getCurrentStandings(Long competitionId) {
        String uri = "/api/app/standings/current";
        if (competitionId != null) {
            uri += "?competitionId=" + competitionId;
        }
        return getBodyNoArgs(uri);
    }

    public Object getCurrentFormTable(Long competitionId, int limit) {
        String uri = "/api/app/standings/form/current?limit=" + limit;
        if (competitionId != null) {
            uri += "&competitionId=" + competitionId;
        }
        return getBodyNoArgs(uri);
    }

    public Object getRecentEvaluations(int limit) {
        return getBodyNoArgs("/api/app/evaluations/recent?limit=" + limit);
    }

    private Object getBody(String uriTemplate, Long fixtureId) {
        return restClient.get()
                .uri(uriTemplate, fixtureId)
                .retrieve()
                .body(Object.class);
    }

    private Object getBodyNoArgs(String uri) {
        return restClient.get()
                .uri(uri)
                .retrieve()
                .body(Object.class);
    }
}