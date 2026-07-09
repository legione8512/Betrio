package ro.betrio.backend.service.analysis;

import java.time.OffsetDateTime;
import ro.betrio.backend.api.dto.TeamStrengthSnapshotDto;
import java.time.temporal.ChronoUnit;
import ro.betrio.backend.api.dto.TeamScheduleContextDto;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.betrio.backend.api.dto.HeadToHeadSnapshotDto;
import ro.betrio.backend.api.dto.LeagueBaselineSnapshotDto;
import ro.betrio.backend.api.dto.MatchFeatureSnapshotDto;
import ro.betrio.backend.api.dto.TeamFormSnapshotDto;
import ro.betrio.backend.domain.entity.Fixture;
import ro.betrio.backend.domain.entity.FixtureTeamStat;
import ro.betrio.backend.domain.entity.Team;
import ro.betrio.backend.repository.FixtureAbsenceRepository;
import ro.betrio.backend.repository.FixtureRepository;
import ro.betrio.backend.repository.FixtureTeamStatRepository;

@Service
public class FeatureBuilderService {

    private static final int FORM_LIMIT = 5;
    private static final int LEAGUE_BASELINE_LIMIT = 80;
    private static final int MIN_VENUE_SAMPLE_SIZE = 3;

    private final FixtureRepository fixtureRepository;
    private final FixtureTeamStatRepository fixtureTeamStatRepository;
    private final FixtureAbsenceRepository fixtureAbsenceRepository;

    public FeatureBuilderService(
            FixtureRepository fixtureRepository,
            FixtureTeamStatRepository fixtureTeamStatRepository,
            FixtureAbsenceRepository fixtureAbsenceRepository) {
        this.fixtureRepository = fixtureRepository;
        this.fixtureTeamStatRepository = fixtureTeamStatRepository;
        this.fixtureAbsenceRepository = fixtureAbsenceRepository;
    }

    @Transactional(readOnly = true)
    public MatchFeatureSnapshotDto buildForFixture(Long fixtureId) {
        Fixture fixture = fixtureRepository.findDetailedById(fixtureId)
                .orElseThrow(() -> new IllegalStateException("Fixture not found: " + fixtureId));

        if (fixture.getHomeTeam() == null || fixture.getAwayTeam() == null) {
            throw new IllegalStateException("Fixture is missing homeTeam or awayTeam.");
        }

        OffsetDateTime before = fixture.getKickoffAt();

        Team homeTeam = fixture.getHomeTeam();
        Team awayTeam = fixture.getAwayTeam();

        TeamFormSnapshotDto homeForm =
                buildTeamForm(homeTeam, fixtureId, before);

        TeamFormSnapshotDto awayForm =
                buildTeamForm(awayTeam, fixtureId, before);

        TeamFormSnapshotDto homeVenueForm =
                buildVenueTeamForm(homeTeam, fixtureId, before, true);

        TeamFormSnapshotDto awayVenueForm =
                buildVenueTeamForm(awayTeam, fixtureId, before, false);

        TeamFormSnapshotDto homeModelForm =
                chooseVenueForm(homeVenueForm, homeForm);

        TeamFormSnapshotDto awayModelForm =
                chooseVenueForm(awayVenueForm, awayForm);
        
        TeamScheduleContextDto homeScheduleContext =
                buildScheduleContext(homeTeam, before);

        TeamScheduleContextDto awayScheduleContext =
                buildScheduleContext(awayTeam, before);
        
        TeamStrengthSnapshotDto homeStrength =
                buildTeamStrength(fixture, homeTeam, before);

        TeamStrengthSnapshotDto awayStrength =
                buildTeamStrength(fixture, awayTeam, before);

        HeadToHeadSnapshotDto h2h =
                buildHeadToHead(homeTeam, awayTeam, before);

        LeagueBaselineSnapshotDto leagueBaseline =
                buildLeagueBaseline(fixture, before);

        double expectedHomeGoals =
                expectedGoals(
                        homeModelForm,
                        awayModelForm,
                        h2h,
                        leagueBaseline,
                        true,
                        homeScheduleContext,
                        awayScheduleContext,
                        homeStrength,
                        awayStrength
                );

        double expectedAwayGoals =
                expectedGoals(
                        awayModelForm,
                        homeModelForm,
                        h2h,
                        leagueBaseline,
                        false,
                        awayScheduleContext,
                        homeScheduleContext,
                        awayStrength,
                        homeStrength
                );
        
        return new MatchFeatureSnapshotDto(
                fixture.getId(),
                homeTeam.getTeamName(),
                awayTeam.getTeamName(),
                fixture.getKickoffAt().toString(),
                homeForm,
                awayForm,
                homeVenueForm,
                awayVenueForm,
                homeScheduleContext,
                awayScheduleContext,
                homeStrength,
                awayStrength,
                h2h,
                leagueBaseline,
                expectedHomeGoals,
                expectedAwayGoals
        );
    }

