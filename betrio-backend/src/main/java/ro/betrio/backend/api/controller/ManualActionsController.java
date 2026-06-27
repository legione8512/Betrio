package ro.betrio.backend.api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import ro.betrio.backend.api.dto.action.ManualActionHistoryDto;
import ro.betrio.backend.api.dto.action.ManualActionResultDto;
import ro.betrio.backend.service.action.ManualOperationsService;

@RestController
@RequestMapping("/api/actions")
public class ManualActionsController {

    private final ManualOperationsService manualOperationsService;

    public ManualActionsController(ManualOperationsService manualOperationsService) {
        this.manualOperationsService = manualOperationsService;
    }

    @PostMapping("/base-refresh")
    public ManualActionResultDto baseRefresh() {
        return manualOperationsService.baseRefresh();
    }

    @PostMapping("/fixture/{fixtureId}/smart-update")
    public ManualActionResultDto smartUpdate(@PathVariable Long fixtureId) {
        return manualOperationsService.smartUpdate(fixtureId);
    }

    @GetMapping("/history")
    public List<ManualActionHistoryDto> getHistory(
            @RequestParam(defaultValue = "20") int limit) {
        return manualOperationsService.getHistory(limit);
    }

    @GetMapping("/fixture/{fixtureId}/history")
    public List<ManualActionHistoryDto> getFixtureHistory(
            @PathVariable Long fixtureId,
            @RequestParam(defaultValue = "20") int limit) {
        return manualOperationsService.getFixtureHistory(fixtureId, limit);
    }
}