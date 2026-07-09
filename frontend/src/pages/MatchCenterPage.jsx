import React, { useMemo, useState } from "react";
import { API_BASE, getJson } from "../api/http.js";
import { ErrorBlock, JsonBox, LoadingBlock, Metric, SectionCard } from "../components/ui.jsx";
import { formatDateTime, formatDecimal, formatProbability } from "../utils/formatters.js";

function probabilityValue(value) {
  const number = Number(value);
  return Number.isFinite(number) ? number : 0;
}

function confidenceLabel(probability) {
  if (probability >= 0.6) return "Ridicată";
  if (probability >= 0.5) return "Medie";
  if (probability >= 0.4) return "Prudentă";
  return "Scăzută";
}

function outcomeLabel(key, homeName, awayName) {
  if (key === "home") return `1 · ${homeName}`;
  if (key === "draw") return "X · Egal";
  if (key === "away") return `2 · ${awayName}`;
  return "N/A";
}

function topOutcome(prediction, homeName, awayName) {
  if (!prediction) return null;

  const outcomes = [
    {
      key: "home",
      label: outcomeLabel("home", homeName, awayName),
      probability: probabilityValue(prediction.homeWinProbability),
    },
    {
      key: "draw",
      label: outcomeLabel("draw", homeName, awayName),
      probability: probabilityValue(prediction.drawProbability),
    },
    {
      key: "away",
      label: outcomeLabel("away", homeName, awayName),
      probability: probabilityValue(prediction.awayWinProbability),
    },
  ];

  return outcomes.sort((a, b) => b.probability - a.probability)[0];
}

function ProbabilityBar({ label, value }) {
  const probability = probabilityValue(value);
  const width = `${Math.max(0, Math.min(100, probability * 100))}%`;

  return (
    <div className="probability-row">
      <div className="probability-label">{label}</div>
      <div className="probability-track">
        <div className="probability-fill" style={{ width }} />
      </div>
      <div className="probability-value">{formatProbability(probability)}</div>
    </div>
  );
}

function readableLeagueType(baseline) {
  if (!baseline) return "Nu avem încă baseline pentru liga asta.";

  if (baseline.avgTotalGoals >= 2.7) {
    return "Ligă cu tendință de multe goluri.";
  }

  if (baseline.avgTotalGoals <= 2.2) {
    return "Ligă mai închisă, cu tendință de puține goluri.";
  }

  return "Ligă relativ echilibrată ca număr de goluri.";
}

function AbsenceSummary({ title, form }) {
  if (!form) {
    return (
      <div className="insight-card">
        <div className="insight-title">{title}</div>
        <div className="insight-note">Nu avem date de formă/absențe.</div>
      </div>
    );
  }

  return (
    <div className="insight-card">
      <div className="insight-title">{title}</div>
      <div className="insight-value">{form.knownAbsencesForFixture ?? 0} total</div>
      <div className="insight-note">
        {form.missingFixtureAbsences ?? 0} absenți siguri · {form.questionableAbsences ?? 0} incerți
      </div>
      <div className="insight-note">
        Impact model: {formatDecimal(form.absenceImpactScore ?? form.knownAbsencesForFixture ?? 0)}
      </div>
    </div>
  );
}

function TeamFormSummary({ title, form }) {
  if (!form) {
    return (
      <div className="insight-card">
        <div className="insight-title">{title}</div>
        <div className="insight-note">Nu avem date de formă.</div>
      </div>
    );
  }

  return (
    <div className="insight-card">
      <div className="insight-title">{title}</div>
      <div className="insight-value">
        {form.pointsPerGame !== undefined ? formatDecimal(form.pointsPerGame) : "—"} puncte/meci
      </div>
      <div className="insight-note">
        Formă: {form.wins ?? 0}V · {form.draws ?? 0}E · {form.losses ?? 0}Î din {form.sampleSize ?? 0} meciuri
      </div>
      <div className="insight-note">
        Goluri: {formatDecimal(form.goalsForPerGame)} marcate · {formatDecimal(form.goalsAgainstPerGame)} primite / meci
      </div>
    </div>
  );
}

