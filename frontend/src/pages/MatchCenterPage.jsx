import React, { useState } from "react";
import { API_BASE, getJson } from "../api/http.js";
import { ErrorBlock, JsonBox, LoadingBlock, Metric, SectionCard } from "../components/ui.jsx";
import {
  formatDateTime,
  formatProbability,
} from "../utils/formatters.js";

export function MatchCenterPage({ competition }) {
  const [fixtureId, setFixtureId] = useState("585");
  const [data, setData] = useState(null);
  const [actionResult, setActionResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState(null);

  const load = async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await getJson(`/api/app/fixture/${fixtureId}/match-center`, {
        h2hLimit: 5,
        formLimit: 5,
      });
      setData(response);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unknown error");
    } finally {
      setLoading(false);
    }
  };

  const postText = async (path) => {
    const response = await fetch(`${API_BASE}${path}`, {
      method: "POST",
    });

    const text = await response.text();

    if (!response.ok) {
      throw new Error(text || "Unknown error");
    }

    return text;
  };

  const runPredictionCapture = async () => {
    setActionLoading(true);
    setError(null);
    setActionResult(null);

    try {
      const text = await postText(`/api/predictions/fixture/${fixtureId}/capture`);
      setActionResult(text);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unknown error");
    } finally {
      setActionLoading(false);
    }
  };

  const runPreMatchCapture = async () => {
    setActionLoading(true);
    setError(null);
    setActionResult(null);

    try {
      const text = await postText(`/api/fixture/${fixtureId}/pre-match-capture`);
      setActionResult(text);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unknown error");
    } finally {
      setActionLoading(false);
    }
  };

  const runPreMatchCaptureAndReload = async () => {
    setActionLoading(true);
    setError(null);
    setActionResult(null);

    try {
      const text = await postText(`/api/fixture/${fixtureId}/pre-match-capture`);
      setActionResult(text);

      const response = await getJson(`/api/app/fixture/${fixtureId}/match-center`, {
        h2hLimit: 5,
        formLimit: 5,
      });
      setData(response);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unknown error");
    } finally {
      setActionLoading(false);
    }
  };

  const overview = data?.overview;
  const homeName = overview?.homeTeam?.name ?? "Unknown Home";
  const awayName = overview?.awayTeam?.name ?? "Unknown Away";

  return (
    <div className="page-grid">
      <SectionCard
        title="Match center"
        subtitle={
          competition
            ? `Exact backend payload from GET /api/app/fixture/{fixtureId}/match-center · ${competition.name}`
            : "Exact backend payload from GET /api/app/fixture/{fixtureId}/match-center"
        }
      >
        <div className="inline-form wrap">
          <input
            value={fixtureId}
            onChange={(e) => setFixtureId(e.target.value)}
            placeholder="Fixture ID"
          />
          <button className="primary" onClick={load} disabled={loading || actionLoading}>
            Load
          </button>
          <button onClick={runPredictionCapture} disabled={loading || actionLoading}>
            Prediction capture
          </button>
          <button onClick={runPreMatchCapture} disabled={loading || actionLoading}>
            Pre-match capture
          </button>
          <button className="primary" onClick={runPreMatchCaptureAndReload} disabled={loading || actionLoading}>
            Capture + reload
          </button>
        </div>
      </SectionCard>

      {loading ? <LoadingBlock label="Loading match center" /> : null}
      {actionLoading ? <LoadingBlock label="Running capture action" /> : null}
      {error ? <ErrorBlock error={error} /> : null}

      {actionResult ? (
        <SectionCard title="Latest action result">
          <div className="summary-box">{actionResult}</div>
        </SectionCard>
      ) : null}

      {data ? (
        <>
          <SectionCard
            title={`${homeName} vs ${awayName}`}
            subtitle={`${formatDateTime(
  overview?.kickoffAt,
  "Unknown kickoff"
)} · ${overview?.statusLong ?? "Unknown status"}`}
          >
            <div className="metrics-grid four">
              <Metric label="Recommendation" value={data.predictionExplanation?.recommendedResultCode ?? "N/A"} />
<Metric
  label="Confidence"
  value={
    data.predictionExplanation?.confidenceTier
      ? `${data.predictionExplanation.confidenceTier} · ${formatProbability(
          data.predictionExplanation.topProbability
        )}`
      : "N/A"
  }
/>              <Metric label="Totals lean" value={data.predictionExplanation?.overUnderLean ?? "N/A"} />
              <Metric label="BTTS lean" value={data.predictionExplanation?.bttsLean ?? "N/A"} />
            </div>

            <div className="summary-box">
              {data.predictionExplanation?.summary ?? "No summary available."}
            </div>
          </SectionCard>

          <SectionCard title="Overview">
            <JsonBox data={data.overview} />
          </SectionCard>

          <SectionCard title="Head to head">
            <JsonBox data={data.h2h} />
          </SectionCard>

          <SectionCard title="Home team form">
            <JsonBox data={data.homeTeamForm} />
          </SectionCard>

          <SectionCard title="Away team form">
            <JsonBox data={data.awayTeamForm} />
          </SectionCard>

          <SectionCard title="Features">
            <JsonBox data={data.features} />
          </SectionCard>

          <SectionCard title="Prediction">
            <JsonBox data={data.prediction} />
          </SectionCard>

          <SectionCard title="Market comparison">
            <JsonBox data={data.marketComparison} />
          </SectionCard>
        </>
      ) : null}
    </div>
  );
}