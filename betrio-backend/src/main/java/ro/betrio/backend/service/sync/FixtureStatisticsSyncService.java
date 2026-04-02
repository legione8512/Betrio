package ro.betrio.backend.service.sync;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;

import ro.betrio.backend.client.apifootball.ApiFootballClient;
import ro.betrio.backend.domain.entity.Fixture;
import ro.betrio.backend.domain.entity.FixtureTeamStat;
import ro.betrio.backend.domain.entity.Season;
import ro.betrio.backend.domain.entity.Team;
import ro.betrio.backend.repository.FixtureRepository;
import ro.betrio.backend.repository.FixtureTeamStatRepository;
import ro.betrio.backend.repository.SeasonRepository;

@Service
public class FixtureStatisticsSyncService {

    private final ApiFootballClient apiFootballClient;
    private final FixtureRepository fixtureRepository;
    private final FixtureTeamStatRepository fixtureTeamStatRepository;
    private final SeasonRepository seasonRepository;

    public FixtureStatisticsSyncService(
            ApiFootballClient apiFootballClient,
            FixtureRepository fixtureRepository,
            FixtureTeamStatRepository fixtureTeamStatRepository,
            SeasonRepository seasonRepository) {
        this.apiFootballClient = apiFootballClient;
        this.fixtureRepository = fixtureRepository;
        this.fixtureTeamStatRepository = fixtureTeamStatRepository;
        this.seasonRepository = seasonRepository;
    }

    @Transactional
    public void syncFixtureStatisticsByFixtureId(Long fixtureId) {
        Fixture fixture = fixtureRepository.findById(fixtureId)
                .orElseThrow(() -> new IllegalStateException("Fixture not found: " + fixtureId));

        JsonNode root = apiFootballClient.getFixtureStatistics(fixture.getExternalFixtureId());

        fixtureTeamStatRepository.deleteByFixtureId(fixture.getId());

        JsonNode response = root != null ? root.path("response") : null;
        if (response == null || !response.isArray()) {
            return;
        }

        for (JsonNode teamBlock : response) {
            Long externalTeamId = longOrNull(teamBlock.path("team"), "id");
            Team team = resolveTeamForFixture(fixture, externalTeamId);
            if (team == null) {
                continue;
            }

            JsonNode statistics = teamBlock.path("statistics");
            if (!statistics.isArray()) {
                continue;
            }

            for (JsonNode statNode : statistics) {
                String statName = textOrNull(statNode, "type");
                JsonNode valueNode = statNode.path("value");

                FixtureTeamStat stat = new FixtureTeamStat();
                stat.setFixture(fixture);
                stat.setTeam(team);
                stat.setStatName(statName);
                stat.setStatValueText(valueNode.isMissingNode() || valueNode.isNull() ? null : valueNode.asText());
                stat.setStatValueNumber(parseNumericStat(valueNode));

                fixtureTeamStatRepository.save(stat);
            }
        }
    }

    @Transactional
    public void syncRecentCompletedStatistics(int seasonYear, int limit) {
        Season season = seasonRepository.findByExternalSeasonYear(seasonYear)
                .orElseThrow(() -> new IllegalStateException("Season not found: " + seasonYear));

        List<Fixture> fixtures = fixtureRepository.findCompletedFixturesForSeason(season, PageRequest.of(0, limit));

        for (Fixture fixture : fixtures) {
            syncFixtureStatisticsByFixtureId(fixture.getId());
        }
    }

    private Team resolveTeamForFixture(Fixture fixture, Long externalTeamId) {
        if (externalTeamId == null) {
            return null;
        }
        if (fixture.getHomeTeam() != null && externalTeamId.equals(fixture.getHomeTeam().getExternalTeamId())) {
            return fixture.getHomeTeam();
        }
        if (fixture.getAwayTeam() != null && externalTeamId.equals(fixture.getAwayTeam().getExternalTeamId())) {
            return fixture.getAwayTeam();
        }
        return null;
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private Long longOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asLong();
    }

    private Double parseNumericStat(JsonNode valueNode) {
        if (valueNode == null || valueNode.isMissingNode() || valueNode.isNull()) {
            return null;
        }

        if (valueNode.isNumber()) {
            return valueNode.asDouble();
        }

        String raw = valueNode.asText();
        if (raw == null || raw.isBlank()) {
            return null;
        }

        raw = raw.trim().replace("%", "").replace(",", ".");

        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}