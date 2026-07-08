import React, { useEffect, useState } from "react";
import { getJson } from "../api/http.js";
import { ErrorBlock, LoadingBlock, SectionCard } from "../components/ui.jsx";
import { formatDateTime } from "../utils/formatters.js";

export function FixturesPage({ competitionId, competition }) {
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("");
  const [teamId, setTeamId] = useState("");
  const [round, setRound] = useState("");
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [data, setData] = useState(null);
  const [statuses, setStatuses] = useState([]);
  const [teams, setTeams] = useState([]);
  const [rounds, setRounds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const loadFilters = async () => {
      const params = competitionId ? { competitionId } : {};

    const [statusData, teamData, roundData] = await Promise.all([
      getJson("/api/app/meta/statuses"),
      getJson("/api/app/meta/teams", params),
      getJson("/api/app/meta/rounds", params),
    ]);

    setStatuses(statusData);
    setTeams(teamData);
    setRounds(roundData);
  };

  const loadPage = async (targetPage = page) => {
    setLoading(true);
    setError(null);

    try {
      const params = {
        query,
        status,
        teamId,
        round,
        page: targetPage,
        size,
        sortBy: "kickoffAt",
        sortDir: "desc",
      };

      if (competitionId) {
        params.competitionId = competitionId;
      }

      const pageData = await getJson("/api/app/fixtures/page", params);
      setData(pageData);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unknown error");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadFilters().catch(() => undefined);
  }, [competitionId]);

  useEffect(() => {
    setTeamId("");
    setRound("");
    setPage(0);
  }, [competitionId]);

  useEffect(() => {
    loadPage().catch(() => undefined);
  }, [page, size, competitionId]);

  const onSearch = () => {
    if (page !== 0) {
      setPage(0);
      return;
    }
    loadPage(0).catch(() => undefined);
  };

  return (
    <div className="page-grid">
      <SectionCard
        title="Fixture search"
        subtitle={
          competition
            ? `Uses GET /api/app/fixtures/page filtered for ${competition.name}.`
            : "Uses GET /api/app/fixtures/page with backend filters."
        }
      >
        <div className="filter-grid">
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search teams or round"
          />

          <select value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">All statuses</option>
            {statuses.map((item) => (
              <option key={item.value} value={item.value}>
                {item.label}
              </option>
            ))}
          </select>

          <select value={teamId} onChange={(e) => setTeamId(e.target.value)}>
            <option value="">All teams</option>
            {teams.map((item) => (
              <option key={item.teamId} value={item.teamId}>
                {item.teamName}
              </option>
            ))}
          </select>

          <select value={round} onChange={(e) => setRound(e.target.value)}>
            <option value="">All rounds</option>
            {rounds.map((item) => (
              <option key={item.value} value={item.value}>
                {item.label}
              </option>
            ))}
          </select>

          <button className="primary" onClick={onSearch}>
            Search
          </button>
        </div>
      </SectionCard>

      {loading ? <LoadingBlock label="Loading fixtures" /> : null}
      {error ? <ErrorBlock error={error} /> : null}

      {data ? (
        <SectionCard
          title="Fixtures"
          subtitle={`Page ${data.page + 1} of ${Math.max(data.totalPages, 1)} · ${data.totalElements} total`}
          actions={
            <div className="inline-actions">
              <button onClick={() => setPage(Math.max(0, page - 1))} disabled={!data.hasPrevious}>
                Prev
              </button>
              <button onClick={() => setPage(page + 1)} disabled={!data.hasNext}>
                Next
              </button>
            </div>
          }
        >
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Kickoff</th>
                  <th>Status</th>
                  <th>Round</th>
                  <th>Home</th>
                  <th>Score</th>
                  <th>Away</th>
                </tr>
              </thead>
              <tbody>
                {data.items.map((item) => (
                  <tr key={item.fixtureId}>
                    <td>{item.fixtureId}</td>
                    <td>{formatDateTime(item.kickoffAt)}</td>
                    <td>{item.statusShort}</td>
                    <td>{item.leagueRound ?? "-"}</td>
                    <td>{item.homeTeamName}</td>
                    <td>
                      {item.homeGoals ?? "-"} : {item.awayGoals ?? "-"}
                    </td>
                    <td>{item.awayTeamName}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </SectionCard>
      ) : null}
    </div>
  );
}