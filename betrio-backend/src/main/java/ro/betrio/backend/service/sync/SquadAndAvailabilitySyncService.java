package ro.betrio.backend.service.sync;

import java.time.OffsetDateTime;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;
import ro.betrio.backend.client.apifootball.ApiFootballClient;
import ro.betrio.backend.client.apifootball.dto.ApiFootballSquadItem;
import ro.betrio.backend.client.apifootball.dto.ApiFootballSquadsResponse;
import ro.betrio.backend.domain.entity.Fixture;
import ro.betrio.backend.domain.entity.FixtureAbsence;
import ro.betrio.backend.domain.entity.FixtureLineup;
import ro.betrio.backend.domain.entity.FixtureTeamSheet;
import ro.betrio.backend.domain.entity.Player;
import ro.betrio.backend.domain.entity.Season;
import ro.betrio.backend.domain.entity.Team;
import ro.betrio.backend.domain.entity.TeamPlayerSquad;
import ro.betrio.backend.repository.FixtureAbsenceRepository;
import ro.betrio.backend.repository.FixtureLineupRepository;
import ro.betrio.backend.repository.FixtureRepository;
import ro.betrio.backend.repository.FixtureTeamSheetRepository;
import ro.betrio.backend.repository.PlayerRepository;
import ro.betrio.backend.repository.SeasonRepository;
import ro.betrio.backend.repository.TeamPlayerSquadRepository;
import ro.betrio.backend.repository.TeamRepository;

@Service
public class SquadAndAvailabilitySyncService {

    private static final String PROVIDER_NAME = "API_FOOTBALL";

    private final ApiFootballClient apiFootballClient;
    private final SeasonRepository seasonRepository;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final TeamPlayerSquadRepository teamPlayerSquadRepository;
    private final FixtureRepository fixtureRepository;
    private final FixtureTeamSheetRepository fixtureTeamSheetRepository;
    private final FixtureLineupRepository fixtureLineupRepository;
    private final FixtureAbsenceRepository fixtureAbsenceRepository;

    public SquadAndAvailabilitySyncService(
            ApiFootballClient apiFootballClient,
            SeasonRepository seasonRepository,
            TeamRepository teamRepository,
            PlayerRepository playerRepository,
            TeamPlayerSquadRepository teamPlayerSquadRepository,
            FixtureRepository fixtureRepository,
            FixtureTeamSheetRepository fixtureTeamSheetRepository,
            FixtureLineupRepository fixtureLineupRepository,
            FixtureAbsenceRepository fixtureAbsenceRepository) {
        this.apiFootballClient = apiFootballClient;
        this.seasonRepository = seasonRepository;
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
        this.teamPlayerSquadRepository = teamPlayerSquadRepository;
        this.fixtureRepository = fixtureRepository;
        this.fixtureTeamSheetRepository = fixtureTeamSheetRepository;
        this.fixtureLineupRepository = fixtureLineupRepository;
        this.fixtureAbsenceRepository = fixtureAbsenceRepository;
    }

    @Transactional
    public void syncCurrentSquads(int currentSeasonYear) {
        Season currentSeason = seasonRepository.findByExternalSeasonYear(currentSeasonYear)
                .orElseThrow(() -> new IllegalStateException("Current season not found in DB: " + currentSeasonYear));

        List<Team> teams = teamRepository.findBySeason(currentSeason);

        for (Team team : teams) {
            ApiFootballSquadsResponse response = apiFootballClient.getSquadByTeam(team.getExternalTeamId());

            teamPlayerSquadRepository.deleteBySeasonIdAndTeamId(currentSeason.getId(), team.getId());

            if (response == null || response.getResponse() == null) {
                continue;
            }

            for (ApiFootballSquadItem squadItem : response.getResponse()) {
                if (squadItem.getPlayers() == null) {
                    continue;
                }

                for (ApiFootballSquadItem.PlayerInfo squadPlayer : squadItem.getPlayers()) {
                    Player player = upsertPlayerFromSquad(squadPlayer);

                    TeamPlayerSquad teamPlayerSquad = new TeamPlayerSquad();
                    teamPlayerSquad.setSeason(currentSeason);
                    teamPlayerSquad.setTeam(team);
                    teamPlayerSquad.setPlayer(player);
                    teamPlayerSquad.setShirtNumber(squadPlayer.getNumber());
                    teamPlayerSquad.setPositionName(squadPlayer.getPosition());

                    teamPlayerSquadRepository.save(teamPlayerSquad);
                }
            }
        }
    }

    @Transactional
    public void syncFixtureContextByFixtureId(Long fixtureId) {
        Fixture fixture = fixtureRepository.findById(fixtureId)
                .orElseThrow(() -> new IllegalStateException("Fixture not found: " + fixtureId));

        syncLineups(fixture);
        syncInjuries(fixture);
    }

    @Transactional
    public void syncCurrentSeasonWindow(int currentSeasonYear, int daysBack, int daysAhead) {
        Season currentSeason = seasonRepository.findByExternalSeasonYear(currentSeasonYear)
                .orElseThrow(() -> new IllegalStateException("Current season not found in DB: " + currentSeasonYear));

        OffsetDateTime from = OffsetDateTime.now().minusDays(daysBack);
        OffsetDateTime to = OffsetDateTime.now().plusDays(daysAhead);

        List<Fixture> fixtures = fixtureRepository.findBySeasonAndKickoffAtBetween(currentSeason, from, to);

        for (Fixture fixture : fixtures) {
            syncLineups(fixture);
            syncInjuries(fixture);
        }
    }

