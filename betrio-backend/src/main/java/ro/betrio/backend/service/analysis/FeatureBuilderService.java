package ro.betrio.backend.service.analysis;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import ro.betrio.backend.api.dto.HeadToHeadSnapshotDto;
import ro.betrio.backend.api.dto.MatchFeatureSnapshotDto;
import ro.betrio.backend.api.dto.TeamFormSnapshotDto;
import ro.betrio.backend.domain.entity.Fixture;
import ro.betrio.backend.domain.entity.FixtureTeamStat;
import ro.betrio.backend.repository.FixtureAbsenceRepository;
import ro.betrio.backend.repository.FixtureRepository;
import ro.betrio.backend.repository.FixtureTeamStatRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;
import ro.betrio.backend.domain.entity.Team;
@Service
public class FeatureBuilderService {

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
        Fixture fixture = fixtureRepository.findById(fixtureId)
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

        HeadToHeadSnapshotDto h2h =
                buildHeadToHead(homeTeam, awayTeam, before);

        double expectedHomeGoals = expectedGoals(homeForm, awayForm, h2h, true);
        double expectedAwayGoals = expectedGoals(awayForm, homeForm, h2h, false);

        return new MatchFeatureSnapshotDto(
                fixture.getId(),
                fixture.getHomeTeam().getTeamName(),
                fixture.getAwayTeam().getTeamName(),
                fixture.getKickoffAt().toString(),
                homeForm,
                awayForm,
                h2h,
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
                        PageRequest.of(0, 5)
                );
        Map<Long, List<FixtureTeamStat>> statsByFixtureId =
                loadStatsByFixture(
                        fixtures,
                        targetTeam.getProviderName(),
                        targetTeam.getExternalTeamId()
                );

        int wins = 0;
        int draws = 0;
        int losses = 0;
        int points = 0;
        int sample = fixtures.size();

        double goalsFor = 0;
        double goalsAgainst = 0;
        double bttsMatches = 0;
        double over25Matches = 0;
        double cleanSheets = 0;

        double possessionSum = 0;
        int possessionCount = 0;

        double shotsOnGoalSum = 0;
        int shotsOnGoalCount = 0;

        for (Fixture f : fixtures) {

            boolean isHome = sameTeam(
                    f.getHomeTeam(),
                    providerName,
                    externalTeamId
            );

            int gf = safeInt(
                    isHome
                            ? f.getHomeGoals()
                            : f.getAwayGoals()
            );

            int ga = safeInt(
                    isHome
                            ? f.getAwayGoals()
                            : f.getHomeGoals()
            );

            goalsFor += gf;
            goalsAgainst += ga;

            if (gf > ga) {
                wins++;
                points += 3;
            } else if (gf == ga) {
                draws++;
                points += 1;
            } else {
                losses++;
            }

            if (gf > 0 && ga > 0) {
                bttsMatches++;
            }

            if (gf + ga > 2) {
                over25Matches++;
            }

            if (ga == 0) {
                cleanSheets++;
            }

            /*
             * Aici folosim ID-ul echipei din sezonul
             * în care s-a jucat meciul istoric.
             */
            List<FixtureTeamStat> stats =
                    statsByFixtureId.getOrDefault(
                            f.getId(),
                            List.of()
                    );

            Double possession =
                    getNumericStat(stats, "Ball Possession");

            if (possession != null) {
                possessionSum += possession;
                possessionCount++;
            }

            Double shotsOnGoal =
                    getNumericStat(stats, "Shots on Goal");

            if (shotsOnGoal != null) {
                shotsOnGoalSum += shotsOnGoal;
                shotsOnGoalCount++;
            }
        }

        /*
         * Aici folosim ID-ul echipei din sezonul actual,
         * deoarece absențele aparțin fixture-ului analizat.
         */
        long absences =
                fixtureAbsenceRepository
                        .countByFixtureIdAndTeamId(
                                fixtureId,
                                targetTeam.getId()
                        );

