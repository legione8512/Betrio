package ro.betrio.backend.service.sync;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.betrio.backend.client.apifootball.ApiFootballClient;
import ro.betrio.backend.client.apifootball.dto.ApiFootballFixtureItem;
import ro.betrio.backend.client.apifootball.dto.ApiFootballFixturesResponse;
import ro.betrio.backend.client.apifootball.dto.ApiFootballLeagueItem;
import ro.betrio.backend.client.apifootball.dto.ApiFootballLeagueResponse;
import ro.betrio.backend.client.apifootball.dto.ApiFootballTeamItem;
import ro.betrio.backend.client.apifootball.dto.ApiFootballTeamsResponse;
import ro.betrio.backend.config.ApiFootballProperties;
import ro.betrio.backend.domain.entity.Competition;
import ro.betrio.backend.domain.entity.Fixture;
import ro.betrio.backend.domain.entity.FixtureTeam;
import ro.betrio.backend.domain.entity.Season;
import ro.betrio.backend.domain.entity.Team;
import ro.betrio.backend.repository.CompetitionRepository;
import ro.betrio.backend.repository.FixtureRepository;
import ro.betrio.backend.repository.FixtureTeamRepository;
import ro.betrio.backend.repository.SeasonRepository;
import ro.betrio.backend.repository.TeamRepository;

@Service
public class BootstrapSyncService {

    private static final String PROVIDER_NAME = "API_FOOTBALL";

    private final ApiFootballClient apiFootballClient;
    private final ApiFootballProperties properties;
    private final CompetitionRepository competitionRepository;
    private final SeasonRepository seasonRepository;
    private final TeamRepository teamRepository;
    private final FixtureRepository fixtureRepository;
    private final FixtureTeamRepository fixtureTeamRepository;

    public BootstrapSyncService(
            ApiFootballClient apiFootballClient,
            ApiFootballProperties properties,
            CompetitionRepository competitionRepository,
            SeasonRepository seasonRepository,
            TeamRepository teamRepository,
            FixtureRepository fixtureRepository,
            FixtureTeamRepository fixtureTeamRepository) {
        this.apiFootballClient = apiFootballClient;
        this.properties = properties;
        this.competitionRepository = competitionRepository;
        this.seasonRepository = seasonRepository;
        this.teamRepository = teamRepository;
        this.fixtureRepository = fixtureRepository;
        this.fixtureTeamRepository = fixtureTeamRepository;
    }

    @Transactional
    public void runBootstrapSync() {
        runBootstrapSync(
                properties.getLeagueId(),
                properties.getCurrentSeason(),
                properties.getPreviousSeason()
        );
    }

    @Transactional
    public void runBootstrapSync(Long leagueId, Integer currentSeason, Integer previousSeason) {
        if (leagueId == null) {
            throw new IllegalArgumentException("leagueId must not be null");
        }
        if (currentSeason == null) {
            throw new IllegalArgumentException("currentSeason must not be null");
        }

        Competition competition = syncCompetition(leagueId, currentSeason);

        if (previousSeason != null) {
            syncOneSeason(competition, leagueId, previousSeason);
        }

        syncOneSeason(competition, leagueId, currentSeason);
    }

    private void syncOneSeason(Competition competition, Long leagueId, int seasonYear) {
        Season season = syncSeason(competition, seasonYear, leagueId);
        syncTeams(season, leagueId);
        syncFixtures(season, leagueId);
    }

    private Competition syncCompetition(Long leagueId, int seasonYear) {
        ApiFootballLeagueResponse response = apiFootballClient.getLeagueDetails(leagueId, seasonYear);

        if (response == null || response.getResponse() == null || response.getResponse().isEmpty()) {
            throw new IllegalStateException("League details response is empty.");
        }

        ApiFootballLeagueItem item = response.getResponse().getFirst();

        Competition competition = competitionRepository
                .findByProviderNameAndExternalLeagueId(PROVIDER_NAME, item.getLeague().getId())
                .orElseGet(Competition::new);

        competition.setProviderName(PROVIDER_NAME);
        competition.setExternalLeagueId(item.getLeague().getId());
        competition.setCompetitionName(item.getLeague().getName());
        competition.setCompetitionType(item.getLeague().getType());
        competition.setCountryName(item.getCountry() != null ? item.getCountry().getName() : null);
        competition.setLogoUrl(item.getLeague().getLogo());

        return competitionRepository.save(competition);
    }