function fmtNumber(value, digits = 2) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return "N/A";
  }

  return Number(value).toFixed(digits);
}

function fmtPercent(value) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return "N/A";
  }

  return `${Math.round(Number(value) * 100)}%`;
}

function formSummary(form) {
  if (!form) {
    return "Nu avem date suficiente.";
  }

  return `${form.sampleSize ?? 0} meciuri · ${form.wins ?? 0}V-${form.draws ?? 0}E-${form.losses ?? 0}Î`;
}

function goalsSummary(form) {
  if (!form) {
    return "Goluri: N/A";
  }

  return `${fmtNumber(form.goalsForPerGame)} marcate / ${fmtNumber(form.goalsAgainstPerGame)} primite pe meci`;
}

function restSummary(context) {
  if (!context || context.restDays === undefined || context.restDays < 0) {
    return "Nu știm când a jucat ultimul meci.";
  }

  if (context.shortRest) {
    return `${context.restDays} zile pauză · posibilă oboseală`;
  }

  if (context.longBreak) {
    return `${context.restDays} zile pauză · posibilă lipsă de ritm`;
  }

  return `${context.restDays} zile pauză · ritm normal`;
}

function strengthSummary(strength) {
  if (!strength) {
    return "Rating indisponibil.";
  }

  const score = Number(strength.strengthScore ?? 0);
  const label =
    score > 0.08
      ? "peste media ligii"
      : score < -0.08
        ? "sub media ligii"
        : "aproape de media ligii";

  return `${fmtNumber(score)} · ${label}`;
}

function absenceSummary(form) {
  if (!form) {
    return "Absențe: N/A";
  }

  const missing = form.missingFixtureAbsences ?? 0;
  const questionable = form.questionableAbsences ?? 0;
  const impact = form.absenceImpactScore ?? form.knownAbsencesForFixture ?? 0;

  return `${missing} absenți, ${questionable} incerți · impact ${fmtNumber(impact)}`;
}

function compareNumber(a, b, threshold = 0.05) {
  const left = Number(a ?? 0);
  const right = Number(b ?? 0);

  if (left - right > threshold) {
    return 1;
  }

  if (right - left > threshold) {
    return -1;
  }

  return 0;
}

function buildModelExplanation({
  prediction,
  bestOutcome,
  homeName,
  awayName,
  homeVenueForm,
  awayVenueForm,
  homeScheduleContext,
  awayScheduleContext,
  homeStrength,
  awayStrength,
  homeForm,
  awayForm,
  leagueBaseline,
}) {
  if (!prediction || !bestOutcome) {
    return "Nu există încă predicție calculată. Apasă Capture + reload pentru a genera o analiză completă.";
  }

  const reasons = [];

  const venueCompare = compareNumber(
    homeVenueForm?.pointsPerGame,
    awayVenueForm?.pointsPerGame,
    0.25
  );

  if (venueCompare > 0) {
    reasons.push(`${homeName} are formă mai bună acasă decât ${awayName} în deplasare`);
  } else if (venueCompare < 0) {
    reasons.push(`${awayName} arată mai bine în deplasare decât ${homeName} acasă`);
  }

  const strengthCompare = compareNumber(
    homeStrength?.strengthScore,
    awayStrength?.strengthScore,
    0.08
  );

  if (strengthCompare > 0) {
    reasons.push(`${homeName} are rating intern peste adversar`);
  } else if (strengthCompare < 0) {
    reasons.push(`${awayName} are rating intern peste adversar`);
  }

  const homeRest = Number(homeScheduleContext?.restDays ?? -1);
  const awayRest = Number(awayScheduleContext?.restDays ?? -1);

  if (homeRest >= 0 && awayRest >= 0) {
    if (homeScheduleContext?.shortRest && !awayScheduleContext?.shortRest) {
      reasons.push(`${homeName} poate fi afectată de pauză scurtă`);
    } else if (awayScheduleContext?.shortRest && !homeScheduleContext?.shortRest) {
      reasons.push(`${awayName} poate fi afectată de pauză scurtă`);
    } else if (Math.abs(homeRest - awayRest) >= 3) {
      const restedTeam = homeRest > awayRest ? homeName : awayName;
      reasons.push(`${restedTeam} are avantaj de odihnă`);
    }
  }

  const homeAbsenceImpact = Number(homeForm?.absenceImpactScore ?? 0);
  const awayAbsenceImpact = Number(awayForm?.absenceImpactScore ?? 0);

  if (homeAbsenceImpact - awayAbsenceImpact >= 1) {
    reasons.push(`${homeName} are absențe mai importante`);
  } else if (awayAbsenceImpact - homeAbsenceImpact >= 1) {
    reasons.push(`${awayName} are absențe mai importante`);
  }

  if (leagueBaseline?.avgTotalGoals >= 2.7) {
    reasons.push("liga are profil de meciuri cu multe goluri");
  } else if (leagueBaseline?.avgTotalGoals > 0 && leagueBaseline.avgTotalGoals <= 2.2) {
    reasons.push("liga are profil mai închis, cu mai puține goluri");
  }

  const probabilityText = formatProbability(bestOutcome.probability);
  const intro = `Modelul favorizează ${bestOutcome.label} cu ${probabilityText}.`;

  if (reasons.length === 0) {
    return `${intro} Nu există un singur factor dominant; predicția vine din combinația mai multor semnale apropiate.`;
  }

  return `${intro} Argumente principale: ${reasons.slice(0, 3).join("; ")}.`;
}

