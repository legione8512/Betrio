package ro.betrio.backend.service.app;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.betrio.backend.api.dto.app.LeagueFormTableDto;
import ro.betrio.backend.domain.entity.Fixture;
import ro.betrio.backend.domain.entity.Season;
import ro.betrio.backend.domain.entity.Team;
import ro.betrio.backend.repository.FixtureRepository;
import ro.betrio.backend.repository.SeasonRepository;
import ro.betrio.backend.repository.TeamRepository;

@Service
public class AppFormTableService {

    private final SeasonRepository seasonRepository;
    private final TeamRepository teamRepository;
    private final FixtureRepository fixtureRepository;

    public AppFormTableService(
            SeasonRepository seasonRepository,
            TeamRepository teamRepository,
            FixtureRepository fixtureRepository) {
        this.seasonRepository = seasonRepository;
        this.teamRepository = teamRepository;
        this.fixtureRepository = fixtureRepository;
    }

    @Transactional(readOnly = true)
    public LeagueFormTableDto getCurrentFormTable(Long competitionId, int limit) {
        Season season = resolveBestCurrentSeason(competitionId);
        return buildFormTable(season, limit);
    }

    @Transactional(readOnly = true)
    public LeagueFormTableDto getFormTableForSeason(Long seasonId, int limit) {
        Season season = seasonRepository.findDetailedById(seasonId)
                .orElseThrow(() -> new IllegalStateException("Season not found: " + seasonId));

        return buildFormTable(season, limit);
    }

    private Season resolveBestCurrentSeason(Long competitionId) {
    	if (competitionId != null) {
            List<Season> current = seasonRepository.findCurrentSeasonsByCompetition(competitionId);

            for (Season season : current) {
                if (hasUsableData(season)) {
                    return season;
                }
            }

            return seasonRepository.findSeasonsWithCompletedFixturesByCompetition(competitionId)
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "No season with completed fixtures found for competitionId=" + competitionId));
        }

        List<Season> current = seasonRepository.findCurrentSeasonsWithCompetition();

        for (Season season : current) {
            if (hasUsableData(season)) {
                return season;
            }
        }

        return seasonRepository.findSeasonsWithCompletedFixtures()
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No season with completed fixtures found"));
    }

    private boolean hasUsableData(Season season) {
        boolean hasTeams = !teamRepository.findBySeason(season).isEmpty();
        boolean hasFixtures = !fixtureRepository.findAllCompletedFixturesForSeason(season).isEmpty();
        return hasTeams || hasFixtures;
    }

    private LeagueFormTableDto buildFormTable(Season season, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        List<Fixture> seasonFixtures = fixtureRepository.findAllCompletedFixturesForSeason(season);

        Map<Long, Team> teams = new LinkedHashMap<>();

        for (Team team : teamRepository.findBySeason(season)) {
            teams.put(team.getId(), team);
        }

        for (Fixture fixture : seasonFixtures) {
            if (fixture.getHomeTeam() != null) {
                teams.putIfAbsent(
                        fixture.getHomeTeam().getId(),
                        fixture.getHomeTeam()
                );
            }

            if (fixture.getAwayTeam() != null) {
                teams.putIfAbsent(
                        fixture.getAwayTeam().getId(),
                        fixture.getAwayTeam()
                );
            }
        }

        List<LeagueFormTableDto.Row> rows = new ArrayList<>();

        for (Map.Entry<Long, Team> entry : teams.entrySet()) {

            Team targetTeam = entry.getValue();

            Long teamId = targetTeam.getId();
            String teamName = targetTeam.getTeamName();

            List<Fixture> recentFixtures =
                    fixtureRepository.findRecentCompletedFixturesForTeam(
                            targetTeam.getProviderName(),
                            targetTeam.getExternalTeamId(),
                            OffsetDateTime.now(),
                            PageRequest.of(0, safeLimit)
                    );

            FormAccumulator acc =
                    new FormAccumulator(teamId, teamName);

            for (Fixture fixture : recentFixtures) {
            	int teamGoals =
            	        teamGoals(targetTeam, fixture);

            	int opponentGoals =
            	        opponentGoals(targetTeam, fixture);

                acc.played++;
                acc.goalsFor += teamGoals;
                acc.goalsAgainst += opponentGoals;

                if (teamGoals > opponentGoals) {
                    acc.wins++;
                    acc.points += 3;
                    acc.form.add("W");
                } else if (teamGoals < opponentGoals) {
                    acc.losses++;
                    acc.form.add("L");
                } else {
                    acc.draws++;
                    acc.points += 1;
                    acc.form.add("D");
                }
            }

            rows.add(new LeagueFormTableDto.Row(
                    0,
                    acc.teamId,
                    acc.teamName,
                    acc.played,
                    acc.wins,
                    acc.draws,
                    acc.losses,
                    acc.goalsFor,
                    acc.goalsAgainst,
                    acc.getGoalDifference(),
                    acc.points,
                    acc.formSequence()
            ));
        }

        rows.sort(
                Comparator.comparingInt(LeagueFormTableDto.Row::points).reversed()
                        .thenComparingInt(LeagueFormTableDto.Row::goalDifference).reversed()
                        .thenComparingInt(LeagueFormTableDto.Row::goalsFor).reversed()
                        .thenComparing(LeagueFormTableDto.Row::teamName, String.CASE_INSENSITIVE_ORDER)
        );

        List<LeagueFormTableDto.Row> rankedRows = new ArrayList<>();
        int position = 1;
        for (LeagueFormTableDto.Row row : rows) {
            rankedRows.add(new LeagueFormTableDto.Row(
                    position++,
                    row.teamId(),
                    row.teamName(),
                    row.played(),
                    row.wins(),
                    row.draws(),
                    row.losses(),
                    row.goalsFor(),
                    row.goalsAgainst(),
                    row.goalDifference(),
                    row.points(),
                    row.formSequence()
            ));
        }

        return new LeagueFormTableDto(
                season.getId(),
                season.getExternalSeasonYear(),
                season.getCompetition().getCompetitionName(),
                safeLimit,
                rankedRows
        );
    }

    private int teamGoals(
            Team targetTeam,
            Fixture fixture) {

        boolean isHome = sameTeam(
                targetTeam,
                fixture.getHomeTeam()
        );

        return isHome
                ? safeInt(fixture.getHomeGoals())
                : safeInt(fixture.getAwayGoals());
    }

    private int opponentGoals(
            Team targetTeam,
            Fixture fixture) {

        boolean isHome = sameTeam(
                targetTeam,
                fixture.getHomeTeam()
        );

        return isHome
                ? safeInt(fixture.getAwayGoals())
                : safeInt(fixture.getHomeGoals());
    }
    
    private boolean sameTeam(
            Team first,
            Team second) {

        return first != null
                && second != null
                && Objects.equals(
                        first.getProviderName(),
                        second.getProviderName()
                )
                && Objects.equals(
                        first.getExternalTeamId(),
                        second.getExternalTeamId()
                );
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private static final class FormAccumulator {
        private final Long teamId;
        private final String teamName;
        private int played;
        private int wins;
        private int draws;
        private int losses;
        private int goalsFor;
        private int goalsAgainst;
        private int points;
        private final List<String> form = new ArrayList<>();

        private FormAccumulator(Long teamId, String teamName) {
            this.teamId = Objects.requireNonNull(teamId);
            this.teamName = Objects.requireNonNull(teamName);
        }

        private int getGoalDifference() {
            return goalsFor - goalsAgainst;
        }

        private String formSequence() {
            return String.join("-", form);
        }
    }
}