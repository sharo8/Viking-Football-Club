"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { motion, useReducedMotion } from "framer-motion";

export function initials(name = "") {
  return name.split(" ").filter(Boolean).slice(0, 2).map((part) => part[0].toUpperCase()).join("");
}

export function money(value, locale) {
  const loc = locale === "fr" ? "fr-FR" : "en-RW";
  return new Intl.NumberFormat(loc, { style: "currency", currency: "RWF", currencyDisplay: "code", maximumFractionDigits: 0 }).format(value || 0);
}

function useCount(to, reduced) {
  const [value, setValue] = useState(reduced ? to : 0);
  useEffect(() => {
    if (reduced) {
      setValue(to);
      return undefined;
    }
    let frame = 0;
    const start = performance.now();
    const step = (now) => {
      const t = Math.min(1, (now - start) / 900);
      setValue(to * (1 - (1 - t) ** 3));
      if (t < 1) frame = requestAnimationFrame(step);
    };
    frame = requestAnimationFrame(step);
    return () => cancelAnimationFrame(frame);
  }, [to, reduced]);
  return value;
}

function Spark({ points }) {
  const data = points && points.length ? points : [0, 0];
  const max = Math.max(...data, 1);
  const min = Math.min(...data, 0);
  const span = max - min || 1;
  const coords = data.map((value, index) => {
    const x = (index / Math.max(1, data.length - 1)) * 80;
    const y = 22 - ((value - min) / span) * 18;
    return `${x},${y}`;
  }).join(" ");
  return (
    <svg className="admin-spark" viewBox="0 0 80 24" width="80" height="24" aria-hidden="true">
      <polyline points={coords} />
    </svg>
  );
}

export function KpiCard({ label, value, format, trend, spark, href, locale }) {
  const reduced = useReducedMotion();
  const shown = useCount(Number(value) || 0, reduced);
  const text = format === "pct"
    ? `${shown.toFixed(1)}%`
    : format === "rwf"
      ? money(Math.round(shown), locale)
      : Math.round(shown).toLocaleString(locale === "fr" ? "fr-FR" : "en");
  const known = typeof trend === "number" && !Number.isNaN(trend);
  const dir = known && trend > 0 ? "is-up" : known && trend < 0 ? "is-down" : "";
  return (
    <Link href={href} className="dash-card dash-kpi">
      <span>{label}</span>
      <b>{text}</b>
      {spark?.length > 1 ? <Spark points={spark} /> : null}
      {known ? <em className={dir}>{trend > 0 ? "▲" : trend < 0 ? "▼" : "•"} {Math.abs(trend)}%</em> : null}
    </Link>
  );
}

export function ChartCard({ title, status, error, onRetry, empty, retryLabel, children }) {
  return (
    <section className="dash-card" aria-label={title}>
      <h2>{title}</h2>
      {status === "loading" ? <div className="dash-skel" role="status" aria-label="loading" /> : null}
      {status === "error" ? <button type="button" className="dash-retry" onClick={onRetry}>{retryLabel}: {error}</button> : null}
      {status === "ready" && empty ? <p className="dash-empty">{empty}</p> : null}
      {status === "ready" && !empty ? children : null}
    </section>
  );
}

export function Mark({ name, logo }) {
  if (logo) return <span className="mark"><img src={logo} alt="" /></span>;
  return <span className="mark" aria-hidden="true">{initials(name)}</span>;
}

