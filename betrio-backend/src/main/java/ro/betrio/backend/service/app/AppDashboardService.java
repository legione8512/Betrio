package ro.betrio.backend.service.app;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.betrio.backend.api.dto.meta.AppDashboardSummaryDto;
import ro.betrio.backend.api.dto.app.DashboardHomeDto;
import ro.betrio.backend.api.dto.app.LeagueFormTableDto;
import ro.betrio.backend.api.dto.app.LeagueStandingsDto;
import ro.betrio.backend.api.dto.app.RecentEvaluationDto;
import ro.betrio.backend.api.dto.app.UpcomingPicksDto;

@Service
public class AppDashboardService {

    private final AppMetadataService appMetadataService;
    private final AppUpcomingPicksService appUpcomingPicksService;
    private final AppStandingsService appStandingsService;
    private final AppFormTableService appFormTableService;
    private final AppEvaluationService appEvaluationService;

    public AppDashboardService(
            AppMetadataService appMetadataService,
            AppUpcomingPicksService appUpcomingPicksService,
            AppStandingsService appStandingsService,
            AppFormTableService appFormTableService,
            AppEvaluationService appEvaluationService) {

        this.appMetadataService = appMetadataService;
        this.appUpcomingPicksService =
                appUpcomingPicksService;
        this.appStandingsService = appStandingsService;
        this.appFormTableService = appFormTableService;
        this.appEvaluationService = appEvaluationService;
    }

    @Transactional(readOnly = true)
    public DashboardHomeDto getDashboardHome(
            int picksLimit,
            int formLimit,
            int evaluationsLimit,
            Long competitionId) {

        int safePicksLimit =
                Math.max(1, Math.min(picksLimit, 20));

        int safeFormLimit =
                Math.max(1, Math.min(formLimit, 20));

        int safeEvaluationsLimit =
                Math.max(1, Math.min(evaluationsLimit, 20));

        AppDashboardSummaryDto summary =
                safeCall(
                        appMetadataService::getDashboardSummary
                );

        UpcomingPicksDto upcomingPicks =
                safeCall(
                        () -> appUpcomingPicksService
                                .getUpcomingPicks(
                                        competitionId,
                                        safePicksLimit
                                )
                );

        LeagueStandingsDto currentStandings =
                safeCall(
                        () -> appStandingsService
                                .getCurrentStandings(
                                        competitionId
                                )
                );

        LeagueFormTableDto currentFormTable =
                safeCall(
                        () -> appFormTableService
                                .getCurrentFormTable(
                                        competitionId,
                                        safeFormLimit
                                )
                );

        List<RecentEvaluationDto> recentEvaluations =
                safeCall(
                        () -> appEvaluationService
                                .getRecentEvaluations(
                                        safeEvaluationsLimit
                                )
                );

        return new DashboardHomeDto(
                OffsetDateTime.now(),
                safePicksLimit,
                safeFormLimit,
                safeEvaluationsLimit,
                summary,
                upcomingPicks,
                currentStandings,
                currentFormTable,
                recentEvaluations
        );
    }

    private <T> T safeCall(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception ex) {
            return null;
        }
    }
}