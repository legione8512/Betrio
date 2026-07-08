package ro.betrio.backend.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ro.betrio.backend.api.dto.app.FixtureRecommendationDto;
import ro.betrio.backend.service.app.AppRecommendationService;

@RestController
@RequestMapping("/api/app")
public class AppRecommendationController {

    private final AppRecommendationService appRecommendationService;

    public AppRecommendationController(AppRecommendationService appRecommendationService) {
        this.appRecommendationService = appRecommendationService;
    }

    @GetMapping("/fixture/{fixtureId}/recommendation")
    public FixtureRecommendationDto getRecommendation(@PathVariable Long fixtureId) {
        return appRecommendationService.getRecommendation(fixtureId);
    }
}