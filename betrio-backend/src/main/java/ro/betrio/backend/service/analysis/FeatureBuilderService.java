package ro.betrio.backend.service.analysis;

import java.time.OffsetDateTime;

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

        TeamFormSnapshotDto homeForm = buildTeamForm(fixture.getHomeTeam().getId(), fixtureId, before);
        TeamFormSnapshotDto awayForm = buildTeamForm(fixture.getAwayTeam().getId(), fixtureId, before);
        HeadToHeadSnapshotDto h2h = buildHeadToHead(fixture.getHomeTeam().getId(), fixture.getAwayTeam().getId(), before);

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

    private TeamFormSnapshotDto buildTeamForm(Long teamId, Long fixtureId, OffsetDateTime before) {
        List<Fixture> fixtures = fixtureRepository.findRecentCompletedFixturesForTeam(teamId, before, PageRequest.of(0, 5));

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
            boolean isHome = f.getHomeTeam() != null && teamId.equals(f.getHomeTeam().getId());

            int gf = safeInt(isHome ? f.getHomeGoals() : f.getAwayGoals());
            int ga = safeInt(isHome ? f.getAwayGoals() : f.getHomeGoals());

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
            if ((gf + ga) > 2) {
                over25Matches++;
            }
            if (ga == 0) {
                cleanSheets++;
            }

            List<FixtureTeamStat> stats = fixtureTeamStatRepository.findByFixtureIdAndTeamId(f.getId(), teamId);
            Double possession = getNumericStat(stats, "Ball Possession");
            if (possession != null) {
                possessionSum += possession;
                possessionCount++;
            }

            Double shotsOnGoal = getNumericStat(stats, "Shots on Goal");
            if (shotsOnGoal != null) {
                shotsOnGoalSum += shotsOnGoal;
                shotsOnGoalCount++;
            }
        }

        long absences = fixtureAbsenceRepository.countByFixtureIdAndTeamId(fixtureId, teamId);

        return new TeamFormSnapshotDto(
                teamId,
                fixtures.isEmpty()
                        ? "Unknown"
                        : (fixtures.getFirst().getHomeTeam().getId().equals(teamId)
                                ? fixtures.getFirst().getHomeTeam().getTeamName()
                                : fixtures.getFirst().getAwayTeam().getTeamName()),
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

    private HeadToHeadSnapshotDto buildHeadToHead(Long homeTeamId, Long awayTeamId, OffsetDateTime before) {
        List<Fixture> fixtures = fixtureRepository.findRecentHeadToHead(
                homeTeamId,
                awayTeamId,
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
            int homeGoals = safeInt(f.getHomeGoals());
            int awayGoals = safeInt(f.getAwayGoals());

            boolean requestedHomeWasActualHome = homeTeamId.equals(f.getHomeTeam().getId());

            int requestedHomeGoals = requestedHomeWasActualHome ? homeGoals : awayGoals;
            int requestedAwayGoals = requestedHomeWasActualHome ? awayGoals : homeGoals;

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
}