    private void syncLineups(Fixture fixture) {
        JsonNode root = apiFootballClient.getLineupsByFixture(fixture.getExternalFixtureId());

        fixtureTeamSheetRepository.deleteByFixtureId(fixture.getId());
        fixtureLineupRepository.deleteByFixtureId(fixture.getId());

        JsonNode response = root != null ? root.path("response") : null;
        if (response == null || !response.isArray()) {
            return;
        }

        for (JsonNode teamNode : response) {
            Long externalTeamId = longOrNull(teamNode.path("team"), "id");
            Team team = resolveTeamForFixture(fixture, externalTeamId);
            if (team == null) {
                continue;
            }

            FixtureTeamSheet teamSheet = new FixtureTeamSheet();
            teamSheet.setFixture(fixture);
            teamSheet.setTeam(team);
            teamSheet.setFormation(textOrNull(teamNode, "formation"));
            teamSheet.setCoachExternalId(longOrNull(teamNode.path("coach"), "id"));
            teamSheet.setCoachName(textOrNull(teamNode.path("coach"), "name"));
            fixtureTeamSheetRepository.save(teamSheet);

            JsonNode startXI = teamNode.path("startXI");
            if (startXI.isArray()) {
                for (JsonNode wrapper : startXI) {
                    saveLineupEntry(fixture, team, wrapper.path("player"), "STARTING");
                }
            }

            JsonNode substitutes = teamNode.path("substitutes");
            if (substitutes.isArray()) {
                for (JsonNode wrapper : substitutes) {
                    saveLineupEntry(fixture, team, wrapper.path("player"), "SUBSTITUTE");
                }
            }
        }
    }

    private void syncInjuries(Fixture fixture) {
        JsonNode root = apiFootballClient.getInjuriesByFixture(fixture.getExternalFixtureId());

        fixtureAbsenceRepository.deleteByFixtureId(fixture.getId());

        JsonNode response = root != null ? root.path("response") : null;
        if (response == null || !response.isArray()) {
            return;
        }

        for (JsonNode item : response) {
            Long externalTeamId = longOrNull(item.path("team"), "id");
            Team team = resolveTeamForFixture(fixture, externalTeamId);
            if (team == null) {
                continue;
            }

            JsonNode playerNode = item.path("player");
            Long externalPlayerId = longOrNull(playerNode, "id");
            String playerName = textOrNull(playerNode, "name");

            Player player = upsertMinimalPlayer(
                    externalPlayerId,
                    playerName,
                    textOrNull(playerNode, "photo")
            );

            FixtureAbsence absence = fixtureAbsenceRepository
                    .findByFixtureIdAndTeamIdAndPlayerNameAndAbsenceTypeAndReasonText(
                            fixture.getId(),
                            team.getId(),
                            playerName,
                            textOrNull(item, "type"),
                            textOrNull(item, "reason"))
                    .orElseGet(FixtureAbsence::new);

            absence.setFixture(fixture);
            absence.setTeam(team);
            absence.setPlayer(player);
            absence.setPlayerName(playerName);
            absence.setAbsenceType(textOrNull(item, "type"));
            absence.setReasonText(textOrNull(item, "reason"));

            fixtureAbsenceRepository.save(absence);
        }
    }

    private void saveLineupEntry(Fixture fixture, Team team, JsonNode playerNode, String lineupType) {
        Long externalPlayerId = longOrNull(playerNode, "id");
        String playerName = textOrNull(playerNode, "name");

        Player player = upsertMinimalPlayer(
                externalPlayerId,
                playerName,
                null
        );

        FixtureLineup lineup = new FixtureLineup();
        lineup.setFixture(fixture);
        lineup.setTeam(team);
        lineup.setPlayer(player);
        lineup.setLineupType(lineupType);
        lineup.setPlayerName(playerName);
        lineup.setShirtNumber(intOrNull(playerNode, "number"));
        lineup.setPositionCode(textOrNull(playerNode, "pos"));
        lineup.setGridCoordinate(textOrNull(playerNode, "grid"));
        lineup.setCaptainFlag(false);

        fixtureLineupRepository.save(lineup);
    }

    private Player upsertPlayerFromSquad(ApiFootballSquadItem.PlayerInfo squadPlayer) {
        Player player = playerRepository
                .findByProviderNameAndExternalPlayerId(PROVIDER_NAME, squadPlayer.getId())
                .orElseGet(Player::new);

        player.setProviderName(PROVIDER_NAME);
        player.setExternalPlayerId(squadPlayer.getId());
        player.setPlayerName(squadPlayer.getName());
        player.setAge(squadPlayer.getAge());
        player.setPhotoUrl(squadPlayer.getPhoto());

        return playerRepository.save(player);
    }

    private Player upsertMinimalPlayer(Long externalPlayerId, String playerName, String photoUrl) {
        if (externalPlayerId == null) {
            return null;
        }

        Player player = playerRepository
                .findByProviderNameAndExternalPlayerId(PROVIDER_NAME, externalPlayerId)
                .orElseGet(Player::new);

        player.setProviderName(PROVIDER_NAME);
        player.setExternalPlayerId(externalPlayerId);
        player.setPlayerName(playerName);

        if (photoUrl != null) {
            player.setPhotoUrl(photoUrl);
        }

        return playerRepository.save(player);
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

    private Integer intOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asInt();
    }
}