    private TeamFormSnapshotDto buildTeamForm(
            Team targetTeam,
            Long fixtureId,
            OffsetDateTime before) {

        String providerName = targetTeam.getProviderName();
        Long externalTeamId = targetTeam.getExternalTeamId();

        List<Fixture> fixtures =
                fixtureRepository.findRecentCompletedFixturesForTeam(
                        providerName,
                        externalTeamId,
                        before,
                        PageRequest.of(0, FORM_LIMIT)
                );

        return buildTeamFormFromFixtures(targetTeam, fixtureId, fixtures);
    }
    private TeamStrengthSnapshotDto buildTeamStrength(
            Fixture fixture,
            Team team,
            OffsetDateTime before) {

        if (fixture.getSeason() == null
                || fixture.getSeason().getCompetition() == null
                || fixture.getSeason().getCompetition().getId() == null) {

            return defaultTeamStrength(team);
        }

        List<Fixture> fixtures =
                fixtureRepository.findCompletedFixturesForCompetitionBefore(
                        fixture.getSeason().getCompetition().getId(),
                        before,
                        PageRequest.of(0, LEAGUE_BASELINE_LIMIT)
                );

        String providerName = team.getProviderName();
        Long externalTeamId = team.getExternalTeamId();

        int sample = 0;
        double points = 0;
        double goalsFor = 0;
        double goalsAgainst = 0;

        for (Fixture historicalFixture : fixtures) {
            boolean wasHome =
                    sameTeam(
                            historicalFixture.getHomeTeam(),
                            providerName,
                            externalTeamId
                    );

            boolean wasAway =
                    sameTeam(
                            historicalFixture.getAwayTeam(),
                            providerName,
                            externalTeamId
                    );

            if (!wasHome && !wasAway) {
                continue;
            }

            int gf = safeInt(
                    wasHome
                            ? historicalFixture.getHomeGoals()
                            : historicalFixture.getAwayGoals()
            );

            int ga = safeInt(
                    wasHome
                            ? historicalFixture.getAwayGoals()
                            : historicalFixture.getHomeGoals()
            );

            sample++;
            goalsFor += gf;
            goalsAgainst += ga;

            if (gf > ga) {
                points += 3;
            } else if (gf == ga) {
                points += 1;
            }
        }

        if (sample == 0) {
            return defaultTeamStrength(team);
        }

        double pointsPerGame = safeRate(points, sample);
        double goalsForPerGame = safeRate(goalsFor, sample);
        double goalsAgainstPerGame = safeRate(goalsAgainst, sample);
        double goalDifferencePerGame =
                goalsForPerGame - goalsAgainstPerGame;

        double strengthScore =
                clamp(
                        (0.18 * (pointsPerGame - 1.35))
                                + (0.12 * goalDifferencePerGame),
                        -0.35,
                        0.35
                );

        return new TeamStrengthSnapshotDto(
                team.getId(),
                team.getTeamName(),
                sample,
                pointsPerGame,
                goalsForPerGame,
                goalsAgainstPerGame,
                goalDifferencePerGame,
                strengthScore
        );
    }
    
    private TeamStrengthSnapshotDto defaultTeamStrength(Team team) {
        return new TeamStrengthSnapshotDto(
                team.getId(),
                team.getTeamName(),
                0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0
        );
    }

