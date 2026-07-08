package ro.betrio.backend.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ro.betrio.backend.api.dto.app.FixtureMatchCenterDto;
import ro.betrio.backend.service.app.AppMatchCenterService;

@RestController
@RequestMapping("/api/app")
public class AppMatchCenterController {

    private final AppMatchCenterService appMatchCenterService;

    public AppMatchCenterController(AppMatchCenterService appMatchCenterService) {
        this.appMatchCenterService = appMatchCenterService;
    }

    @GetMapping("/fixture/{fixtureId}/match-center")
    public FixtureMatchCenterDto getMatchCenter(
            @PathVariable Long fixtureId,
            @RequestParam(defaultValue = "5") int h2hLimit,
            @RequestParam(defaultValue = "5") int formLimit) {
        return appMatchCenterService.getMatchCenter(fixtureId, h2hLimit, formLimit);
    }
}