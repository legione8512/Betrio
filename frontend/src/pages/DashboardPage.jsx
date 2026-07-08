import React, { useMemo } from "react";
import { getJson } from "../api/http.js";
import { ErrorBlock, JsonBox, LoadingBlock, Metric, SectionCard } from "../components/ui.jsx";
import { useAsync } from "../components/hooks.js";
import {
  formatDateTime,
  formatDecimal,
  formatProbability,
} from "../utils/formatters.js";

export function DashboardPage({ competitionId, competition }) {
  const dashboardPath = useMemo(() => {
    const params = new URLSearchParams();

    if (competitionId) {
      params.set("competitionId", String(competitionId));
    }

    const query = params.toString();
    return query ? `/api/app/dashboard/home?${query}` : "/api/app/dashboard/home";
  }, [competitionId]);

  const dashboard = useAsync(() => getJson(dashboardPath), [dashboardPath]);
  const model = useAsync(() => getJson("/api/reports/dashboard"), []);

  if (dashboard.loading || model.loading) return <LoadingBlock label="Loading dashboard" />;
  if (dashboard.error) return <ErrorBlock error={dashboard.error} />;
  if (model.error) return <ErrorBlock error={model.error} />;
  if (!dashboard.data || !model.data) return null;

  const summary = dashboard.data.summary;

  return (
    <div className="page-grid">
      <SectionCard
        title="Overview"
        subtitle={
          competition
            ? `Summary aggregated by the backend dashboard endpoint for ${competition.name}.`
            : "Summary aggregated by the backend dashboard endpoint."
        }
      >
        <div className="metrics-grid four">
          <Metric label="Fixtures" value={summary?.totalFixtures ?? 0} />
          <Metric label="Finished" value={summary?.finishedFixtures ?? 0} />
          <Metric label="Upcoming" value={summary?.upcomingFixtures ?? 0} />
          <Metric label="Evaluated" value={summary?.evaluatedPredictions ?? 0} />
        </div>
      </SectionCard>

      <SectionCard title="Model quality" subtitle="From GET /api/reports/dashboard">
        <div className="metrics-grid three">
          <Metric
  label="1X2 accuracy"
  value={formatProbability(model.data.accuracy1x2)}
/>
<Metric
  label="Over 2.5 accuracy"
  value={formatProbability(model.data.accuracyOver25)}
/>
<Metric
  label="BTTS accuracy"
  value={formatProbability(model.data.accuracyBtts)}
/>
<Metric
  label="Exact score hit rate"
  value={formatProbability(model.data.exactScoreHitRate)}
/>
<Metric
  label="Avg Brier"
  value={formatDecimal(model.data.averageBrierScore1x2)}
/>
<Metric
  label="Avg Log Loss"
  value={formatDecimal(model.data.averageLogLoss1x2)}
/>
        </div>
      </SectionCard>

      <SectionCard title="Upcoming picks" subtitle="From GET /api/app/recommendations/upcoming">
        {dashboard.data.upcomingPicks?.picks?.length ? (
          <div className="list-stack">
            {dashboard.data.upcomingPicks.picks.map((pick) => (
              <div key={pick.fixtureId} className="list-item">
                <div>
                  <strong>
                    {pick.homeTeamName} vs {pick.awayTeamName}
                  </strong>
                  <div className="muted">
  {formatDateTime(pick.kickoffAt)}
</div>
                  <div className="muted">{pick.summary ?? "No summary available."}</div>
                </div>

                <div className="right-cluster">
                  <span>{pick.recommendedResultCode ?? "N/A"}</span>
                  <span>{pick.confidenceTier ?? "N/A"}</span>
                  <span>{formatProbability(pick.confidenceScore)}</span>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="muted">No upcoming picks returned by the backend.</div>
        )}
      </SectionCard>

      <SectionCard title="Standings snapshot" subtitle="Current standings and current form table from the backend.">
        <JsonBox
          data={{
            currentStandings: dashboard.data.currentStandings,
            currentFormTable: dashboard.data.currentFormTable,
          }}
        />
      </SectionCard>

      <SectionCard title="Recent evaluations" subtitle="Recent evaluated predictions from the dashboard endpoint.">
        <JsonBox data={dashboard.data.recentEvaluations} />
      </SectionCard>
    </div>
  );
}