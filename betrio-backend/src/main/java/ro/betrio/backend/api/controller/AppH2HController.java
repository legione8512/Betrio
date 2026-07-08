package ro.betrio.backend.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ro.betrio.backend.api.dto.app.FixtureH2HDto;
import ro.betrio.backend.service.app.AppH2HService;

@RestController
@RequestMapping("/api/app")
public class AppH2HController {

    private final AppH2HService appH2HService;

    public AppH2HController(AppH2HService appH2HService) {
        this.appH2HService = appH2HService;
    }

    @GetMapping("/fixture/{fixtureId}/h2h")
    public FixtureH2HDto getFixtureH2H(
            @PathVariable Long fixtureId,
            @RequestParam(defaultValue = "5") int limit) {
        return appH2HService.getFixtureH2H(fixtureId, limit);
    }
}