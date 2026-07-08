import React, { useState } from "react";
import { getJson } from "../api/http.js";
import { ErrorBlock, JsonBox, LoadingBlock, Metric, SectionCard } from "../components/ui.jsx";
import { useAsync } from "../components/hooks.js";
import {
  formatDecimal,
  formatProbability,
} from "../utils/formatters.js";

export function ReportsPage({ competition }) {
  const [fixtureId, setFixtureId] = useState("310");
  const [fixtureReport, setFixtureReport] = useState(null);
  const [fixtureLoading, setFixtureLoading] = useState(false);
  const [fixtureError, setFixtureError] = useState(null);

  const dashboard = useAsync(() => getJson("/api/reports/dashboard"), []);
  const recent = useAsync(() => getJson("/api/reports/recent", { limit: 10 }), []);

  const loadFixtureReport = async () => {
    setFixtureLoading(true);
    setFixtureError(null);

    try {
      const data = await getJson(`/api/reports/fixture/${fixtureId}`);
      setFixtureReport(data);
    } catch (err) {
      setFixtureError(err instanceof Error ? err.message : "Unknown error");
    } finally {
      setFixtureLoading(false);
    }
  };

  return (
    <div className="page-grid">
      <SectionCard
        title="Reports scope"
        subtitle="Current reports endpoints are global unless the backend is later extended with competition filters."
      >
        <div className="muted">
          Selected competition:{" "}
          <strong>{competition ? competition.name : "None"}</strong>
          {competition?.countryName ? ` · ${competition.countryName}` : ""}
          {competition?.currentSeasonYear ? ` · ${competition.currentSeasonYear}` : ""}
        </div>
      </SectionCard>

      {dashboard.loading || recent.loading ? <LoadingBlock label="Loading reports" /> : null}
      {dashboard.error ? <ErrorBlock error={dashboard.error} /> : null}
      {recent.error ? <ErrorBlock error={recent.error} /> : null}

      {dashboard.data ? (
        <SectionCard title="Model dashboard" subtitle="GET /api/reports/dashboard">
          <div className="metrics-grid three">
            <Metric label="Evaluated" value={dashboard.data.totalEvaluatedPredictions} />
            <Metric
  label="1X2 accuracy"
  value={formatProbability(dashboard.data.accuracy1x2)}
/>
<Metric
  label="Over 2.5"
  value={formatProbability(dashboard.data.accuracyOver25)}
/>
<Metric
  label="BTTS"
  value={formatProbability(dashboard.data.accuracyBtts)}
/>
<Metric
  label="Avg Brier"
  value={formatDecimal(dashboard.data.averageBrierScore1x2)}
/>
<Metric
  label="Avg Log Loss"
  value={formatDecimal(dashboard.data.averageLogLoss1x2)}
/>
          </div>
        </SectionCard>
      ) : null}

      <SectionCard title="Recent evaluations" subtitle="GET /api/reports/recent?limit=10">
        <JsonBox data={recent.data} />
      </SectionCard>

      <SectionCard
        title="Fixture evaluation report"
        subtitle="GET /api/reports/fixture/{fixtureId}"
      >
        <div className="inline-form">
          <input
            value={fixtureId}
            onChange={(e) => setFixtureId(e.target.value)}
            placeholder="Fixture ID"
          />
          <button className="primary" onClick={loadFixtureReport}>
            Load report
          </button>
        </div>

        {fixtureLoading ? <LoadingBlock label="Loading fixture report" /> : null}
        {fixtureError ? <ErrorBlock error={fixtureError} /> : null}
        {fixtureReport ? <JsonBox data={fixtureReport} /> : null}
      </SectionCard>
    </div>
  );
}