    private TeamFormSnapshotDto buildVenueTeamForm(
            Team targetTeam,
            Long fixtureId,
            OffsetDateTime before,
            boolean homeVenue) {

        String providerName = targetTeam.getProviderName();
        Long externalTeamId = targetTeam.getExternalTeamId();

        List<Fixture> fixtures = homeVenue
                ? fixtureRepository.findRecentCompletedHomeFixturesForTeam(
                        providerName,
                        externalTeamId,
                        before,
                        PageRequest.of(0, FORM_LIMIT)
                )
                : fixtureRepository.findRecentCompletedAwayFixturesForTeam(
                        providerName,
                        externalTeamId,
                        before,
                        PageRequest.of(0, FORM_LIMIT)
                );

        return buildTeamFormFromFixtures(targetTeam, fixtureId, fixtures);
    }

    private TeamFormSnapshotDto buildTeamFormFromFixtures(
            Team targetTeam,
            Long fixtureId,
            List<Fixture> fixtures) {

        String providerName = targetTeam.getProviderName();
        Long externalTeamId = targetTeam.getExternalTeamId();

        Map<Long, List<FixtureTeamStat>> statsByFixtureId =
                loadStatsByFixture(
                        fixtures,
                        providerName,
                        externalTeamId
                );

        int wins = 0;
        int draws = 0;
        int losses = 0;
        int points = 0;
        int sample = fixtures.size();

        double weightedPoints = 0;
        double goalsFor = 0;
        double goalsAgainst = 0;
        double bttsMatches = 0;
        double over25Matches = 0;
        double cleanSheets = 0;
        double totalWeight = 0;

        double possessionSum = 0;
        double possessionWeight = 0;

        double shotsOnGoalSum = 0;
        double shotsOnGoalWeight = 0;

        for (int index = 0; index < fixtures.size(); index++) {
            Fixture historicalFixture = fixtures.get(index);
            double weight = recencyWeight(index);

            boolean isHome = sameTeam(
                    historicalFixture.getHomeTeam(),
                    providerName,
                    externalTeamId
            );

            int gf = safeInt(
                    isHome
                            ? historicalFixture.getHomeGoals()
                            : historicalFixture.getAwayGoals()
            );

            int ga = safeInt(
                    isHome
                            ? historicalFixture.getAwayGoals()
                            : historicalFixture.getHomeGoals()
            );

            goalsFor += gf * weight;
            goalsAgainst += ga * weight;
            totalWeight += weight;

            if (gf > ga) {
                wins++;
                points += 3;
                weightedPoints += 3 * weight;
            } else if (gf == ga) {
                draws++;
                points += 1;
                weightedPoints += weight;
            } else {
                losses++;
            }

            if (gf > 0 && ga > 0) {
                bttsMatches += weight;
            }

            if (gf + ga > 2) {
                over25Matches += weight;
            }

            if (ga == 0) {
                cleanSheets += weight;
            }

            List<FixtureTeamStat> stats =
                    statsByFixtureId.getOrDefault(
                            historicalFixture.getId(),
                            List.of()
                    );

            Double possession =
                    getNumericStat(stats, "Ball Possession");

            if (possession != null) {
                possessionSum += possession * weight;
                possessionWeight += weight;
            }

            Double shotsOnGoal =
                    getNumericStat(stats, "Shots on Goal");

            if (shotsOnGoal != null) {
                shotsOnGoalSum += shotsOnGoal * weight;
                shotsOnGoalWeight += weight;
            }
        }

        long knownAbsences =
                fixtureAbsenceRepository.countByFixtureIdAndTeamId(
                        fixtureId,
                        targetTeam.getId()
                );

        long missingFixtureAbsences =
                fixtureAbsenceRepository.countByFixtureIdAndTeamIdAndAbsenceType(
                        fixtureId,
                        targetTeam.getId(),
                        "Missing Fixture"
                );

        long questionableAbsences =
                fixtureAbsenceRepository.countByFixtureIdAndTeamIdAndAbsenceType(
                        fixtureId,
                        targetTeam.getId(),
                        "Questionable"
                );

        double absenceImpactScore =
                missingFixtureAbsences + (0.5 * questionableAbsences);

        return new TeamFormSnapshotDto(
                targetTeam.getId(),
                targetTeam.getTeamName(),
                sample,
                wins,
                draws,
                losses,
                points,
                safeRate(weightedPoints, totalWeight),
                safeRate(goalsFor, totalWeight),
                safeRate(goalsAgainst, totalWeight),
                safeRate(bttsMatches, totalWeight),
                safeRate(over25Matches, totalWeight),
                safeRate(cleanSheets, totalWeight),
                safeRate(possessionSum, possessionWeight),
                safeRate(shotsOnGoalSum, shotsOnGoalWeight),
                knownAbsences,
                missingFixtureAbsences,
                questionableAbsences,
                absenceImpactScore
        );
    }
    
