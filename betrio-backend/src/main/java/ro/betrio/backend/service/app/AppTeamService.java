package ro.betrio.backend.service.app;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.betrio.backend.api.dto.app.TeamFormDto;
import ro.betrio.backend.api.dto.app.TeamOverviewDto;
import ro.betrio.backend.domain.entity.Fixture;
import ro.betrio.backend.domain.entity.Team;
import ro.betrio.backend.repository.FixtureRepository;
import ro.betrio.backend.repository.TeamRepository;

@Service
public class AppTeamService {

    private final TeamRepository teamRepository;
    private final FixtureRepository fixtureRepository;

    public AppTeamService(
            TeamRepository teamRepository,
            FixtureRepository fixtureRepository) {
        this.teamRepository = teamRepository;
        this.fixtureRepository = fixtureRepository;
    }

    @Transactional(readOnly = true)
    public TeamOverviewDto getTeamOverview(Long teamId) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Team not found: " + teamId
                        )
                );

        List<Fixture> recentFixtures =
                fixtureRepository
                        .findRecentCompletedFixturesForTeam(
                                team.getProviderName(),
                                team.getExternalTeamId(),
                                OffsetDateTime.now(),
                                PageRequest.of(0, 5)
                        );

        /*
         * Meciurile viitoare sunt din sezonul curent,
         * deci aici ID-ul intern rămâne corect.
         */
        List<Fixture> upcomingFixtures =
                fixtureRepository.findUpcomingFixturesForTeam(
                        team.getId(),
                        OffsetDateTime.now(),
                        PageRequest.of(0, 5)
                );

        TeamOverviewDto.Summary summary =
                buildOverviewSummary(team, recentFixtures);

        return new TeamOverviewDto(
                team.getId(),
                team.getTeamName(),
                summary,
                recentFixtures.stream()
                        .map(f -> toOverviewMatchItem(team, f))
                        .toList(),
                upcomingFixtures.stream()
                        .map(f -> toOverviewMatchItem(team, f))
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public List<TeamOverviewDto.MatchItem> getRecentMatches(
            Long teamId,
            int limit) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Team not found: " + teamId
                        )
                );

        List<Fixture> recentFixtures =
                fixtureRepository
                        .findRecentCompletedFixturesForTeam(
                                team.getProviderName(),
                                team.getExternalTeamId(),
                                OffsetDateTime.now(),
                                PageRequest.of(0, limit)
                        );

        return recentFixtures.stream()
                .map(f -> toOverviewMatchItem(team, f))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeamOverviewDto.MatchItem> getUpcomingMatches(
            Long teamId,
            int limit) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Team not found: " + teamId
                        )
                );

        List<Fixture> upcomingFixtures =
                fixtureRepository.findUpcomingFixturesForTeam(
                        team.getId(),
                        OffsetDateTime.now(),
                        PageRequest.of(0, limit)
                );

        return upcomingFixtures.stream()
                .map(f -> toOverviewMatchItem(team, f))
                .toList();
    }

    @Transactional(readOnly = true)
    public TeamFormDto getTeamForm(Long teamId, int limit) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalStateException("Team not found: " + teamId));

        List<Fixture> fixtures =
                fixtureRepository.findRecentCompletedFixturesForTeam(
                        team.getProviderName(),
                        team.getExternalTeamId(),
                        OffsetDateTime.now(),
                        PageRequest.of(0, limit)
                );

        TeamFormDto.FormSummary overall =
                buildFormSummary(team, fixtures);

        TeamFormDto.VenueSummary home =
                buildVenueSummary(team, fixtures, true);

        TeamFormDto.VenueSummary away =
                buildVenueSummary(team, fixtures, false);

        TeamFormDto.StreakSummary streak =
                buildStreak(team, fixtures);

        String formSequence = fixtures.stream()
        		.map(f -> resultCodeForTeam(team, f))
                .reduce("", (a, b) -> a.isEmpty() ? b : a + "-" + b);

        List<TeamFormDto.MatchFormItem> items = fixtures.stream()
        		.map(f -> toFormMatchItem(team, f))
                .toList();

        return new TeamFormDto(
                team.getId(),
                team.getTeamName(),
                fixtures.size(),
                overall,
                home,
                away,
                streak,
                formSequence,
                items
        );
    }

    private TeamOverviewDto.Summary buildOverviewSummary(Team team, List<Fixture> fixtures) {
        int wins = 0;
        int draws = 0;
        int losses = 0;
        int goalsFor = 0;
        int goalsAgainst = 0;

        for (Fixture fixture : fixtures) {
        	boolean isHome =
        	        sameTeam(team, fixture.getHomeTeam());

            int teamGoals = isHome ? safeInt(fixture.getHomeGoals()) : safeInt(fixture.getAwayGoals());
            int opponentGoals = isHome ? safeInt(fixture.getAwayGoals()) : safeInt(fixture.getHomeGoals());

            goalsFor += teamGoals;
            goalsAgainst += opponentGoals;

            if (teamGoals > opponentGoals) {
                wins++;
            } else if (teamGoals == opponentGoals) {
                draws++;
            } else {
                losses++;
            }
        }

        int count = fixtures.size();
        int points = wins * 3 + draws;

        double averageGoalsFor = count == 0 ? 0.0 : (double) goalsFor / count;
        double averageGoalsAgainst = count == 0 ? 0.0 : (double) goalsAgainst / count;

        return new TeamOverviewDto.Summary(
                count,
                wins,
                draws,
                losses,
                goalsFor,
                goalsAgainst,
                points,
                averageGoalsFor,
                averageGoalsAgainst
        );
    }

    private TeamFormDto.FormSummary buildFormSummary(Team team, List<Fixture> fixtures) {
        int wins = 0;
        int draws = 0;
        int losses = 0;
        int goalsFor = 0;
        int goalsAgainst = 0;
        int cleanSheets = 0;
        int failedToScore = 0;
        int bttsYesCount = 0;
        int over25Count = 0;

        for (Fixture fixture : fixtures) {
            int teamGoals = teamGoals(team, fixture);
            int opponentGoals = opponentGoals(team, fixture);

            goalsFor += teamGoals;
            goalsAgainst += opponentGoals;

            if (teamGoals > opponentGoals) {
                wins++;
            } else if (teamGoals == opponentGoals) {
                draws++;
            } else {
                losses++;
            }

            if (opponentGoals == 0) {
                cleanSheets++;
            }
            if (teamGoals == 0) {
                failedToScore++;
            }
            if (teamGoals > 0 && opponentGoals > 0) {
                bttsYesCount++;
            }
            if ((teamGoals + opponentGoals) > 2) {
                over25Count++;
            }
        }

        int played = fixtures.size();
        int points = wins * 3 + draws;

        return new TeamFormDto.FormSummary(
                played,
                wins,
                draws,
                losses,
                points,
                goalsFor,
                goalsAgainst,
                goalsFor - goalsAgainst,
                played == 0 ? 0.0 : (double) goalsFor / played,
                played == 0 ? 0.0 : (double) goalsAgainst / played,
                cleanSheets,
                failedToScore,
                bttsYesCount,
                over25Count
        );
    }

    private TeamFormDto.VenueSummary buildVenueSummary(Team team, List<Fixture> fixtures, boolean homeOnly) {
        int played = 0;
        int wins = 0;
        int draws = 0;
        int losses = 0;
        int goalsFor = 0;
        int goalsAgainst = 0;

        for (Fixture fixture : fixtures) {
        	boolean isHome =
        	        sameTeam(team, fixture.getHomeTeam());

            if (isHome != homeOnly) {
                continue;
            }

            played++;

            int teamGoals = teamGoals(team, fixture);
            int opponentGoals = opponentGoals(team, fixture);

            goalsFor += teamGoals;
            goalsAgainst += opponentGoals;

            if (teamGoals > opponentGoals) {
                wins++;
            } else if (teamGoals == opponentGoals) {
                draws++;
            } else {
                losses++;
            }
        }

        return new TeamFormDto.VenueSummary(
                played,
                wins,
                draws,
                losses,
                wins * 3 + draws,
                goalsFor,
                goalsAgainst
        );
    }

    private TeamFormDto.StreakSummary buildStreak(Team team, List<Fixture> fixtures) {
        if (fixtures.isEmpty()) {
            return new TeamFormDto.StreakSummary(null, 0);
        }

        String first = resultCodeForTeam(team, fixtures.get(0));
        int length = 0;

        for (Fixture fixture : fixtures) {
            String current = resultCodeForTeam(team, fixture);
            if (!first.equals(current)) {
                break;
            }
            length++;
        }

        return new TeamFormDto.StreakSummary(first, length);
    }

    private TeamOverviewDto.MatchItem toOverviewMatchItem(Team team, Fixture fixture) {
    	boolean isHome =
    	        sameTeam(team, fixture.getHomeTeam());

        String opponentName = isHome
                ? fixture.getAwayTeam().getTeamName()
                : fixture.getHomeTeam().getTeamName();

        Integer teamGoals = isHome ? fixture.getHomeGoals() : fixture.getAwayGoals();
        Integer opponentGoals = isHome ? fixture.getAwayGoals() : fixture.getHomeGoals();

        String resultCode = null;
        if (teamGoals != null && opponentGoals != null
                && isCompleted(fixture)) {
            if (teamGoals > opponentGoals) {
                resultCode = "W";
            } else if (teamGoals.equals(opponentGoals)) {
                resultCode = "D";
            } else {
                resultCode = "L";
            }
        }

        return new TeamOverviewDto.MatchItem(
                fixture.getId(),
                fixture.getKickoffAt(),
                fixture.getStatusShort(),
                fixture.getStatusLong(),
                fixture.getLeagueRound(),
                isHome,
                opponentName,
                teamGoals,
                opponentGoals,
                resultCode
        );
    }

    private TeamFormDto.MatchFormItem toFormMatchItem(Team team, Fixture fixture) {
    	boolean isHome =
    	        sameTeam(team, fixture.getHomeTeam());
        int teamGoals = teamGoals(team, fixture);
        int opponentGoals = opponentGoals(team, fixture);

        return new TeamFormDto.MatchFormItem(
                fixture.getId(),
                fixture.getKickoffAt(),
                isHome,
                isHome ? fixture.getAwayTeam().getTeamName() : fixture.getHomeTeam().getTeamName(),
                teamGoals,
                opponentGoals,
                resultCodeForTeam(team, fixture),
                opponentGoals == 0,
                teamGoals == 0,
                teamGoals > 0 && opponentGoals > 0,
                (teamGoals + opponentGoals) > 2
        );
    }

    private String resultCodeForTeam(Team team, Fixture fixture) {
        int teamGoals = teamGoals(team, fixture);
        int opponentGoals = opponentGoals(team, fixture);

        if (teamGoals > opponentGoals) {
            return "W";
        }
        if (teamGoals < opponentGoals) {
            return "L";
        }
        return "D";
    }

    private int teamGoals(
            Team team,
            Fixture fixture) {

        boolean isHome =
                sameTeam(team, fixture.getHomeTeam());

        return isHome
                ? safeInt(fixture.getHomeGoals())
                : safeInt(fixture.getAwayGoals());
    }

    private int opponentGoals(
            Team team,
            Fixture fixture) {

        boolean isHome =
                sameTeam(team, fixture.getHomeTeam());

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

    private boolean isCompleted(Fixture fixture) {
        String status = fixture.getStatusShort();
        return "FT".equals(status) || "AET".equals(status) || "PEN".equals(status);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}