        return new TeamFormSnapshotDto(
                targetTeam.getId(),
                targetTeam.getTeamName(),
                sample,
                wins,
                draws,
                losses,
                points,
                safeRate(points, sample),
                safeRate(goalsFor, sample),
                safeRate(goalsAgainst, sample),
                safeRate(bttsMatches, sample),
                safeRate(over25Matches, sample),
                safeRate(cleanSheets, sample),
                safeRate(possessionSum, possessionCount),
                safeRate(shotsOnGoalSum, shotsOnGoalCount),
                absences
        );
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
                        PageRequest.of(0, 5)
                );

        int homeWins = 0;
        int draws = 0;
        int awayWins = 0;
        int sample = fixtures.size();

        double homePoints = 0;
        double awayPoints = 0;

        for (Fixture f : fixtures) {

            int actualHomeGoals =
                    safeInt(f.getHomeGoals());

            int actualAwayGoals =
                    safeInt(f.getAwayGoals());

            /*
             * Verificăm dacă echipa care este gazdă
             * în meciul analizat a fost gazdă și în
             * meciul istoric.
             */
            boolean requestedHomeWasActualHome =
                    sameTeam(
                            f.getHomeTeam(),
                            providerName,
                            homeExternalTeamId
                    );

            int requestedHomeGoals;

            int requestedAwayGoals;

            if (requestedHomeWasActualHome) {
                requestedHomeGoals = actualHomeGoals;
                requestedAwayGoals = actualAwayGoals;
            } else {
                /*
                 * În meciul istoric echipele au jucat
                 * cu pozițiile inversate.
                 */
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

    private double expectedGoals(
            TeamFormSnapshotDto attackTeam,
            TeamFormSnapshotDto defenceTeam,
            HeadToHeadSnapshotDto h2h,
            boolean homeSide) {

        double homeAdvantage = homeSide ? 0.18 : 0.00;
        double h2hAdjustment = homeSide
                ? 0.05 * (h2h.homeTeamPointsPerGame() - h2h.awayTeamPointsPerGame())
                : 0.05 * (h2h.awayTeamPointsPerGame() - h2h.homeTeamPointsPerGame());

        double absencePenalty = 0.08 * attackTeam.knownAbsencesForFixture();

        double lambda = (0.55 * attackTeam.goalsForPerGame())
                + (0.35 * defenceTeam.goalsAgainstPerGame())
                + (0.02 * (attackTeam.avgPossession() - 50.0))
                + (0.10 * attackTeam.avgShotsOnGoal())
                + homeAdvantage
                + h2hAdjustment
                - absencePenalty;

        return clamp(lambda, 0.20, 3.50);
    }
    
    private Map<Long, List<FixtureTeamStat>>
    loadStatsByFixture(
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
        .map(fixture -> {

            if (sameTeam(
                    fixture.getHomeTeam(),
                    providerName,
                    externalTeamId)) {

                return fixture.getHomeTeam();
            }

            return fixture.getAwayTeam();
        })
        .filter(Objects::nonNull)
        .map(Team::getId)
        .distinct()
        .toList();

if (historicalTeamIds.isEmpty()) {
    return Map.of();
}

List<FixtureTeamStat> allStats =
        fixtureTeamStatRepository
                .findByFixtureIdInAndTeamIdIn(
                        fixtureIds,
                        historicalTeamIds
                );

return allStats.stream()
        .collect(
                Collectors.groupingBy(
                        stat -> stat
                                .getFixture()
                                .getId()
                )
        );
}

    private Double getNumericStat(List<FixtureTeamStat> stats, String name) {
        return stats.stream()
                .filter(s -> name.equalsIgnoreCase(s.getStatName()))
                .map(FixtureTeamStat::getStatValueNumber)
                .filter(v -> v != null)
                .findFirst()
                .orElse(null);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private double safeRate(double numerator, int denominator) {
        return denominator == 0 ? 0.0 : numerator / denominator;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
    private boolean sameTeam(
            Team team,
            String providerName,
            Long externalTeamId) {

        return team != null
                && Objects.equals(
                        team.getProviderName(),
                        providerName
                )
                && Objects.equals(
                        team.getExternalTeamId(),
                        externalTeamId
                );
    }
}