package ro.betrio.backend.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ro.betrio.backend.api.dto.app.UpcomingPicksDto;
import ro.betrio.backend.service.app.AppUpcomingPicksService;

@RestController
@RequestMapping("/api/app/recommendations")
public class AppUpcomingPicksController {

    private final AppUpcomingPicksService appUpcomingPicksService;

    public AppUpcomingPicksController(AppUpcomingPicksService appUpcomingPicksService) {
        this.appUpcomingPicksService = appUpcomingPicksService;
    }

    @GetMapping("/upcoming")
    public UpcomingPicksDto getUpcomingPicks(
            @RequestParam(required = false) Long competitionId,
            @RequestParam(defaultValue = "10") int limit) {
        return appUpcomingPicksService.getUpcomingPicks(competitionId, limit);
    }
}