    private Season syncSeason(Competition competition, int seasonYear, Long leagueId) {
    	ApiFootballLeagueResponse response = apiFootballClient.getLeagueDetails(leagueId, seasonYear);

        if (response == null || response.getResponse() == null || response.getResponse().isEmpty()) {
            throw new IllegalStateException("League details response is empty.");
        }

        ApiFootballLeagueItem item = response.getResponse().getFirst();
        ApiFootballLeagueItem.SeasonInfo matchedSeason = null;

        if (item.getSeasons() != null) {
            for (ApiFootballLeagueItem.SeasonInfo seasonInfo : item.getSeasons()) {
                if (seasonInfo != null && seasonInfo.getYear() != null && seasonInfo.getYear() == seasonYear) {
                    matchedSeason = seasonInfo;
                    break;
                }
            }
        }

        Season season = seasonRepository
                .findByCompetitionAndExternalSeasonYear(competition, seasonYear)
                .orElseGet(Season::new);

        season.setCompetition(competition);
        season.setExternalSeasonYear(seasonYear);

        if (matchedSeason != null) {
            season.setStartDate(matchedSeason.getStart() != null ? LocalDate.parse(matchedSeason.getStart()) : null);
            season.setEndDate(matchedSeason.getEnd() != null ? LocalDate.parse(matchedSeason.getEnd()) : null);
            season.setCurrentFlag(Boolean.TRUE.equals(matchedSeason.getCurrent()));
        } else {
            season.setCurrentFlag(false);
        }

        return seasonRepository.save(season);
    }

    private void syncTeams(Season season, Long leagueId) {
        ApiFootballTeamsResponse response = apiFootballClient.getTeamsBySeason(leagueId, season.getExternalSeasonYear());

        if (response == null || response.getResponse() == null) {
            return;
        }

        for (ApiFootballTeamItem item : response.getResponse()) {
            Team team = teamRepository
                    .findBySeasonAndProviderNameAndExternalTeamId(season, PROVIDER_NAME, item.getTeam().getId())
                    .orElseGet(Team::new);

            team.setSeason(season);
            team.setProviderName(PROVIDER_NAME);
            team.setExternalTeamId(item.getTeam().getId());
            team.setTeamName(item.getTeam().getName());
            team.setShortCode(item.getTeam().getCode());
            team.setCountryName(item.getTeam().getCountry());
            team.setFoundedYear(item.getTeam().getFounded());
            team.setNationalFlag(Boolean.TRUE.equals(item.getTeam().getNational()));
            team.setLogoUrl(item.getTeam().getLogo());

            if (item.getVenue() != null) {
                team.setVenueName(item.getVenue().getName());
                team.setVenueCity(item.getVenue().getCity());
                team.setVenueCapacity(item.getVenue().getCapacity());
            }

            teamRepository.save(team);
        }
    }

