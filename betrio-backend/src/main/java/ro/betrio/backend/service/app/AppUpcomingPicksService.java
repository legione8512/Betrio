package ro.betrio.backend.service.app;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import ro.betrio.backend.api.dto.app.FixtureRecommendationDto;
import ro.betrio.backend.api.dto.app.UpcomingPicksDto;
import ro.betrio.backend.domain.entity.Fixture;
import ro.betrio.backend.repository.FixtureRepository;

@Service
public class AppUpcomingPicksService {

    private final FixtureRepository fixtureRepository;
    private final AppRecommendationService appRecommendationService;

    public AppUpcomingPicksService(
            FixtureRepository fixtureRepository,
            AppRecommendationService appRecommendationService) {
        this.fixtureRepository = fixtureRepository;
        this.appRecommendationService = appRecommendationService;
    }

    public UpcomingPicksDto getUpcomingPicks(Long competitionId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));

        List<Fixture> upcomingFixtures = competitionId == null
                ? fixtureRepository.findUpcomingWithTeams(PageRequest.of(0, safeLimit))
                : fixtureRepository.findUpcomingWithTeamsByCompetition(competitionId, PageRequest.of(0, safeLimit));

        List<UpcomingPicksDto.PickItem> picks = new ArrayList<>();

        for (Fixture fixture : upcomingFixtures) {
            try {
                picks.add(toPickItem(fixture));
            } catch (Exception ex) {
                System.out.println("Upcoming pick failed for fixtureId=" + fixture.getId()
                        + " -> " + ex.getClass().getName() + ": " + ex.getMessage());
                ex.printStackTrace();

                picks.add(toUnavailablePickItem(
                        fixture,
                        "Recommendation failed for fixtureId=" + fixture.getId()
                ));
            }
        }

        int availableCount = (int) picks.stream()
                .filter(UpcomingPicksDto.PickItem::available)
                .count();

        return new UpcomingPicksDto(
                OffsetDateTime.now(),
                safeLimit,
                picks.size(),
                availableCount,
                picks
        );
    }

    private UpcomingPicksDto.PickItem toPickItem(Fixture fixture) {
        FixtureRecommendationDto recommendation =
                appRecommendationService.getRecommendation(fixture.getId());

        String summary = recommendation.summary() != null
                ? recommendation.summary()
                : recommendation.reason();
        
        return new UpcomingPicksDto.PickItem(
                fixture.getId(),
                fixture.getKickoffAt(),
                fixture.getHomeTeam() != null ? fixture.getHomeTeam().getTeamName() : null,
                fixture.getAwayTeam() != null ? fixture.getAwayTeam().getTeamName() : null,
                recommendation.available(),
                recommendation.recommendationType(),
                recommendation.recommendedResultCode(),
                recommendation.confidenceScore(),
                recommendation.confidenceTier(),
                recommendation.riskTag(),
                recommendation.overUnderLean(),
                recommendation.bttsLean(),
                summary
        );
    }

    private UpcomingPicksDto.PickItem toUnavailablePickItem(Fixture fixture, String summary) {
        return new UpcomingPicksDto.PickItem(
                fixture.getId(),
                fixture.getKickoffAt(),
                fixture.getHomeTeam() != null ? fixture.getHomeTeam().getTeamName() : null,
                fixture.getAwayTeam() != null ? fixture.getAwayTeam().getTeamName() : null,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                summary
        );
    }
}