package ro.betrio.backend.service.app;
import java.util.Objects;

import ro.betrio.backend.domain.entity.Team;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.betrio.backend.api.dto.app.FixtureH2HDto;
import ro.betrio.backend.domain.entity.Fixture;
import ro.betrio.backend.repository.FixtureRepository;

@Service
public class AppH2HService {

    private final FixtureRepository fixtureRepository;

    public AppH2HService(FixtureRepository fixtureRepository) {
        this.fixtureRepository = fixtureRepository;
    }

    @Transactional(readOnly = true)
    public FixtureH2HDto getFixtureH2H(Long fixtureId, int limit) {
        Fixture fixture = fixtureRepository.findDetailedById(fixtureId)
                .orElseThrow(() -> new IllegalStateException("Fixture not found: " + fixtureId));

        Team currentHomeTeam = fixture.getHomeTeam();
        Team currentAwayTeam = fixture.getAwayTeam();

        if (currentHomeTeam == null || currentAwayTeam == null) {
            throw new IllegalStateException(
                    "Fixture is missing home or away team."
            );
        }

        if (!Objects.equals(
                currentHomeTeam.getProviderName(),
                currentAwayTeam.getProviderName())) {

            throw new IllegalStateException(
                    "The two teams use different providers."
            );
        }

        Long homeTeamId = currentHomeTeam.getId();
        Long awayTeamId = currentAwayTeam.getId();

        List<Fixture> h2hFixtures =
                fixtureRepository.findRecentHeadToHead(
                        currentHomeTeam.getProviderName(),
                        currentHomeTeam.getExternalTeamId(),
                        currentAwayTeam.getExternalTeamId(),
                        fixture.getKickoffAt(),
                        PageRequest.of(0, limit)
                );

        FixtureH2HDto.Summary summary =
                buildSummary(
                        currentHomeTeam,
                        h2hFixtures
                );
        
        List<FixtureH2HDto.H2HMatchItem> items = h2hFixtures.stream()
                .map(this::toMatchItem)
                .toList();

        return new FixtureH2HDto(
                fixture.getId(),
                homeTeamId,
                fixture.getHomeTeam().getTeamName(),
                awayTeamId,
                fixture.getAwayTeam().getTeamName(),
                summary,
                items
        );
    }

    private FixtureH2HDto.Summary buildSummary(
            Team currentHomeTeam,
            List<Fixture> fixtures) {

        int homeTeamWins = 0;
        int draws = 0;
        int awayTeamWins = 0;
        int homeTeamGoals = 0;
        int awayTeamGoals = 0;

        for (Fixture fixture : fixtures) {

            boolean currentHomeWasHome = sameTeam(
                    currentHomeTeam,
                    fixture.getHomeTeam()
            );

            int currentHomeGoalsInThatMatch =
                    currentHomeWasHome
                            ? safeInt(fixture.getHomeGoals())
                            : safeInt(fixture.getAwayGoals());

            int currentAwayGoalsInThatMatch =
                    currentHomeWasHome
                            ? safeInt(fixture.getAwayGoals())
                            : safeInt(fixture.getHomeGoals());

            homeTeamGoals += currentHomeGoalsInThatMatch;
            awayTeamGoals += currentAwayGoalsInThatMatch;

            if (currentHomeGoalsInThatMatch
                    > currentAwayGoalsInThatMatch) {

                homeTeamWins++;

            } else if (currentHomeGoalsInThatMatch
                    < currentAwayGoalsInThatMatch) {

                awayTeamWins++;

            } else {
                draws++;
            }
        }

        return new FixtureH2HDto.Summary(
                fixtures.size(),
                homeTeamWins,
                draws,
                awayTeamWins,
                homeTeamGoals,
                awayTeamGoals
        );
    }

    private FixtureH2HDto.H2HMatchItem toMatchItem(Fixture fixture) {
        String resultCode = null;

        if (fixture.getHomeGoals() != null && fixture.getAwayGoals() != null) {
            if (fixture.getHomeGoals() > fixture.getAwayGoals()) {
                resultCode = "HOME";
            } else if (fixture.getHomeGoals() < fixture.getAwayGoals()) {
                resultCode = "AWAY";
            } else {
                resultCode = "DRAW";
            }
        }

        return new FixtureH2HDto.H2HMatchItem(
                fixture.getId(),
                fixture.getKickoffAt(),
                fixture.getLeagueRound(),
                fixture.getStatusShort(),
                fixture.getHomeTeam().getTeamName(),
                fixture.getAwayTeam().getTeamName(),
                fixture.getHomeGoals(),
                fixture.getAwayGoals(),
                resultCode
        );
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
}