function firstNumber(source, keys) {
  if (!source) {
    return null;
  }

  for (const key of keys) {
    const value = source[key];
    const number = Number(value);

    if (Number.isFinite(number)) {
      return number;
    }
  }

  return null;
}

function formatSignedPercent(value) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return "N/A";
  }

  const number = Number(value);
  const sign = number > 0 ? "+" : "";

  return `${sign}${Math.round(number * 100)}%`;
}

function valueVerdict(edge) {
  if (edge === null || edge === undefined || Number.isNaN(Number(edge))) {
    return {
      label: "N/A",
      className: "neutral",
      note: "Nu avem suficiente date de cote.",
    };
  }

  if (edge >= 0.06) {
    return {
      label: "Good value",
      className: "good",
      note: "Modelul vede o diferență clar pozitivă față de piață.",
    };
  }

  if (edge >= 0.025) {
    return {
      label: "Small value",
      className: "small",
      note: "Există un mic avantaj, dar nu e foarte puternic.",
    };
  }

  if (edge > 0) {
    return {
      label: "Watchlist",
      className: "watch",
      note: "Edge pozitiv, dar prea mic pentru încredere serioasă.",
    };
  }

  return {
    label: "No bet",
    className: "bad",
    note: "Cota nu pare suficient de bună față de probabilitatea modelului.",
  };
}

function movementForSelection(oddsMovement, selection) {
  if (!oddsMovement?.available) {
    return null;
  }

  if (selection === "HOME") return oddsMovement.home;
  if (selection === "DRAW") return oddsMovement.draw;
  if (selection === "AWAY") return oddsMovement.away;

  return null;
}

function movementLabel(direction) {
  if (direction === "STEAM_IN") return "Piața intră";
  if (direction === "DRIFT_OUT") return "Piața iese";
  if (direction === "STABLE") return "Stabil";
  return "N/A";
}