export function Countdown({ iso, labels }) {
  const reduced = useReducedMotion();
  const [now, setNow] = useState(Date.now());
  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(id);
  }, []);
  const diff = Math.max(0, new Date(iso).getTime() - now);
  const parts = [
    [labels.days, Math.floor(diff / 86400000)],
    [labels.hours, Math.floor(diff / 3600000) % 24],
    [labels.mins, Math.floor(diff / 60000) % 60],
    [labels.secs, Math.floor(diff / 1000) % 60],
  ];
  return (
    <div className="dash-count" aria-label={parts.map(([label, value]) => `${value} ${label}`).join(", ")}>
      {parts.map(([label, value]) => (
        <b key={label}>
          <motion.span key={value} initial={reduced ? false : { rotateX: -80, opacity: 0 }} animate={{ rotateX: 0, opacity: 1 }} transition={{ duration: 0.35 }}>
            {String(value).padStart(2, "0")}
          </motion.span>
          <small>{label}</small>
        </b>
      ))}
    </div>
  );
}

export function Leaderboard({ row, metric }) {
  const reduced = useReducedMotion();
  const medals = ["🥇", "🥈", "🥉"];
  const rank = row.rank || 1;
  const max = row.leader || row[metric] || 1;
  return (
    <div className="dash-row">
      <span aria-hidden="true" style={{ width: 22 }}>{rank <= 3 ? medals[rank - 1] : rank}</span>
      <Mark name={row.name} logo={row.photoUrl} />
      <div className="grow">
        <strong>#{row.jersey} {row.name}</strong>
        <div className="meta">{row.position}</div>
        <div className="dash-bar" aria-hidden="true">
          <motion.span initial={reduced ? false : { scaleX: 0 }} animate={{ scaleX: 1 }} transition={{ duration: 0.8 }} style={{ width: `${(row[metric] / max) * 100}%` }} />
        </div>
      </div>
      <b>{row[metric]}</b>
    </div>
  );
}

export function ActivityFeed({ items, text }) {
  return (
    <ul className="dash-feed">
      {items.map((item) => (
        <li key={item.id}>
          <strong>{text(item.textKey)}</strong>
          <div style={{ color: "var(--text-muted)", fontSize: 12 }}>{item.at.replace("T", " ").slice(0, 16)}</div>
        </li>
      ))}
    </ul>
  );
}

export function AlertList({ items, text }) {
  if (!items.length) return null;
  return (
    <ul className="dash-list" style={{ listStyle: "none", margin: 0, padding: 0 }}>
      {items.map((item) => (
        <li key={item.id}>
          <span className={`dash-badge ${item.tone}`}>{text(item.key)}</span>
          <div>{item.name} {item.detail}</div>
        </li>
      ))}
    </ul>
  );
}

export function MatchDrawer({ match, onClose, text }) {
  useEffect(() => {
    const onKey = (event) => { if (event.key === "Escape") onClose(); };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);
  const events = [...(match.events || [])].sort((a, b) => a.minute - b.minute);
  return (
    <div className="dash-drawer" onClick={onClose}>
      <article role="dialog" aria-modal="true" aria-label={`${match.opponent}`} onClick={(event) => event.stopPropagation()}>
        <button type="button" className="dash-retry" onClick={onClose} aria-label="Close">×</button>
        <h2 style={{ marginTop: 12 }}>{text("vs")} {match.opponent}</h2>
        <p>{match.date} · {match.time} · {match.venue}</p>
        <p><span className={`dash-badge ${match.result}`}>{match.result}</span> {match.scoreFor} – {match.scoreAgainst}</p>
        <ol>
          {events.map((event) => (
            <li key={`${event.minute}-${event.type}-${event.player}`}>
              {event.minute}&apos; {event.type === "YELLOW_CARD" || event.type === "RED_CARD"
                ? `${event.type === "RED_CARD" ? "red" : "yellow"} – ${event.player}`
                : event.type === "SUB_IN" || event.type === "SUB_OUT"
                  ? `${text("sub")} – ${event.player}`
                  : event.type === "GOAL" || event.type === "PENALTY_GOAL"
                    ? `${text("goal")} – ${event.player}${event.related ? ` (${text("assist")}: ${event.related})` : ""}`
                    : `${event.type} – ${event.player || ""}`}
            </li>
          ))}
        </ol>
      </article>
    </div>
  );
}
