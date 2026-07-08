package ro.betrio.backend.api.controller;

import ro.betrio.backend.api.dto.app.PagedResponseDto;

import java.util.List;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ro.betrio.backend.api.dto.app.FixtureListItemDto;
import ro.betrio.backend.api.dto.app.FixtureOverviewDto;
import ro.betrio.backend.service.app.AppFixtureService;

@RestController
@RequestMapping("/api/app")
public class AppFixtureController {

    private final AppFixtureService appFixtureService;

    public AppFixtureController(AppFixtureService appFixtureService) {
        this.appFixtureService = appFixtureService;
    }

    @GetMapping("/fixtures/upcoming")
    public List<FixtureListItemDto> getUpcomingFixtures(
            @RequestParam(defaultValue = "20") int limit) {
        return appFixtureService.getUpcomingFixtures(limit);
    }

    @GetMapping("/fixtures/recent")
    public List<FixtureListItemDto> getRecentFixtures(
            @RequestParam(defaultValue = "20") int limit) {
        return appFixtureService.getRecentFixtures(limit);
    }

    @GetMapping("/fixture/{fixtureId}/overview")
    public FixtureOverviewDto getFixtureOverview(@PathVariable Long fixtureId) {
        return appFixtureService.getFixtureOverview(fixtureId);
    }
    @GetMapping("/fixtures/search")
    public List<FixtureListItemDto> searchFixtures(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime to,
            @RequestParam(defaultValue = "20") int limit) {
        return appFixtureService.searchFixtures(query, status, from, to, limit);
    }
    @GetMapping("/fixtures/page")
    public PagedResponseDto<FixtureListItemDto> getFixturesPage(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) Long competitionId,
            @RequestParam(required = false) String round,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "kickoffAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return appFixtureService.getFixturesPage(
                query, status, teamId, competitionId, round, from, to, page, size, sortBy, sortDir
        );
    }
}