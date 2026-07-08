package ro.betrio.backend.api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ro.betrio.backend.api.dto.app.CompetitionOptionDto;
import ro.betrio.backend.service.app.AppCompetitionService;

@RestController
@RequestMapping("/api/app/meta")
public class AppCompetitionController {

    private final AppCompetitionService appCompetitionService;

    public AppCompetitionController(AppCompetitionService appCompetitionService) {
        this.appCompetitionService = appCompetitionService;
    }

    @GetMapping("/competitions")
    public List<CompetitionOptionDto> getCompetitions() {
        return appCompetitionService.getCompetitions();
    }
}