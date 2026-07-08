import React, { useState } from "react";
import { getJson } from "../api/http.js";
import { ErrorBlock, JsonBox, LoadingBlock, Metric, SectionCard } from "../components/ui.jsx";

export function TeamsPage({ competition }) {
  const [teamId, setTeamId] = useState("12");
  const [overview, setOverview] = useState(null);
  const [form, setForm] = useState(null);
  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const load = async () => {
    setLoading(true);
    setError(null);

    try {
      const [overviewData, formData, reportData] = await Promise.all([
        getJson(`/api/app/team/${teamId}/overview`),
        getJson(`/api/app/team/${teamId}/form`, { limit: 10 }),
        getJson(`/api/reports/team/${teamId}`),
      ]);

      setOverview(overviewData);
      setForm(formData);
      setReport(reportData);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unknown error");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page-grid">
      <SectionCard
        title="Team view"
        subtitle={
          competition
            ? `Overview, form and team report. Selected competition: ${competition.name}.`
            : "Overview, form and team report from the backend."
        }
      >
        <div className="inline-form">
          <input
            value={teamId}
            onChange={(e) => setTeamId(e.target.value)}
            placeholder="Team ID"
          />
          <button className="primary" onClick={load}>
            Load
          </button>
        </div>
      </SectionCard>

      {loading ? <LoadingBlock label="Loading team" /> : null}
      {error ? <ErrorBlock error={error} /> : null}

      {overview && form && report ? (
        <>
          <SectionCard
            title={overview.teamName ?? "Team overview"}
            subtitle="GET /api/app/team/{teamId}/overview"
          >
            <div className="metrics-grid four">
              <Metric label="Points" value={overview.summary?.points ?? 0} />
              <Metric label="Wins" value={overview.summary?.wins ?? 0} />
              <Metric label="Avg GF" value={overview.summary?.averageGoalsFor ?? 0} />
              <Metric label="Avg GA" value={overview.summary?.averageGoalsAgainst ?? 0} />
            </div>
          </SectionCard>

          <SectionCard title="Extended team form">
            <div className="metrics-grid four">
              <Metric label="Sample" value={form.sampleSize ?? 0} />
              <Metric label="Form sequence" value={form.formSequence ?? "N/A"} />
              <Metric label="Clean sheets" value={form.overall?.cleanSheets ?? 0} />
              <Metric label="Over 2.5 count" value={form.overall?.over25Count ?? 0} />
            </div>
            <JsonBox data={form} />
          </SectionCard>

          <SectionCard title="Team report">
            <JsonBox data={report} />
          </SectionCard>
        </>
      ) : null}
    </div>
  );
}