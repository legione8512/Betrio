import React, { useState } from "react";
import { API_BASE, getJson, postJson } from "../api/http.js";
import { ErrorBlock, JsonBox, LoadingBlock, SectionCard } from "../components/ui.jsx";

export function ActionsPage({ competitionId, competition }) {
  const [fixtureId, setFixtureId] = useState("585");
  const [batchLimit, setBatchLimit] = useState("50");
  const [result, setResult] = useState(null);
  const [history, setHistory] = useState(null);
  const [fixtureHistory, setFixtureHistory] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const postText = async (path) => {
    const response = await fetch(`${API_BASE}${path}`, {
      method: "POST",
    });

    const text = await response.text();

    if (!response.ok) {
      throw new Error(text || "Unknown error");
    }

    return { message: text };
  };

  const runBaseRefresh = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await postJson("/api/actions/base-refresh");
      setResult(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unknown error");
    } finally {
      setLoading(false);
    }
  };

  const runSmartUpdate = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await postJson(`/api/actions/fixture/${fixtureId}/smart-update`);
      setResult(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unknown error");
    } finally {
      setLoading(false);
    }
  };

  const runPredictionCapture = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await postText(`/api/predictions/fixture/${fixtureId}/capture`);
      setResult(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unknown error");
    } finally {
      setLoading(false);
    }
  };

  const runPreMatchCapture = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await postText(`/api/fixture/${fixtureId}/pre-match-capture`);
      setResult(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unknown error");
    } finally {
      setLoading(false);
    }
  };

  const runCompetitionBatchPreMatchCapture = async () => {
    if (!competitionId) {
      setError("No competition selected.");
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const safeLimit = Math.max(1, Number(batchLimit || 50));
      const data = await postText(
        `/api/competition/${competitionId}/upcoming/pre-match-capture?limit=${safeLimit}`
      );
      setResult(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unknown error");
    } finally {
      setLoading(false);
    }
  };

  const loadHistory = async () => {
    setLoading(true);
    setError(null);
    try {
      const [allData, fixtureData] = await Promise.all([
        getJson("/api/actions/history", { limit: 20 }),
        getJson(`/api/actions/fixture/${fixtureId}/history`, { limit: 20 }),
      ]);
      setHistory(allData);
      setFixtureHistory(fixtureData);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unknown error");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page-grid">
      <SectionCard
        title="Prediction capture"
        subtitle={
          competition
            ? `Prediction tools for ${competition.name}.`
            : "Prediction generation and pre-match capture."
        }
      >
        <div className="inline-form wrap">
          <input
            value={fixtureId}
            onChange={(e) => setFixtureId(e.target.value)}
            placeholder="Fixture ID"
          />
          <button className="primary" onClick={runPredictionCapture}>
            POST prediction capture
          </button>
          <button className="primary" onClick={runPreMatchCapture}>
            POST pre-match capture
          </button>
        </div>

        <div className="inline-form wrap" style={{ marginTop: "12px" }}>
          <input
            value={batchLimit}
            onChange={(e) => setBatchLimit(e.target.value)}
            placeholder="Batch limit"
          />
          <button
            className="primary"
            onClick={runCompetitionBatchPreMatchCapture}
            disabled={!competitionId}
          >
            POST competition batch pre-match capture
          </button>
        </div>

        <div className="muted" style={{ marginTop: "10px" }}>
          Selected competition: {competition ? competition.name : "None"}
          {competitionId ? ` (ID ${competitionId})` : ""}
        </div>
      </SectionCard>

      <SectionCard title="Manual actions" subtitle="Exact action endpoints exposed by the backend.">
        <div className="inline-form wrap">
          <button className="primary" onClick={runBaseRefresh}>
            POST /api/actions/base-refresh
          </button>
          <input
            value={fixtureId}
            onChange={(e) => setFixtureId(e.target.value)}
            placeholder="Fixture ID"
          />
          <button className="primary" onClick={runSmartUpdate}>
            POST smart-update
          </button>
          <button onClick={loadHistory}>Load history</button>
        </div>
      </SectionCard>

      {loading ? <LoadingBlock label="Running action" /> : null}
      {error ? <ErrorBlock error={error} /> : null}

      {result ? (
        <SectionCard title="Latest action result">
          <JsonBox data={result} />
        </SectionCard>
      ) : null}

      {history ? (
        <SectionCard title="Action history">
          <JsonBox data={history} />
        </SectionCard>
      ) : null}

      {fixtureHistory ? (
        <SectionCard title="Fixture history">
          <JsonBox data={fixtureHistory} />
        </SectionCard>
      ) : null}
    </div>
  );
}