function buildValueRows({ marketComparison, prediction, homeName, awayName }) {
  const market = marketComparison?.market ?? {};
  const model = marketComparison?.model ?? {};
  const edges = marketComparison?.edges ?? {};

  const rows = [
    {
      key: "HOME",
      label: `1 · ${homeName}`,
      odd: firstNumber(market, ["homeOdds", "homeOdd", "oddHome"]),
      marketProbability: firstNumber(market, [
        "normalizedHome",
        "normalizedHomeImpliedProbability",
        "homeImpliedProbability",
        "homeMarketProbability",
        "marketHomeProbability",
      ]),
      modelProbability:
        firstNumber(model, ["homeWinProbability", "homeProbability", "modelHomeProbability"]) ??
        firstNumber(prediction, ["homeWinProbability"]),
      edge: firstNumber(edges, ["homeEdge", "edgeHome"]),
    },
    {
      key: "DRAW",
      label: "X · Egal",
      odd: firstNumber(market, ["drawOdds", "drawOdd", "oddDraw"]),
      marketProbability: firstNumber(market, [
        "normalizedDraw",
        "normalizedDrawImpliedProbability",
        "drawImpliedProbability",
        "drawMarketProbability",
        "marketDrawProbability",
      ]),
      modelProbability:
        firstNumber(model, ["drawProbability", "modelDrawProbability"]) ??
        firstNumber(prediction, ["drawProbability"]),
      edge: firstNumber(edges, ["drawEdge", "edgeDraw"]),
    },
    {
      key: "AWAY",
      label: `2 · ${awayName}`,
      odd: firstNumber(market, ["awayOdds", "awayOdd", "oddAway"]),
      marketProbability: firstNumber(market, [
        "normalizedAway",
        "normalizedAwayImpliedProbability",
        "awayImpliedProbability",
        "awayMarketProbability",
        "marketAwayProbability",
      ]),
      modelProbability:
        firstNumber(model, ["awayWinProbability", "awayProbability", "modelAwayProbability"]) ??
        firstNumber(prediction, ["awayWinProbability"]),
      edge: firstNumber(edges, ["awayEdge", "edgeAway"]),
    },
  ];

  return rows.map((row) => {
    const calculatedEdge =
      row.edge ??
      (
        row.modelProbability !== null && row.marketProbability !== null
          ? row.modelProbability - row.marketProbability
          : null
      );

    const movement = movementForSelection(
      marketComparison?.oddsMovement,
      row.key
    );

    return {
      ...row,
      edge: calculatedEdge,
      movement,
    };
  });
}

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
    const response = await fetch(`${API_BASE}${path}`, { method: "POST" });
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
  const features = data?.features;
  const prediction = data?.prediction;
  const marketComparison = data?.marketComparison;

  const homeName = overview?.homeTeam?.name ?? features?.homeTeamName ?? "Home";
  const awayName = overview?.awayTeam?.name ?? features?.awayTeamName ?? "Away";

  const homeForm = features?.homeForm ?? data?.homeTeamForm;
  const awayForm = features?.awayForm ?? data?.awayTeamForm;

  const homeVenueForm = features?.homeVenueForm ?? homeForm;
  const awayVenueForm = features?.awayVenueForm ?? awayForm;

  const homeScheduleContext = features?.homeScheduleContext;
  const awayScheduleContext = features?.awayScheduleContext;

  const homeStrength = features?.homeStrength;
  const awayStrength = features?.awayStrength;

  const leagueBaseline = features?.leagueBaseline;
  const baseline = features?.leagueBaseline;

  const h2h = features?.headToHead ?? data?.h2h;

  const modelVersion =
    overview?.latestPrediction?.modelVersion ??
    data?.prediction?.modelVersion ??
    "N/A";

  const bestOutcome = useMemo(
    () => topOutcome(prediction, homeName, awayName),
    [prediction, homeName, awayName]
  );

  const exactScores = prediction?.topExactScores ?? [];

  const hasMarket =
    Boolean(marketComparison?.market) &&
    Boolean(marketComparison?.edges);

  const modelExplanation = buildModelExplanation({
    prediction,
    bestOutcome,
    homeName,
    awayName,
    homeVenueForm,
    awayVenueForm,
    homeScheduleContext,
    awayScheduleContext,
    homeStrength,
    awayStrength,
    homeForm,
    awayForm,
    leagueBaseline,
  });

  const valueBetRows = buildValueRows({
    marketComparison,
    prediction,
    homeName,
    awayName,
  });

  const bestValueRow =
    valueBetRows
      .filter((row) => row.edge !== null && row.edge !== undefined)
      .sort((a, b) => b.edge - a.edge)[0] ?? null;

  const bestValueVerdict = valueVerdict(bestValueRow?.edge);

  return (
    <div className="page-grid">
      <SectionCard
        title="Match Center"
        subtitle={
          competition
            ? `Analiză pre-meci pentru ${competition.name}.`
            : "Analiză pre-meci, predicție, formă, absențe și context de ligă."
        }
      >
        <div className="inline-form wrap">
          <input
            value={fixtureId}
            onChange={(event) => setFixtureId(event.target.value)}
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
        <SectionCard title="Ultima acțiune">
          <div className="summary-box">{actionResult}</div>
        </SectionCard>
      ) : null}

      {data ? (
        <>
          <SectionCard
            title={`${homeName} vs ${awayName}`}
            subtitle={`${formatDateTime(overview?.kickoffAt, "Dată necunoscută")} · ${overview?.statusLong ?? "Status necunoscut"}`}
          >
            <div className="metrics-grid four">
              <Metric
                label="Predicție model"
                value={bestOutcome ? bestOutcome.label : "N/A"}
                hint={bestOutcome ? formatProbability(bestOutcome.probability) : "Generează predicția întâi"}
              />
              <Metric
                label="Încredere"
                value={bestOutcome ? confidenceLabel(bestOutcome.probability) : "N/A"}
                hint="Bazată pe probabilitatea cea mai mare"
              />
              <Metric
                label="Goluri estimate"
                value={
                  features
                    ? `${formatDecimal(features.expectedHomeGoals)} - ${formatDecimal(features.expectedAwayGoals)}`
                    : "N/A"
                }
                hint="Expected goals estimate"
              />
              <Metric
                label="Value betting"
                value={hasMarket ? "Disponibil" : "Lipsesc cote"}
                hint={hasMarket ? "Există comparație cu piața" : "Rulează odds/pre-match capture dacă API-ul are cote"}
              />
            </div>

            <div className="summary-box">
              {modelExplanation}
            </div>
          </SectionCard>

          <SectionCard
            title="De ce spune modelul asta?"
            subtitle="Explicație pe înțeles: formă, odihnă, forță echipe și contextul ligii."
          >
            <div className="summary-box">
              {modelExplanation}
            </div>

            <div className="reason-grid">
              <div className="reason-card">
                <div className="reason-title">Forma acasă/deplasare</div>
                <div className="reason-team">{homeName} acasă</div>
                <div className="reason-main">{formSummary(homeVenueForm)}</div>
                <div className="reason-note">{goalsSummary(homeVenueForm)}</div>

                <div className="reason-divider" />

                <div className="reason-team">{awayName} deplasare</div>
                <div className="reason-main">{formSummary(awayVenueForm)}</div>
                <div className="reason-note">{goalsSummary(awayVenueForm)}</div>
              </div>

              <div className="reason-card">
                <div className="reason-title">Odihnă / ritm</div>
                <div className="reason-team">{homeName}</div>
                <div className="reason-main">{restSummary(homeScheduleContext)}</div>

                <div className="reason-divider" />

                <div className="reason-team">{awayName}</div>
                <div className="reason-main">{restSummary(awayScheduleContext)}</div>
              </div>

              <div className="reason-card">
                <div className="reason-title">Forță echipă</div>
                <div className="reason-team">{homeName}</div>
                <div className="reason-main">{strengthSummary(homeStrength)}</div>
                <div className="reason-note">
                  PPG {fmtNumber(homeStrength?.pointsPerGame)} · golaveraj/meci {fmtNumber(homeStrength?.goalDifferencePerGame)}
                </div>

                <div className="reason-divider" />

                <div className="reason-team">{awayName}</div>
                <div className="reason-main">{strengthSummary(awayStrength)}</div>
                <div className="reason-note">
                  PPG {fmtNumber(awayStrength?.pointsPerGame)} · golaveraj/meci {fmtNumber(awayStrength?.goalDifferencePerGame)}
                </div>
              </div>

              <div className="reason-card">
                <div className="reason-title">Context ligă</div>
                <div className="reason-main">
                  {leagueBaseline
                    ? `${fmtNumber(leagueBaseline.avgTotalGoals)} goluri/meci`
                    : "N/A"}
                </div>
                <div className="reason-note">
                  Home win {fmtPercent(leagueBaseline?.homeWinRate)} · Draw {fmtPercent(leagueBaseline?.drawRate)} · Away win {fmtPercent(leagueBaseline?.awayWinRate)}
                </div>
                <div className="reason-note">
                  Over 2.5 {fmtPercent(leagueBaseline?.over25Rate)} · BTTS {fmtPercent(leagueBaseline?.bttsRate)}
                </div>

                <div className="reason-divider" />

                <div className="reason-title">Absențe</div>
                <div className="reason-note">{homeName}: {absenceSummary(homeForm)}</div>
                <div className="reason-note">{awayName}: {absenceSummary(awayForm)}</div>

                <div className="reason-divider" />

                <div className="reason-title">Model</div>
                <div className="reason-main">{modelVersion}</div>
              </div>
            </div>
          </SectionCard>

          {prediction ? (
            <SectionCard title="Probabilități principale" subtitle="Aici vezi predicția modelului, fără jargon tehnic.">
              <div className="prediction-layout">
                <div>
                  <h3 className="mini-title">1X2</h3>
                  <ProbabilityBar label={`1 · ${homeName}`} value={prediction.homeWinProbability} />
                  <ProbabilityBar label="X · Egal" value={prediction.drawProbability} />
                  <ProbabilityBar label={`2 · ${awayName}`} value={prediction.awayWinProbability} />
                </div>

                <div>
                  <h3 className="mini-title">Goluri</h3>
                  <ProbabilityBar label="Over 2.5" value={prediction.over25Probability} />
                  <ProbabilityBar label="Under 2.5" value={prediction.under25Probability} />
                  <ProbabilityBar label="BTTS Yes" value={prediction.bttsYesProbability} />
                  <ProbabilityBar label="BTTS No" value={prediction.bttsNoProbability} />
                </div>
              </div>
            </SectionCard>
          ) : null}

          {exactScores.length > 0 ? (
            <SectionCard title="Scoruri exacte probabile" subtitle="Nu sunt recomandări de pariu, ci cele mai probabile scoruri din model.">
              <div className="score-chip-list">
                {exactScores.map((score) => (
                  <div
                    key={`${score.homeGoals}-${score.awayGoals}-${score.probability}`}
                    className="score-chip"
                  >
                    <strong>{score.homeGoals}-{score.awayGoals}</strong>
                    <span>{formatProbability(score.probability)}</span>
                  </div>
                ))}
              </div>
            </SectionCard>
          ) : null}

          <SectionCard title="Contextul ligii" subtitle="Baseline-ul spune cum se comportă liga în general.">
            {baseline ? (
              <>
                <div className="metrics-grid four">
                  <Metric label="Goluri/meci" value={formatDecimal(baseline.avgTotalGoals)} />
                  <Metric label="Goluri gazde" value={formatDecimal(baseline.avgHomeGoals)} />
                  <Metric label="Goluri oaspeți" value={formatDecimal(baseline.avgAwayGoals)} />
                  <Metric label="Meciuri analizate" value={baseline.sampleSize ?? 0} />
                </div>

                <div className="metrics-grid four mt">
                  <Metric label="Gazde câștigă" value={formatProbability(baseline.homeWinRate)} />
                  <Metric label="Egaluri" value={formatProbability(baseline.drawRate)} />
                  <Metric label="Oaspeți câștigă" value={formatProbability(baseline.awayWinRate)} />
                  <Metric label="BTTS" value={formatProbability(baseline.bttsRate)} />
                </div>

                <div className="summary-box mt">
                  {readableLeagueType(baseline)} Over 2.5 apare în {formatProbability(baseline.over25Rate)}, iar BTTS în {formatProbability(baseline.bttsRate)}.
                </div>
              </>
            ) : (
              <div className="summary-box">Nu avem încă baseline de ligă pentru acest meci.</div>
            )}
          </SectionCard>

          <SectionCard title="Formă și absențe" subtitle="Aici vezi de ce modelul poate ajusta golurile estimate.">
            <div className="insight-grid">
              <TeamFormSummary title={`${homeName} · formă`} form={homeForm} />
              <TeamFormSummary title={`${awayName} · formă`} form={awayForm} />
              <AbsenceSummary title={`${homeName} · absențe`} form={homeForm} />
              <AbsenceSummary title={`${awayName} · absențe`} form={awayForm} />
            </div>
          </SectionCard>

          <SectionCard title="Head to head" subtitle="Istoric direct recent între echipe.">
            {h2h?.sampleSize > 0 ? (
              <div className="metrics-grid four">
                <Metric label="Meciuri H2H" value={h2h.sampleSize} />
                <Metric label={`${homeName} victorii`} value={h2h.homeTeamWins ?? 0} />
                <Metric label="Egaluri" value={h2h.draws ?? 0} />
                <Metric label={`${awayName} victorii`} value={h2h.awayTeamWins ?? 0} />
              </div>
            ) : (
              <div className="summary-box">
                Nu avem istoric direct relevant în eșantionul folosit.
              </div>
            )}
          </SectionCard>

          <SectionCard title="Value bet / cote" subtitle="Comparația dintre probabilitatea modelului și probabilitatea pieței.">
            {hasMarket ? (
              <>
                <div className={`value-summary ${bestValueVerdict.className}`}>
                  <div>
                    <div className="value-summary-label">Cel mai bun semnal</div>
                    <div className="value-summary-main">
                      {bestValueRow ? bestValueRow.label : "N/A"} · {bestValueVerdict.label}
                    </div>
                    <div className="value-summary-note">
                      {bestValueVerdict.note}
                    </div>
                  </div>

                  <div className="value-summary-edge">
                    {formatSignedPercent(bestValueRow?.edge)}
                  </div>
                </div>

                {marketComparison?.oddsMovement?.summary ? (
                  <div className="summary-box mt">
                    {marketComparison.oddsMovement.summary}
                  </div>
                ) : null}

                <div className="value-table mt">
                  <div className="value-row value-header">
                    <div>Selecție</div>
                    <div>Cotă</div>
                    <div>Piață</div>
                    <div>Model</div>
                    <div>Edge</div>
                    <div>Mișcare</div>
                    <div>Verdict</div>
                  </div>

                  {valueBetRows.map((row) => {
                    const verdict = valueVerdict(row.edge);

                    return (
                      <div className="value-row" key={row.key}>
                        <div className="value-selection">{row.label}</div>
                        <div>{row.odd !== null ? fmtNumber(row.odd) : "N/A"}</div>
                        <div>{row.marketProbability !== null ? formatProbability(row.marketProbability) : "N/A"}</div>
                        <div>{row.modelProbability !== null ? formatProbability(row.modelProbability) : "N/A"}</div>
                        <div className={`value-edge ${verdict.className}`}>
                          {formatSignedPercent(row.edge)}
                        </div>
                        <div>
                          {row.movement
                            ? `${movementLabel(row.movement.direction)} · ${formatSignedPercent(row.movement.impliedProbabilityDelta)}`
                            : "N/A"}
                        </div>
                        <div>
                          <span className={`value-pill ${verdict.className}`}>
                            {verdict.label}
                          </span>
                        </div>
                      </div>
                    );
                  })}
                </div>

                {marketComparison?.summary ? (
                  <div className="summary-box mt">
                    {marketComparison.summary}
                  </div>
                ) : null}
              </>
            ) : (
              <div className="summary-box">
                {marketComparison?.oddsMovement?.summary ??
                  "Nu avem cote capturate pentru acest fixture. Fără cote, modelul poate face predicție, dar nu poate spune dacă există value bet."}
              </div>
            )}
          </SectionCard>

          <SectionCard title="Debug / payload brut">
            <details className="debug-details">
              <summary className="debug-summary">Arată JSON-ul brut pentru verificare tehnică</summary>

              <div className="debug-stack">
                <h3>Overview</h3>
                <JsonBox data={data.overview} />

                <h3>Features</h3>
                <JsonBox data={data.features} />

                <h3>Prediction</h3>
                <JsonBox data={data.prediction} />

                <h3>Market comparison</h3>
                <JsonBox data={data.marketComparison} />
              </div>
            </details>
          </SectionCard>
        </>
      ) : null}
    </div>
  );
}