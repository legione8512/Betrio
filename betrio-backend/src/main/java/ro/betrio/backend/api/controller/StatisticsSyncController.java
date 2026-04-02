package ro.betrio.backend.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import ro.betrio.backend.config.ApiFootballProperties;
import ro.betrio.backend.service.sync.FixtureStatisticsSyncService;

@RestController
@RequestMapping("/api/sync")
public class StatisticsSyncController {

    private final FixtureStatisticsSyncService fixtureStatisticsSyncService;
    private final ApiFootballProperties apiFootballProperties;

    public StatisticsSyncController(
            FixtureStatisticsSyncService fixtureStatisticsSyncService,
            ApiFootballProperties apiFootballProperties) {
        this.fixtureStatisticsSyncService = fixtureStatisticsSyncService;
        this.apiFootballProperties = apiFootballProperties;
    }

    @PostMapping("/fixture-stats/{fixtureId}")
    public ResponseEntity<String> syncFixtureStats(@PathVariable Long fixtureId) {
        fixtureStatisticsSyncService.syncFixtureStatisticsByFixtureId(fixtureId);
        return ResponseEntity.ok("Fixture statistics sync completed for fixture id " + fixtureId);
    }

    @PostMapping("/fixture-stats/current-season")
    public ResponseEntity<String> syncCurrentSeasonStats(@RequestParam(defaultValue = "20") int limit) {
        fixtureStatisticsSyncService.syncRecentCompletedStatistics(apiFootballProperties.getCurrentSeason(), limit);
        return ResponseEntity.ok("Recent completed fixture statistics sync completed.");
    }
}