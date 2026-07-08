import React, { useEffect, useMemo, useState } from "react";
import { API_BASE } from "./api/http.js";
import { DashboardPage } from "./pages/DashboardPage.jsx";
import { FixturesPage } from "./pages/FixturesPage.jsx";
import { MatchCenterPage } from "./pages/MatchCenterPage.jsx";
import { TeamsPage } from "./pages/TeamsPage.jsx";
import { ReportsPage } from "./pages/ReportsPage.jsx";
import { ActionsPage } from "./pages/ActionsPage.jsx";
import "./app-competition.css";

const NAV_ITEMS = [
  { key: "dashboard", label: "Dashboard", description: "Home metrics, picks, standings and evaluations" },
  { key: "fixtures", label: "Fixtures", description: "Search, filter and page fixtures" },
  { key: "match-center", label: "Match Center", description: "Fixture overview, H2H, market, features and prediction" },
  { key: "teams", label: "Teams", description: "Team overview, form and report" },
  { key: "reports", label: "Reports", description: "Model dashboard and evaluation reports" },
  { key: "actions", label: "Actions", description: "Manual operations and action history" },
];

export default function App() {
  const [view, setView] = useState("dashboard");
  const [competitions, setCompetitions] = useState([]);
  const [selectedCompetitionId, setSelectedCompetitionId] = useState("");
  const [competitionsLoading, setCompetitionsLoading] = useState(true);
  const [competitionsError, setCompetitionsError] = useState("");

  useEffect(() => {
    loadCompetitions();
  }, []);

  async function loadCompetitions() {
    try {
      setCompetitionsLoading(true);
      setCompetitionsError("");

      const response = await fetch(`${API_BASE}/api/app/meta/competitions`);
      if (!response.ok) {
        throw new Error("Failed to load competitions");
      }

      const data = await response.json();

      const items = Array.isArray(data)
        ? data
        : data?.items || data?.competitions || data?.data || [];

      const normalized = items.map((item) => ({
        id: item.id ?? item.competitionId ?? item.competition_id,
        name:
          item.competitionName ??
          item.name ??
          item.competition_name ??
          `Competition ${item.id ?? item.competitionId}`,
        countryName: item.countryName ?? item.country_name ?? "",
        currentSeasonYear:
          item.currentSeasonYear ??
          item.currentSeason?.externalSeasonYear ??
          "",
      }));

      setCompetitions(normalized);

      if (normalized.length > 0) {
        setSelectedCompetitionId(String(normalized[0].id));
      }
    } catch (error) {
      setCompetitionsError(error.message || "Failed to load competitions");
    } finally {
      setCompetitionsLoading(false);
    }
  }

  const selectedCompetition = useMemo(() => {
    return competitions.find((item) => String(item.id) === String(selectedCompetitionId)) || null;
  }, [competitions, selectedCompetitionId]);

  const sharedPageProps = useMemo(
    () => ({
      competitionId: selectedCompetitionId ? Number(selectedCompetitionId) : null,
      competition: selectedCompetition,
    }),
    [selectedCompetitionId, selectedCompetition]
  );

  const content = useMemo(() => {
    switch (view) {
      case "dashboard":
        return <DashboardPage {...sharedPageProps} />;
      case "fixtures":
        return <FixturesPage {...sharedPageProps} />;
      case "match-center":
        return <MatchCenterPage {...sharedPageProps} />;
      case "teams":
        return <TeamsPage {...sharedPageProps} />;
      case "reports":
        return <ReportsPage {...sharedPageProps} />;
      case "actions":
        return <ActionsPage {...sharedPageProps} />;
      default:
        return null;
    }
  }, [view, sharedPageProps]);

  const currentNavItem = NAV_ITEMS.find((item) => item.key === view);

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark">B</div>
          <div>
            <h1>Betrio</h1>
            <p>Frontend matched to the backend archive</p>
          </div>
        </div>

        <nav className="nav-list">
          {NAV_ITEMS.map((item) => (
            <button
              key={item.key}
              className={item.key === view ? "nav-item active" : "nav-item"}
              onClick={() => setView(item.key)}
            >
              <span className="nav-label">{item.label}</span>
              <span className="nav-description">{item.description}</span>
            </button>
          ))}
        </nav>

        <div className="sidebar-footer">
          <div className="sidebar-caption">Backend base URL</div>
          <code>{API_BASE}</code>
          <div className="sidebar-note">Change it in .env using VITE_API_BASE_URL.</div>
        </div>
      </aside>

      <main className="main-area">
        <header className="page-header">
          <div className="page-header-top">
            <div className="page-header-main">
              <div className="eyebrow">Betrio frontend</div>
              <h2>{currentNavItem?.label}</h2>
              <p>{currentNavItem?.description}</p>
            </div>

            <div className="competition-switcher">
              <label className="competition-switcher-label">Competition</label>

              <select
                value={selectedCompetitionId}
                onChange={(e) => setSelectedCompetitionId(e.target.value)}
                disabled={competitionsLoading || competitions.length === 0}
                className="competition-switcher-select"
              >
                {competitionsLoading ? (
                  <option value="">Loading competitions...</option>
                ) : competitions.length === 0 ? (
                  <option value="">No competitions found</option>
                ) : (
                  competitions.map((competition) => (
                    <option key={competition.id} value={competition.id}>
                      {competition.name}
                      {competition.countryName ? ` · ${competition.countryName}` : ""}
                      {competition.currentSeasonYear ? ` · ${competition.currentSeasonYear}` : ""}
                    </option>
                  ))
                )}
              </select>

              {competitionsError ? (
                <div className="competition-switcher-error">{competitionsError}</div>
              ) : null}
            </div>
          </div>
        </header>

        {content}
      </main>
    </div>
  );
}