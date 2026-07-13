import React, { useState } from "react";

import { getJson } from "../api/http.js";
import { useAsync } from "../components/hooks.js";

import {
  ErrorBlock,
  JsonBox,
  LoadingBlock,
  Metric,
  SectionCard,
} from "../components/ui.jsx";

import {
  formatDecimal,
  formatProbability,
} from "../utils/formatters.js";

function formatGap(value) {
  if (value == null || Number.isNaN(Number(value))) {
    return "N/A";
  }

  const numeric = Number(value);
  const sign = numeric > 0 ? "+" : "";

  return `${sign}${formatProbability(numeric)}`;
}

function calibrationVerdict(gap) {
  const numeric = Number(gap ?? 0);
  const absoluteValue = Math.abs(numeric);

  if (absoluteValue < 0.05) {
    return "Calibrare bună";
  }

  if (numeric > 0) {
    return "Model prudent";
  }

  return "Model optimist";
}

function selectionLabel(selection) {
  if (selection === "HOME") {
    return "Gazde";
  }

  if (selection === "DRAW") {
    return "Egal";
  }

  if (selection === "AWAY") {
    return "Oaspeți";
  }

  return selection ?? "Necunoscut";
}

export function ReportsPage({ competition }) {
  const [fixtureId, setFixtureId] = useState("310");
  const [fixtureReport, setFixtureReport] = useState(null);
  const [fixtureLoading, setFixtureLoading] = useState(false);
  const [fixtureError, setFixtureError] = useState(null);
  const [fixtureNotice, setFixtureNotice] = useState(null);

  const [
    valueBetMinEdgePercent,
    setValueBetMinEdgePercent,
  ] = useState("3");

  const [
    appliedValueBetMinEdge,
    setAppliedValueBetMinEdge,
  ] = useState(0.03);

  const [
    valueBetRefresh,
    setValueBetRefresh,
  ] = useState(0);

  const dashboard = useAsync(
    () => getJson("/api/reports/dashboard"),
    []
  );

  const recent = useAsync(
    () =>
      getJson("/api/reports/recent", {
        limit: 10,
      }),
    []
  );

  const modelVersions = useAsync(
    () => getJson("/api/reports/model-versions"),
    []
  );

  const calibration = useAsync(
    () => getJson("/api/reports/calibration"),
    []
  );

  const valueBets = useAsync(
    () =>
      getJson("/api/reports/value-bets", {
        minEdge: appliedValueBetMinEdge,
      }),
    [appliedValueBetMinEdge, valueBetRefresh]
  );

  const loadFixtureReport = async () => {
    setFixtureLoading(true);
    setFixtureError(null);
    setFixtureNotice(null);
    setFixtureReport(null);

    try {
      const data = await getJson(
        `/api/reports/fixture/${fixtureId}`
      );

      setFixtureReport(data);
    } catch (error) {
      if (error?.status === 404) {
        setFixtureNotice(
          "Nu există încă o evaluare pentru acest meci. " +
            "Pentru un meci viitor, raportul devine disponibil " +
            "după terminarea meciului și rularea Smart Update."
        );
      } else {
        setFixtureError(
          error instanceof Error
            ? error.message
            : "A apărut o eroare necunoscută."
        );
      }
    } finally {
      setFixtureLoading(false);
    }
  };

  const applyValueBetThreshold = () => {
    const parsedValue = Number(valueBetMinEdgePercent);

    const safePercent = Number.isFinite(parsedValue)
      ? Math.max(0, Math.min(50, parsedValue))
      : 3;

    setValueBetMinEdgePercent(String(safePercent));
    setAppliedValueBetMinEdge(safePercent / 100);
    setValueBetRefresh((current) => current + 1);
  };

  const reportsLoading =
    dashboard.loading ||
    recent.loading ||
    modelVersions.loading ||
    calibration.loading ||
    valueBets.loading;

  return (
    <div className="page-grid">
      <SectionCard
        title="Reports scope"
        subtitle="Rapoartele sunt globale și includ toate competițiile disponibile."
      >
        <div className="muted">
          Competiția selectată:{" "}
          <strong>
            {competition ? competition.name : "Niciuna"}
          </strong>

          {competition?.countryName
            ? ` · ${competition.countryName}`
            : ""}

          {competition?.currentSeasonYear
            ? ` · ${competition.currentSeasonYear}`
            : ""}
        </div>
      </SectionCard>

      {reportsLoading ? (
        <LoadingBlock label="Loading reports" />
      ) : null}

      {dashboard.error ? (
        <ErrorBlock error={dashboard.error} />
      ) : null}

      {recent.error ? (
        <ErrorBlock error={recent.error} />
      ) : null}

      {modelVersions.error ? (
        <ErrorBlock error={modelVersions.error} />
      ) : null}

      {calibration.error ? (
        <ErrorBlock error={calibration.error} />
      ) : null}

      {valueBets.error ? (
        <ErrorBlock error={valueBets.error} />
      ) : null}

      {dashboard.data ? (
        <SectionCard
          title="Model dashboard"
          subtitle="Performanța generală a predicțiilor evaluate."
        >
          <div className="metrics-grid three">
            <Metric
              label="Predicții evaluate"
              value={
                dashboard.data.totalEvaluatedPredictions
              }
            />

            <Metric
              label="Acuratețe 1X2"
              value={formatProbability(
                dashboard.data.accuracy1x2
              )}
            />

            <Metric
              label="Over/Under 2.5"
              value={formatProbability(
                dashboard.data.accuracyOver25
              )}
            />

            <Metric
              label="BTTS"
              value={formatProbability(
                dashboard.data.accuracyBtts
              )}
            />

            <Metric
              label="Brier mediu"
              value={formatDecimal(
                dashboard.data.averageBrierScore1x2
              )}
            />

            <Metric
              label="Log loss mediu"
              value={formatDecimal(
                dashboard.data.averageLogLoss1x2
              )}
            />
          </div>
        </SectionCard>
      ) : null}

      <SectionCard
        title="Model versions"
        subtitle="Compară performanța fiecărei versiuni de model."
      >
        {modelVersions.data?.length ? (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Model</th>
                  <th>Evaluări</th>
                  <th>1X2</th>
                  <th>Over 2.5</th>
                  <th>BTTS</th>
                  <th>Scor exact</th>
                  <th>Brier</th>
                  <th>Log loss</th>
                </tr>
              </thead>

              <tbody>
                {modelVersions.data.map(
                  (row, index) => (
                    <tr
                      key={`${
                        row.modelVersion ?? "unknown"
                      }-${index}`}
                    >
                      <td>
                        {row.modelVersion ?? "unknown"}
                      </td>

                      <td>{row.evaluations ?? 0}</td>

                      <td>
                        {formatProbability(
                          row.accuracy1x2
                        )}
                      </td>

                      <td>
                        {formatProbability(
                          row.accuracyOver25
                        )}
                      </td>

                      <td>
                        {formatProbability(
                          row.accuracyBtts
                        )}
                      </td>

                      <td>
                        {formatProbability(
                          row.exactScoreHitRate
                        )}
                      </td>

                      <td>
                        {formatDecimal(
                          row.avgBrierScore
                        )}
                      </td>

                      <td>
                        {formatDecimal(
                          row.avgLogLoss
                        )}
                      </td>
                    </tr>
                  )
                )}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="summary-box">
            Nu există încă evaluări pe versiuni de
            model. Rulează evaluări pentru meciuri
            terminate.
          </div>
        )}
      </SectionCard>

      <SectionCard
        title="Calibration buckets"
        subtitle="Verifică dacă probabilitățile modelului sunt realiste."
      >
        {calibration.data?.length ? (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Piață</th>
                  <th>Interval</th>
                  <th>Evaluări</th>
                  <th>Probabilitate model</th>
                  <th>Rată reală</th>
                  <th>Diferență</th>
                  <th>Interpretare</th>
                </tr>
              </thead>

              <tbody>
                {calibration.data.map(
                  (row, index) => (
                    <tr
                      key={`${row.market}-${row.bucket}-${index}`}
                    >
                      <td>{row.market}</td>
                      <td>{row.bucket}</td>
                      <td>{row.evaluations ?? 0}</td>

                      <td>
                        {formatProbability(
                          row.averagePredictedProbability
                        )}
                      </td>

                      <td>
                        {formatProbability(
                          row.actualHitRate
                        )}
                      </td>

                      <td>
                        {formatGap(
                          row.calibrationGap
                        )}
                      </td>

                      <td>
                        {calibrationVerdict(
                          row.calibrationGap
                        )}
                      </td>
                    </tr>
                  )
                )}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="summary-box">
            Nu există încă suficiente evaluări pentru
            calibrare.
          </div>
        )}

        <div className="summary-box mt">
          Dacă rata reală este mult sub probabilitatea
          modelului, modelul este prea optimist. Dacă este
          mult peste, modelul este prea prudent.
        </div>
      </SectionCard>

      <SectionCard
        title="Value Bet performance / ROI"
        subtitle="Performanța pariurilor virtuale 1X2 folosind cotele existente la momentul predicției."
      >
        <div className="inline-form wrap">
          <input
            type="number"
            min="0"
            max="50"
            step="0.5"
            value={valueBetMinEdgePercent}
            onChange={(event) =>
              setValueBetMinEdgePercent(
                event.target.value
              )
            }
            placeholder="Edge minim (%)"
          />

          <button
            className="primary"
            onClick={applyValueBetThreshold}
          >
            Aplică pragul și reîncarcă
          </button>
        </div>

        <div
          className="muted"
          style={{ marginTop: "10px" }}
        >
          Este inclus un pariu virtual dacă edge-ul
          modelului este cel puțin egal cu pragul ales.
          Fiecare pariu are o miză virtuală de 1 unitate.
        </div>

        {valueBets.data ? (
          <>
            <div
              className="metrics-grid three"
              style={{ marginTop: "14px" }}
            >
              <Metric
                label="Predicții evaluate"
                value={
                  valueBets.data
                    .evaluatedPredictions ?? 0
                }
              />

              <Metric
                label="Evaluări cu cote"
                value={
                  valueBets.data.marketEvaluations ?? 0
                }
              />

              <Metric
                label="Pariuri virtuale"
                value={
                  valueBets.data.virtualBets ?? 0
                }
              />

              <Metric
                label="Câștigate"
                value={
                  valueBets.data.winningBets ?? 0
                }
              />

              <Metric
                label="Pierdute"
                value={
                  valueBets.data.losingBets ?? 0
                }
              />

              <Metric
                label="Miză totală"
                value={formatDecimal(
                  valueBets.data.totalStake
                )}
              />

              <Metric
                label="Profit"
                value={formatDecimal(
                  valueBets.data.profit
                )}
              />

              <Metric
                label="ROI"
                value={formatProbability(
                  valueBets.data.roi
                )}
              />

              <Metric
                label="Win rate"
                value={formatProbability(
                  valueBets.data.winRate
                )}
              />

              <Metric
                label="Edge mediu"
                value={formatProbability(
                  valueBets.data.averageEdge
                )}
              />

              <Metric
                label="Cotă medie"
                value={formatDecimal(
                  valueBets.data.averageOdd
                )}
              />

              <Metric
                label="Prag aplicat"
                value={formatProbability(
                  valueBets.data.minEdge
                )}
              />
            </div>

            {valueBets.data.selections?.length ? (
              <div
                className="table-wrap"
                style={{ marginTop: "16px" }}
              >
                <table>
                  <thead>
                    <tr>
                      <th>Selecție</th>
                      <th>Pariuri</th>
                      <th>Câștigate</th>
                      <th>Pierdute</th>
                      <th>Profit</th>
                      <th>ROI</th>
                      <th>Win rate</th>
                      <th>Edge mediu</th>
                      <th>Cotă medie</th>
                    </tr>
                  </thead>

                  <tbody>
                    {valueBets.data.selections.map(
                      (row) => (
                        <tr key={row.selection}>
                          <td>
                            {selectionLabel(
                              row.selection
                            )}
                          </td>

                          <td>
                            {row.virtualBets ?? 0}
                          </td>

                          <td>
                            {row.winningBets ?? 0}
                          </td>

                          <td>
                            {row.losingBets ?? 0}
                          </td>

                          <td>
                            {formatDecimal(
                              row.profit
                            )}
                          </td>

                          <td>
                            {formatProbability(
                              row.roi
                            )}
                          </td>

                          <td>
                            {formatProbability(
                              row.winRate
                            )}
                          </td>

                          <td>
                            {formatProbability(
                              row.averageEdge
                            )}
                          </td>

                          <td>
                            {formatDecimal(
                              row.averageOdd
                            )}
                          </td>
                        </tr>
                      )
                    )}
                  </tbody>
                </table>
              </div>
            ) : (
              <div className="summary-box mt">
                Nu există încă pariuri virtuale pentru
                pragul selectat.
              </div>
            )}
          </>
        ) : null}
      </SectionCard>

      <SectionCard
        title="Recent evaluations"
        subtitle="Ultimele zece predicții evaluate."
      >
        <JsonBox data={recent.data} />
      </SectionCard>

      <SectionCard
        title="Fixture evaluation report"
        subtitle="Raportul individual pentru un meci evaluat."
      >
        <div className="inline-form">
          <input
            value={fixtureId}
            onChange={(event) =>
              setFixtureId(event.target.value)
            }
            placeholder="Fixture ID"
          />

          <button
            className="primary"
            onClick={loadFixtureReport}
          >
            Încarcă raportul
          </button>
        </div>

        {fixtureLoading ? (
          <LoadingBlock label="Loading fixture report" />
        ) : null}

        {fixtureNotice ? (
          <div className="summary-box">
            {fixtureNotice}
          </div>
        ) : null}

        {fixtureError ? (
          <ErrorBlock error={fixtureError} />
        ) : null}

        {fixtureReport ? (
          <JsonBox data={fixtureReport} />
        ) : null}
      </SectionCard>
    </div>
  );
}