    private double recencyWeight(int index) {
        return Math.max(1.0, FORM_LIMIT - index);
    }
    
    private TeamScheduleContextDto buildScheduleContext(
            Team team,
            OffsetDateTime before) {

        List<Fixture> fixtures =
                fixtureRepository.findRecentCompletedFixturesForTeam(
                        team.getProviderName(),
                        team.getExternalTeamId(),
                        before,
                        PageRequest.of(0, 1)
                );

        if (fixtures.isEmpty()
                || fixtures.get(0).getKickoffAt() == null
                || before == null) {

            return new TeamScheduleContextDto(
                    team.getId(),
                    team.getTeamName(),
                    null,
                    -1,
                    false,
                    false,
                    0.0
            );
        }

        OffsetDateTime lastMatchAt =
                fixtures.get(0).getKickoffAt();

        long rawRestDays =
                ChronoUnit.DAYS.between(lastMatchAt, before);

        int restDays =
                clampInt(rawRestDays, 0, 30);

        return new TeamScheduleContextDto(
                team.getId(),
                team.getTeamName(),
                lastMatchAt.toString(),
                restDays,
                restDays <= 3,
                restDays >= 13,
                restScore(restDays)
        );
    }
    
    private double restScore(int restDays) {
        if (restDays < 0) {
            return 0.0;
        }

        if (restDays <= 2) {
            return -1.0;
        }

        if (restDays == 3) {
            return -0.5;
        }

        if (restDays <= 7) {
            return 0.0;
        }

        if (restDays <= 12) {
            return 0.25;
        }

        return -0.10;
    }

    private int clampInt(long value, int min, int max) {
        return (int) Math.max(min, Math.min(max, value));
    }

    private TeamFormSnapshotDto chooseVenueForm(
            TeamFormSnapshotDto venueForm,
            TeamFormSnapshotDto overallForm) {

        if (venueForm.sampleSize() >= MIN_VENUE_SAMPLE_SIZE) {
            return venueForm;
        }

        return overallForm;
    }

    private HeadToHeadSnapshotDto buildHeadToHead(
            Team requestedHomeTeam,
            Team requestedAwayTeam,
            OffsetDateTime before) {

        String providerName =
                requestedHomeTeam.getProviderName();

        if (!Objects.equals(
                providerName,
                requestedAwayTeam.getProviderName())) {

            throw new IllegalStateException(
                    "The two teams use different providers."
            );
        }

        Long homeExternalTeamId =
                requestedHomeTeam.getExternalTeamId();

        Long awayExternalTeamId =
                requestedAwayTeam.getExternalTeamId();

        List<Fixture> fixtures =
                fixtureRepository.findRecentHeadToHead(
                        providerName,
                        homeExternalTeamId,
                        awayExternalTeamId,
                        before,
                        PageRequest.of(0, FORM_LIMIT)
                );

        int homeWins = 0;
        int draws = 0;
        int awayWins = 0;
        int sample = fixtures.size();

        double homePoints = 0;
        double awayPoints = 0;

        for (Fixture historicalFixture : fixtures) {
            int actualHomeGoals =
                    safeInt(historicalFixture.getHomeGoals());

            int actualAwayGoals =
                    safeInt(historicalFixture.getAwayGoals());

            boolean requestedHomeWasActualHome =
                    sameTeam(
                            historicalFixture.getHomeTeam(),
                            providerName,
                            homeExternalTeamId
                    );

            int requestedHomeGoals;
            int requestedAwayGoals;

            if (requestedHomeWasActualHome) {
                requestedHomeGoals = actualHomeGoals;
                requestedAwayGoals = actualAwayGoals;
            } else {
                requestedHomeGoals = actualAwayGoals;
                requestedAwayGoals = actualHomeGoals;
            }

            if (requestedHomeGoals > requestedAwayGoals) {
                homeWins++;
                homePoints += 3;
            } else if (requestedHomeGoals == requestedAwayGoals) {
                draws++;
                homePoints += 1;
                awayPoints += 1;
            } else {
                awayWins++;
                awayPoints += 3;
            }
        }

        return new HeadToHeadSnapshotDto(
                sample,
                homeWins,
                draws,
                awayWins,
                safeRate(homePoints, sample),
                safeRate(awayPoints, sample)
        );
    }

