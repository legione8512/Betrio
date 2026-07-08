import React from "react";

export function SectionCard({ title, subtitle, actions, children }) {
  return (
    <section className="card">
      <div className="card-header">
        <div>
          <h2>{title}</h2>
          {subtitle ? <p>{subtitle}</p> : null}
        </div>
        {actions ? <div className="card-actions">{actions}</div> : null}
      </div>
      <div className="card-body">{children}</div>
    </section>
  );
}

export function Metric({ label, value, hint }) {
  return (
    <div className="metric">
      <div className="metric-label">{label}</div>
      <div className="metric-value">{value}</div>
      {hint ? <div className="metric-hint">{hint}</div> : null}
    </div>
  );
}

export function Pill({ children, tone = "neutral" }) {
  return <span className={`pill pill-${tone}`}>{children}</span>;
}

export function JsonBox({ data }) {
  return <pre className="json-box">{JSON.stringify(data, null, 2)}</pre>;
}

export function LoadingBlock({ label = "Loading" }) {
  return <div className="loading-block">{label}...</div>;
}

export function ErrorBlock({ error }) {
  return <div className="error-block">{error}</div>;
}
