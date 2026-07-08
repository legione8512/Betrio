package ro.betrio.backend.api.controller;

import org.springframework.http.ResponseEntity;


import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ro.betrio.backend.api.dto.sync.LeagueBootstrapRequestDto;
import ro.betrio.backend.service.sync.BootstrapSyncService;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final BootstrapSyncService bootstrapSyncService;

    public SyncController(BootstrapSyncService bootstrapSyncService) {
        this.bootstrapSyncService = bootstrapSyncService;
    }

    @PostMapping("/bootstrap")
    public ResponseEntity<String> bootstrap() {
        bootstrapSyncService.runBootstrapSync();
        return ResponseEntity.ok("Bootstrap sync completed.");
    }
    @PostMapping("/bootstrap/league")
    public String bootstrapLeague(@RequestBody LeagueBootstrapRequestDto request) {
        bootstrapSyncService.runBootstrapSync(
                request.leagueId(),
                request.currentSeason(),
                request.previousSeason()
        );
        return "Bootstrap completed for leagueId=" + request.leagueId();
    }
}