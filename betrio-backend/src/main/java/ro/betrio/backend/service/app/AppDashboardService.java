package ro.betrio.backend.service.app;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.betrio.backend.api.dto.app.DashboardHomeDto;

@Service
public class AppDashboardService {

    private final AppInternalApiClient appInternalApiClient;

    public AppDashboardService(AppInternalApiClient appInternalApiClient) {
        this.appInternalApiClient = appInternalApiClient;
    }

    @Transactional(readOnly = true)
    public DashboardHomeDto getDashboardHome(int picksLimit, int formLimit, int evaluationsLimit, Long competitionId) {
        int safePicksLimit = Math.max(1, Math.min(picksLimit, 20));
        int safeFormLimit = Math.max(1, Math.min(formLimit, 20));
        int safeEvaluationsLimit = Math.max(1, Math.min(evaluationsLimit, 20));

        Object summary = safeCall(() -> appInternalApiClient.getDashboardSummary());
        Object upcomingPicks = safeCall(() -> appInternalApiClient.getUpcomingPicks(competitionId, safePicksLimit));
        Object currentStandings = safeCall(() -> appInternalApiClient.getCurrentStandings(competitionId));
        Object currentFormTable = safeCall(() -> appInternalApiClient.getCurrentFormTable(competitionId, safeFormLimit));
        Object recentEvaluations = safeCall(() -> appInternalApiClient.getRecentEvaluations(safeEvaluationsLimit));

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

    private Object safeCall(SupplierWithException supplier) {
        try {
            return supplier.get();
        } catch (Exception ex) {
            return null;
        }
    }

    @FunctionalInterface
    private interface SupplierWithException {
        Object get() throws Exception;
    }
}