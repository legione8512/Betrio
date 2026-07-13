package ro.betrio.backend.service.sync;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.betrio.backend.client.apifootball.ApiFootballClient;
import ro.betrio.backend.client.apifootball.dto.ApiFootballFixtureItem;
import ro.betrio.backend.client.apifootball.dto.ApiFootballFixturesResponse;
import ro.betrio.backend.domain.entity.Fixture;
import ro.betrio.backend.repository.FixtureRepository;

@Service
public class FixtureResultSyncService {

    private final ApiFootballClient apiFootballClient;
    private final FixtureRepository fixtureRepository;

    public FixtureResultSyncService(
            ApiFootballClient apiFootballClient,
            FixtureRepository fixtureRepository) {
        this.apiFootballClient = apiFootballClient;
        this.fixtureRepository = fixtureRepository;
    }

    @Transactional
    public Fixture refreshFixtureById(Long fixtureId) {
        Fixture fixture = fixtureRepository.findById(fixtureId)
                .orElseThrow(() ->
                        new IllegalStateException("Fixture not found: " + fixtureId));

        ApiFootballFixturesResponse response =
                apiFootballClient.getFixtureById(fixture.getExternalFixtureId());

        if (response == null
                || response.getResponse() == null
                || response.getResponse().isEmpty()) {
            throw new IllegalStateException(
                    "API-Football returned no data for external fixture id: "
                            + fixture.getExternalFixtureId());
        }

        ApiFootballFixtureItem item = response.getResponse().getFirst();

        if (item.getFixture() == null) {
            throw new IllegalStateException(
                    "API-Football response contains no fixture information for: "
                            + fixture.getExternalFixtureId());
        }

        fixture.setRefereeName(item.getFixture().getReferee());
        fixture.setTimezoneName(item.getFixture().getTimezone());

        if (item.getFixture().getDate() != null
                && !item.getFixture().getDate().isBlank()) {
            fixture.setKickoffAt(
                    OffsetDateTime.parse(item.getFixture().getDate()));
        }

        if (item.getFixture().getStatus() != null) {
            fixture.setStatusLong(
                    item.getFixture().getStatus().getLongValue());

            fixture.setStatusShort(
                    item.getFixture().getStatus().getShortValue());

            fixture.setElapsedMinutes(
                    item.getFixture().getStatus().getElapsed());
        }

        if (item.getFixture().getVenue() != null) {
            fixture.setVenueName(
                    item.getFixture().getVenue().getName());

            fixture.setVenueCity(
                    item.getFixture().getVenue().getCity());
        }

        if (item.getLeague() != null) {
            fixture.setLeagueRound(item.getLeague().getRound());
        }

        if (item.getGoals() != null) {
            fixture.setHomeGoals(item.getGoals().getHome());
            fixture.setAwayGoals(item.getGoals().getAway());
        }

        if (item.getScore() != null) {
            if (item.getScore().getHalftime() != null) {
                fixture.setHalftimeHomeGoals(
                        item.getScore().getHalftime().getHome());

                fixture.setHalftimeAwayGoals(
                        item.getScore().getHalftime().getAway());
            }

            if (item.getScore().getFulltime() != null) {
                fixture.setFulltimeHomeGoals(
                        item.getScore().getFulltime().getHome());

                fixture.setFulltimeAwayGoals(
                        item.getScore().getFulltime().getAway());
            }

            if (item.getScore().getExtratime() != null) {
                fixture.setExtratimeHomeGoals(
                        item.getScore().getExtratime().getHome());

                fixture.setExtratimeAwayGoals(
                        item.getScore().getExtratime().getAway());
            }

            if (item.getScore().getPenalty() != null) {
                fixture.setPenaltyHomeGoals(
                        item.getScore().getPenalty().getHome());

                fixture.setPenaltyAwayGoals(
                        item.getScore().getPenalty().getAway());
            }
        }

        return fixtureRepository.save(fixture);
    }
}