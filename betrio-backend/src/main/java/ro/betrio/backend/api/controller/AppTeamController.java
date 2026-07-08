package ro.betrio.backend.api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ro.betrio.backend.api.dto.app.TeamFormDto;
import ro.betrio.backend.api.dto.app.TeamOverviewDto;
import ro.betrio.backend.service.app.AppTeamService;

@RestController
@RequestMapping("/api/app/team")
public class AppTeamController {

    private final AppTeamService appTeamService;

    public AppTeamController(AppTeamService appTeamService) {
        this.appTeamService = appTeamService;
    }

    @GetMapping("/{teamId}/overview")
    public TeamOverviewDto getTeamOverview(@PathVariable Long teamId) {
        return appTeamService.getTeamOverview(teamId);
    }

    @GetMapping("/{teamId}/recent")
    public List<TeamOverviewDto.MatchItem> getRecentMatches(
            @PathVariable Long teamId,
            @RequestParam(defaultValue = "5") int limit) {
        return appTeamService.getRecentMatches(teamId, limit);
    }

    @GetMapping("/{teamId}/upcoming")
    public List<TeamOverviewDto.MatchItem> getUpcomingMatches(
            @PathVariable Long teamId,
            @RequestParam(defaultValue = "5") int limit) {
        return appTeamService.getUpcomingMatches(teamId, limit);
    }

    @GetMapping("/{teamId}/form")
    public TeamFormDto getTeamForm(
            @PathVariable Long teamId,
            @RequestParam(defaultValue = "10") int limit) {
        return appTeamService.getTeamForm(teamId, limit);
    }
}