    private LeagueBaselineSnapshotDto buildLeagueBaseline(
            Fixture fixture,
            OffsetDateTime before) {

        if (fixture.getSeason() == null
                || fixture.getSeason().getCompetition() == null
                || fixture.getSeason().getCompetition().getId() == null) {
            return defaultLeagueBaseline();
        }

        Long competitionId =
                fixture.getSeason().getCompetition().getId();

        List<Fixture> fixtures =
                fixtureRepository.findCompletedFixturesForCompetitionBefore(
                        competitionId,
                        before,
                        PageRequest.of(0, LEAGUE_BASELINE_LIMIT)
                );

        if (fixtures.isEmpty()) {
            return defaultLeagueBaseline();
        }

        int sample = fixtures.size();

        double homeGoals = 0;
        double awayGoals = 0;
        double homeWins = 0;
        double draws = 0;
        double awayWins = 0;
        double over25 = 0;
        double btts = 0;

        for (Fixture historicalFixture : fixtures) {
            int hg = safeInt(historicalFixture.getHomeGoals());
            int ag = safeInt(historicalFixture.getAwayGoals());

            homeGoals += hg;
            awayGoals += ag;

            if (hg > ag) {
                homeWins++;
            } else if (hg == ag) {
                draws++;
            } else {
                awayWins++;
            }

            if (hg + ag > 2) {
                over25++;
            }

            if (hg > 0 && ag > 0) {
                btts++;
            }
        }

        double avgHomeGoals = safeRate(homeGoals, sample);
        double avgAwayGoals = safeRate(awayGoals, sample);

        return new LeagueBaselineSnapshotDto(
                sample,
                avgHomeGoals,
                avgAwayGoals,
                avgHomeGoals + avgAwayGoals,
                safeRate(homeWins, sample),
                safeRate(draws, sample),
                safeRate(awayWins, sample),
                safeRate(over25, sample),
                safeRate(btts, sample)
        );
    }

    private LeagueBaselineSnapshotDto defaultLeagueBaseline() {
        return new LeagueBaselineSnapshotDto(
                0,
                1.45,
                1.15,
                2.60,
                0.45,
                0.27,
                0.28,
                0.50,
                0.50
        );
    }

