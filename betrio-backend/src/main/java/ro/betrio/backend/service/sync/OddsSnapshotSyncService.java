package ro.betrio.backend.service.sync;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;

import ro.betrio.backend.client.apifootball.ApiFootballClient;
import ro.betrio.backend.domain.entity.Fixture;
import ro.betrio.backend.domain.entity.OddsSnapshot;
import ro.betrio.backend.repository.FixtureRepository;
import ro.betrio.backend.repository.OddsSnapshotRepository;

@Service
public class OddsSnapshotSyncService {

    private final ApiFootballClient apiFootballClient;
    private final FixtureRepository fixtureRepository;
    private final OddsSnapshotRepository oddsSnapshotRepository;

    public OddsSnapshotSyncService(
            ApiFootballClient apiFootballClient,
            FixtureRepository fixtureRepository,
            OddsSnapshotRepository oddsSnapshotRepository) {
        this.apiFootballClient = apiFootballClient;
        this.fixtureRepository = fixtureRepository;
        this.oddsSnapshotRepository = oddsSnapshotRepository;
    }

    @Transactional
    public void captureOddsForFixture(Long fixtureId) {
        Fixture fixture = fixtureRepository.findById(fixtureId)
                .orElseThrow(() -> new IllegalStateException("Fixture not found: " + fixtureId));

        JsonNode root = apiFootballClient.getOddsByFixture(fixture.getExternalFixtureId());

        oddsSnapshotRepository.deleteByFixtureId(fixture.getId());

        JsonNode response = root != null ? root.path("response") : null;
        if (response == null || !response.isArray()) {
            return;
        }

        for (JsonNode fixtureNode : response) {
            JsonNode bookmakers = fixtureNode.path("bookmakers");
            if (!bookmakers.isArray()) {
                continue;
            }

            for (JsonNode bookmaker : bookmakers) {
                Long bookmakerId = longOrNull(bookmaker, "id");
                String bookmakerName = textOrNull(bookmaker, "name");

                JsonNode bets = bookmaker.path("bets");
                if (!bets.isArray()) {
                    continue;
                }

                for (JsonNode bet : bets) {
                    String marketName = textOrNull(bet, "name");

                    JsonNode values = bet.path("values");
                    if (!values.isArray()) {
                        continue;
                    }

                    for (JsonNode value : values) {
                        OddsSnapshot row = new OddsSnapshot();
                        row.setFixture(fixture);
                        row.setBookmakerId(bookmakerId);
                        row.setBookmakerName(bookmakerName);
                        row.setMarketName(marketName);
                        row.setOutcomeName(textOrNull(value, "value"));
                        row.setOddValue(doubleOrNull(value, "odd"));

                        oddsSnapshotRepository.save(row);
                    }
                }
            }
        }
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private Long longOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asLong();
    }

    private Double doubleOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        try {
            return Double.parseDouble(value.asText());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}