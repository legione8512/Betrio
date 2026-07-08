package ro.betrio.backend.service.app;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.betrio.backend.api.dto.app.LeagueStandingsDto;
import ro.betrio.backend.domain.entity.Fixture;
import ro.betrio.backend.domain.entity.Season;
import ro.betrio.backend.domain.entity.Team;
import ro.betrio.backend.repository.FixtureRepository;
import ro.betrio.backend.repository.SeasonRepository;
import ro.betrio.backend.repository.TeamRepository;

@Service
public class AppStandingsService {

    private final SeasonRepository seasonRepository;
    private final TeamRepository teamRepository;
    private final FixtureRepository fixtureRepository;

    public AppStandingsService(
            SeasonRepository seasonRepository,
            TeamRepository teamRepository,
            FixtureRepository fixtureRepository) {
        this.seasonRepository = seasonRepository;
        this.teamRepository = teamRepository;
        this.fixtureRepository = fixtureRepository;
    }

    @Transactional(readOnly = true)
    public LeagueStandingsDto getCurrentStandings(Long competitionId) {
        Season season = resolveBestCurrentSeason(competitionId);
        return buildStandings(season);
    }

    @Transactional(readOnly = true)
    public LeagueStandingsDto getStandingsForSeason(Long seasonId) {
        Season season = seasonRepository.findDetailedById(seasonId)
                .orElseThrow(() -> new IllegalStateException("Season not found: " + seasonId));

        return buildStandings(season);
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

    private LeagueStandingsDto buildStandings(Season season) {
        List<Fixture> fixtures = fixtureRepository.findAllCompletedFixturesForSeason(season);

        Map<Long, StandingAccumulator> table = new LinkedHashMap<>();

        for (Team team : teamRepository.findBySeason(season)) {
            table.put(team.getId(), new StandingAccumulator(team.getId(), team.getTeamName()));
        }

        for (Fixture fixture : fixtures) {
            if (fixture.getHomeTeam() != null) {
                table.putIfAbsent(
                        fixture.getHomeTeam().getId(),
                        new StandingAccumulator(
                                fixture.getHomeTeam().getId(),
                                fixture.getHomeTeam().getTeamName()
                        )
                );
            }

            if (fixture.getAwayTeam() != null) {
                table.putIfAbsent(
                        fixture.getAwayTeam().getId(),
                        new StandingAccumulator(
                                fixture.getAwayTeam().getId(),
                                fixture.getAwayTeam().getTeamName()
                        )
                );
            }
        }

        for (Fixture fixture : fixtures) {
            if (fixture.getHomeTeam() == null || fixture.getAwayTeam() == null) {
                continue;
            }
            if (fixture.getHomeGoals() == null || fixture.getAwayGoals() == null) {
                continue;
            }

            StandingAccumulator home = table.get(fixture.getHomeTeam().getId());
            StandingAccumulator away = table.get(fixture.getAwayTeam().getId());

            if (home == null || away == null) {
                continue;
            }

            int homeGoals = fixture.getHomeGoals();
            int awayGoals = fixture.getAwayGoals();

            home.played++;
            away.played++;

            home.goalsFor += homeGoals;
            home.goalsAgainst += awayGoals;

            away.goalsFor += awayGoals;
            away.goalsAgainst += homeGoals;

            if (homeGoals > awayGoals) {
                home.wins++;
                home.points += 3;
                away.losses++;
                home.form.add("W");
                away.form.add("L");
            } else if (homeGoals < awayGoals) {
                away.wins++;
                away.points += 3;
                home.losses++;
                home.form.add("L");
                away.form.add("W");
            } else {
                home.draws++;
                away.draws++;
                home.points += 1;
                away.points += 1;
                home.form.add("D");
                away.form.add("D");
            }
        }

        List<StandingAccumulator> sorted = new ArrayList<>(table.values());
        sorted.sort(
                Comparator.comparingInt(StandingAccumulator::getPoints).reversed()
                        .thenComparingInt(StandingAccumulator::getGoalDifference).reversed()
                        .thenComparingInt(StandingAccumulator::getGoalsFor).reversed()
                        .thenComparing(acc -> acc.teamName, String.CASE_INSENSITIVE_ORDER)
        );

        List<LeagueStandingsDto.Row> rows = new ArrayList<>();
        int position = 1;

        for (StandingAccumulator acc : sorted) {
            rows.add(new LeagueStandingsDto.Row(
                    position++,
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
                    acc.lastFiveForm()
            ));
        }

        return new LeagueStandingsDto(
                season.getId(),
                season.getExternalSeasonYear(),
                season.getCompetition().getCompetitionName(),
                rows
        );
    }

    private static final class StandingAccumulator {
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

        private StandingAccumulator(Long teamId, String teamName) {
            this.teamId = Objects.requireNonNull(teamId);
            this.teamName = Objects.requireNonNull(teamName);
        }

        private int getPoints() {
            return points;
        }

        private int getGoalsFor() {
            return goalsFor;
        }

        private int getGoalDifference() {
            return goalsFor - goalsAgainst;
        }

        private String lastFiveForm() {
            if (form.isEmpty()) {
                return "";
            }
            int start = Math.max(0, form.size() - 5);
            return String.join("-", form.subList(start, form.size()));
        }
    }
}