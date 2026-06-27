package ro.betrio.backend.api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ro.betrio.backend.api.dto.report.FixtureEvaluationReportDto;
import ro.betrio.backend.api.dto.report.ModelDashboardDto;
import ro.betrio.backend.api.dto.report.RecentEvaluationDto;
import ro.betrio.backend.api.dto.report.TeamPerformanceReportDto;
import ro.betrio.backend.service.report.ReportingService;

@RestController
@RequestMapping("/api/reports")
public class ReportingController {

    private final ReportingService reportingService;

    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/dashboard")
    public ModelDashboardDto getDashboard() {
        return reportingService.getDashboard();
    }

    @GetMapping("/recent")
    public List<RecentEvaluationDto> getRecent(
            @RequestParam(defaultValue = "10") int limit) {
        return reportingService.getRecentEvaluations(limit);
    }

    @GetMapping("/fixture/{fixtureId}")
    public FixtureEvaluationReportDto getFixtureReport(@PathVariable Long fixtureId) {
        return reportingService.getFixtureReport(fixtureId);
    }

    @GetMapping("/team/{teamId}")
    public TeamPerformanceReportDto getTeamReport(@PathVariable Long teamId) {
        return reportingService.getTeamReport(teamId);
    }
}