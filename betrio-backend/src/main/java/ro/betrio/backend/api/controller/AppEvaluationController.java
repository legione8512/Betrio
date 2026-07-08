package ro.betrio.backend.api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ro.betrio.backend.api.dto.app.RecentEvaluationDto;
import ro.betrio.backend.service.app.AppEvaluationService;

@RestController
@RequestMapping("/api/app/evaluations")
public class AppEvaluationController {

    private final AppEvaluationService appEvaluationService;

    public AppEvaluationController(AppEvaluationService appEvaluationService) {
        this.appEvaluationService = appEvaluationService;
    }

    @GetMapping("/recent")
    public List<RecentEvaluationDto> getRecentEvaluations(
            @RequestParam(defaultValue = "5") int limit) {
        return appEvaluationService.getRecentEvaluations(limit);
    }
}