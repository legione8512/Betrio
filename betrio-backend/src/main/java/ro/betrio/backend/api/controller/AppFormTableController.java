package ro.betrio.backend.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ro.betrio.backend.api.dto.app.LeagueFormTableDto;
import ro.betrio.backend.service.app.AppFormTableService;

@RestController
@RequestMapping("/api/app/standings/form")
public class AppFormTableController {

    private final AppFormTableService appFormTableService;

    public AppFormTableController(AppFormTableService appFormTableService) {
        this.appFormTableService = appFormTableService;
    }

    @GetMapping("/current")
    public LeagueFormTableDto getCurrentFormTable(
            @RequestParam(required = false) Long competitionId,
            @RequestParam(defaultValue = "5") int limit) {
        return appFormTableService.getCurrentFormTable(competitionId, limit);
    }

    @GetMapping("/season/{seasonId}")
    public LeagueFormTableDto getSeasonFormTable(
            @PathVariable Long seasonId,
            @RequestParam(defaultValue = "5") int limit) {
        return appFormTableService.getFormTableForSeason(seasonId, limit);
    }
}