    private void syncFixtures(Season season, Long leagueId) {
        ApiFootballFixturesResponse response = apiFootballClient.getFixturesBySeason(leagueId, season.getExternalSeasonYear());

        if (response == null || response.getResponse() == null) {
            return;
        }

        Map<Long, Team> teamByExternalId = new HashMap<>();
        for (Team team : teamRepository.findAll()) {
            if (team.getSeason().getId().equals(season.getId())) {
                teamByExternalId.put(team.getExternalTeamId(), team);
            }
        }

        for (ApiFootballFixtureItem item : response.getResponse()) {
            Fixture fixture = fixtureRepository
                    .findBySeasonAndProviderNameAndExternalFixtureId(season, PROVIDER_NAME, item.getFixture().getId())
                    .orElseGet(Fixture::new);

            fixture.setSeason(season);
            fixture.setProviderName(PROVIDER_NAME);
            fixture.setExternalFixtureId(item.getFixture().getId());
            fixture.setRefereeName(item.getFixture().getReferee());
            fixture.setTimezoneName(item.getFixture().getTimezone());
            fixture.setKickoffAt(OffsetDateTime.parse(item.getFixture().getDate()));
            fixture.setLeagueRound(item.getLeague() != null ? item.getLeague().getRound() : null);

            if (item.getFixture().getStatus() != null) {
                fixture.setStatusLong(item.getFixture().getStatus().getLongValue());
                fixture.setStatusShort(item.getFixture().getStatus().getShortValue());
                fixture.setElapsedMinutes(item.getFixture().getStatus().getElapsed());
            }

            if (item.getFixture().getVenue() != null) {
                fixture.setVenueName(item.getFixture().getVenue().getName());
                fixture.setVenueCity(item.getFixture().getVenue().getCity());
            }

            if (item.getGoals() != null) {
                fixture.setHomeGoals(item.getGoals().getHome());
                fixture.setAwayGoals(item.getGoals().getAway());
            }

            if (item.getScore() != null && item.getScore().getHalftime() != null) {
                fixture.setHalftimeHomeGoals(item.getScore().getHalftime().getHome());
                fixture.setHalftimeAwayGoals(item.getScore().getHalftime().getAway());
            }

            if (item.getScore() != null && item.getScore().getFulltime() != null) {
                fixture.setFulltimeHomeGoals(item.getScore().getFulltime().getHome());
                fixture.setFulltimeAwayGoals(item.getScore().getFulltime().getAway());
            }

            if (item.getScore() != null && item.getScore().getExtratime() != null) {
                fixture.setExtratimeHomeGoals(item.getScore().getExtratime().getHome());
                fixture.setExtratimeAwayGoals(item.getScore().getExtratime().getAway());
            }

            if (item.getScore() != null && item.getScore().getPenalty() != null) {
                fixture.setPenaltyHomeGoals(item.getScore().getPenalty().getHome());
                fixture.setPenaltyAwayGoals(item.getScore().getPenalty().getAway());
            }

            Team homeTeam = item.getTeams() != null && item.getTeams().getHome() != null
                    ? teamByExternalId.get(item.getTeams().getHome().getId())
                    : null;

            Team awayTeam = item.getTeams() != null && item.getTeams().getAway() != null
                    ? teamByExternalId.get(item.getTeams().getAway().getId())
                    : null;

            fixture.setHomeTeam(homeTeam);
            fixture.setAwayTeam(awayTeam);

            fixture = fixtureRepository.save(fixture);

            saveFixtureTeam(
                    fixture,
                    homeTeam,
                    "HOME",
                    item.getTeams() != null && item.getTeams().getHome() != null
                            ? item.getTeams().getHome().getWinner()
                            : null
            );

            saveFixtureTeam(
                    fixture,
                    awayTeam,
                    "AWAY",
                    item.getTeams() != null && item.getTeams().getAway() != null
                            ? item.getTeams().getAway().getWinner()
                            : null
            );
        }
    }

    private void saveFixtureTeam(Fixture fixture, Team team, String side, Boolean winnerFlag) {
        if (team == null) {
            return;
        }

        FixtureTeam fixtureTeam = fixtureTeamRepository
                .findByFixtureIdAndSide(fixture.getId(), side)
                .orElseGet(FixtureTeam::new);

        fixtureTeam.setFixture(fixture);
        fixtureTeam.setTeam(team);
        fixtureTeam.setSide(side);
        fixtureTeam.setWinnerFlag(winnerFlag);

        fixtureTeamRepository.save(fixtureTeam);
    }
}