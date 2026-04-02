package ro.betrio.backend.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import ro.betrio.backend.config.ApiFootballProperties;
import ro.betrio.backend.service.sync.SquadAndAvailabilitySyncService;

@RestController
@RequestMapping("/api/sync")
public class AvailabilitySyncController {

    private final SquadAndAvailabilitySyncService squadAndAvailabilitySyncService;
    private final ApiFootballProperties apiFootballProperties;

    public AvailabilitySyncController(
            SquadAndAvailabilitySyncService squadAndAvailabilitySyncService,
            ApiFootballProperties apiFootballProperties) {
        this.squadAndAvailabilitySyncService = squadAndAvailabilitySyncService;
        this.apiFootballProperties = apiFootballProperties;
    }

    @PostMapping("/squads/current")
    public ResponseEntity<String> syncCurrentSquads() {
        squadAndAvailabilitySyncService.syncCurrentSquads(apiFootballProperties.getCurrentSeason());
        return ResponseEntity.ok("Current squads sync completed.");
    }

    @PostMapping("/fixture-context/{fixtureId}")
    public ResponseEntity<String> syncFixtureContext(@PathVariable Long fixtureId) {
        squadAndAvailabilitySyncService.syncFixtureContextByFixtureId(fixtureId);
        return ResponseEntity.ok("Fixture context sync completed for fixture id " + fixtureId);
    }

    @PostMapping("/fixture-context/current-window")
    public ResponseEntity<String> syncCurrentWindow(
            @RequestParam(defaultValue = "2") int daysBack,
            @RequestParam(defaultValue = "7") int daysAhead) {
        squadAndAvailabilitySyncService.syncCurrentSeasonWindow(
                apiFootballProperties.getCurrentSeason(),
                daysBack,
                daysAhead
        );
        return ResponseEntity.ok("Current season fixture context sync completed.");
    }
}