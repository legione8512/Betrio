package ro.betrio.backend.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ro.betrio.backend.api.dto.app.FixtureMarketComparisonDto;
import ro.betrio.backend.service.app.MarketComparisonService;

@RestController
@RequestMapping("/api/app")
public class MarketComparisonController {

    private final MarketComparisonService marketComparisonService;

    public MarketComparisonController(MarketComparisonService marketComparisonService) {
        this.marketComparisonService = marketComparisonService;
    }

    @GetMapping("/fixture/{fixtureId}/market-comparison")
    public FixtureMarketComparisonDto getMarketComparison(@PathVariable Long fixtureId) {
        return marketComparisonService.getMarketComparison(fixtureId);
    }
}