package ro.betrio.backend.api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ro.betrio.backend.api.dto.meta.AppDashboardSummaryDto;
import ro.betrio.backend.api.dto.meta.FilterValueDto;
import ro.betrio.backend.api.dto.meta.TeamOptionDto;
import ro.betrio.backend.service.app.AppMetadataService;

@RestController
@RequestMapping("/api/app")
public class AppMetadataController {

    private final AppMetadataService appMetadataService;

    public AppMetadataController(AppMetadataService appMetadataService) {
        this.appMetadataService = appMetadataService;
    }

    @GetMapping("/meta/statuses")
    public List<FilterValueDto> getStatuses() {
        return appMetadataService.getStatuses();
    }

    @GetMapping("/meta/teams")
    public List<TeamOptionDto> getTeams(
            @RequestParam(required = false) Long competitionId) {
        return appMetadataService.getTeams(competitionId);
    }

    @GetMapping("/meta/rounds")
    public List<FilterValueDto> getRounds(
            @RequestParam(required = false) Long competitionId) {
        return appMetadataService.getRounds(competitionId);
    }

    @GetMapping("/dashboard/summary")
    public AppDashboardSummaryDto getDashboardSummary() {
        return appMetadataService.getDashboardSummary();
    }
}