    private double expectedGoals(
            TeamFormSnapshotDto attackTeam,
            TeamFormSnapshotDto defenceTeam,
            HeadToHeadSnapshotDto h2h,
            LeagueBaselineSnapshotDto leagueBaseline,
            boolean homeSide,
            TeamScheduleContextDto attackSchedule,
            TeamScheduleContextDto defenceSchedule,
            TeamStrengthSnapshotDto attackStrength,
            TeamStrengthSnapshotDto defenceStrength) {

        LeagueBaselineSnapshotDto baseline =
                leagueBaseline != null ? leagueBaseline : defaultLeagueBaseline();

        double sideBaselineGoals =
                homeSide
                        ? baseline.avgHomeGoals()
                        : baseline.avgAwayGoals();

        double attackGoalsFor =
                blendWithBaseline(
                        attackTeam.goalsForPerGame(),
                        sideBaselineGoals,
                        attackTeam.sampleSize(),
                        5.0
                );

        double defenceGoalsAgainst =
                blendWithBaseline(
                        defenceTeam.goalsAgainstPerGame(),
                        sideBaselineGoals,
                        defenceTeam.sampleSize(),
                        5.0
                );

        double homeTeamPpg =
                h2h != null ? h2h.homeTeamPointsPerGame() : 0.0;

        double awayTeamPpg =
                h2h != null ? h2h.awayTeamPointsPerGame() : 0.0;

        double h2hAdjustment = homeSide
                ? 0.05 * (homeTeamPpg - awayTeamPpg)
                : 0.05 * (awayTeamPpg - homeTeamPpg);

        double possessionAdjustment =
                attackTeam.avgPossession() > 0
                        ? 0.02 * (attackTeam.avgPossession() - 50.0)
                        : 0.0;

        double shotsAdjustment =
                attackTeam.avgShotsOnGoal() > 0
                        ? 0.06 * (attackTeam.avgShotsOnGoal() - 4.0)
                        : 0.0;

        double homeAdvantage =
                homeSide ? 0.10 : 0.0;

        double absencePenalty =
                0.08 * attackTeam.absenceImpactScore();
        
        double restAdjustment =
                0.06 * (
                        safeRestScore(attackSchedule)
                                - safeRestScore(defenceSchedule)
                );
        
        double strengthAdjustment =
                safeStrengthScore(attackStrength)
                        - safeStrengthScore(defenceStrength);

        double lambda =
                (0.55 * attackGoalsFor)
                        + (0.35 * defenceGoalsAgainst)
                        + homeAdvantage
                        + h2hAdjustment
                        + possessionAdjustment
                        + shotsAdjustment
                        + restAdjustment
                        + strengthAdjustment
                        - absencePenalty;

        return clamp(lambda, 0.20, 3.50);
    }
    
    private double safeRestScore(TeamScheduleContextDto scheduleContext) {
        return scheduleContext == null ? 0.0 : scheduleContext.restScore();
    }

    private double safeStrengthScore(TeamStrengthSnapshotDto strength) {
        return strength == null ? 0.0 : strength.strengthScore();
    }
    
    private double blendWithBaseline(
            double value,
            double baseline,
            int sampleSize,
            double baselineWeight) {

        if (sampleSize <= 0 || value <= 0) {
            return baseline;
        }

        return ((value * sampleSize) + (baseline * baselineWeight))
                / (sampleSize + baselineWeight);
    }

    private Map<Long, List<FixtureTeamStat>> loadStatsByFixture(
            List<Fixture> fixtures,
            String providerName,
            Long externalTeamId) {

        if (fixtures.isEmpty()) {
            return Map.of();
        }

        List<Long> fixtureIds = fixtures.stream()
                .map(Fixture::getId)
                .toList();

        List<Long> historicalTeamIds = fixtures.stream()
                .map(historicalFixture -> {
                    if (sameTeam(
                            historicalFixture.getHomeTeam(),
                            providerName,
                            externalTeamId)) {

                        return historicalFixture.getHomeTeam();
                    }

                    return historicalFixture.getAwayTeam();
                })
                .filter(Objects::nonNull)
                .map(Team::getId)
                .distinct()
                .toList();

        if (historicalTeamIds.isEmpty()) {
            return Map.of();
        }

        List<FixtureTeamStat> allStats =
                fixtureTeamStatRepository.findByFixtureIdInAndTeamIdIn(
                        fixtureIds,
                        historicalTeamIds
                );

        return allStats.stream()
                .collect(
                        Collectors.groupingBy(
                                stat -> stat.getFixture().getId()
                        )
                );
    }

    private Double getNumericStat(
            List<FixtureTeamStat> stats,
            String name) {

        return stats.stream()
                .filter(stat -> name.equalsIgnoreCase(stat.getStatName()))
                .map(FixtureTeamStat::getStatValueNumber)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private double safeRate(double numerator, double denominator) {
        return denominator == 0.0 ? 0.0 : numerator / denominator;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private boolean sameTeam(
            Team team,
            String providerName,
            Long externalTeamId) {

        return team != null
                && Objects.equals(team.getProviderName(), providerName)
                && Objects.equals(team.getExternalTeamId(), externalTeamId);
    }
}