package ro.betrio.backend.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ro.betrio.backend.api.dto.app.LeagueStandingsDto;
import ro.betrio.backend.service.app.AppStandingsService;

@RestController
@RequestMapping("/api/app/standings")
public class AppStandingsController {

    private final AppStandingsService appStandingsService;

    public AppStandingsController(AppStandingsService appStandingsService) {
        this.appStandingsService = appStandingsService;
    }

    @GetMapping("/current")
    public LeagueStandingsDto getCurrentStandings(
            @RequestParam(required = false) Long competitionId) {
        return appStandingsService.getCurrentStandings(competitionId);
    }

    @GetMapping("/season/{seasonId}")
    public LeagueStandingsDto getStandingsForSeason(@PathVariable Long seasonId) {
        return appStandingsService.getStandingsForSeason(seasonId);
    }
}