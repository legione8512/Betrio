package ro.betrio.backend.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ro.betrio.backend.api.dto.app.DashboardHomeDto;
import ro.betrio.backend.service.app.AppDashboardService;

@RestController
@RequestMapping("/api/app/dashboard")
public class AppDashboardHomeController {

    private final AppDashboardService appDashboardService;

    public AppDashboardHomeController(AppDashboardService appDashboardService) {
        this.appDashboardService = appDashboardService;
    }

    @GetMapping("/home")
    public DashboardHomeDto getDashboardHome(
            @RequestParam(defaultValue = "5") int picksLimit,
            @RequestParam(defaultValue = "5") int formLimit,
            @RequestParam(defaultValue = "5") int evaluationsLimit,
            @RequestParam(required = false) Long competitionId) {
        return appDashboardService.getDashboardHome(
                picksLimit,
                formLimit,
                evaluationsLimit,
                competitionId
        );
    }
}