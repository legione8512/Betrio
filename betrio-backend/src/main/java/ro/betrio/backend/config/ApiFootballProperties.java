package ro.betrio.backend.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.provider.api-football")
public class ApiFootballProperties {

    @NotBlank
    private String baseUrl;

    @NotBlank
    private String apiKey;

    @NotNull
    private Long leagueId;

    @NotNull
    @Min(2000)
    @Max(2100)
    private Integer currentSeason;

    @NotNull
    @Min(2000)
    @Max(2100)
    private Integer previousSeason;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Long getLeagueId() {
        return leagueId;
    }

    public void setLeagueId(Long leagueId) {
        this.leagueId = leagueId;
    }

    public Integer getCurrentSeason() {
        return currentSeason;
    }

    public void setCurrentSeason(Integer currentSeason) {
        this.currentSeason = currentSeason;
    }

    public Integer getPreviousSeason() {
        return previousSeason;
    }

    public void setPreviousSeason(Integer previousSeason) {
        this.previousSeason = previousSeason;
    }
}