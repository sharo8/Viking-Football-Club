"use client";

import { useEffect, useState } from "react";
import { Button, Modal } from "@/ui/kit";
import { getData } from "@/services/api";
import { useLocale } from "@/components/providers/LocaleProvider";
import { activitySentence, roleLabel } from "@/lib/admin/activityText";

const COPY = {
  en: {
    title: "Audit log",
    subtitle: "Who did what, and when.",
    analysis: "Activity analysis",
    actor: "Person",
    what: "What happened",
    when: "When",
    search: "Search",
    today: "Today",
    week: "This week",
    month: "This month",
    year: "This year",
    all: "All",
    anyRole: "Any role",
    count: "Events",
    overTime: "Events over time",
    empty: "No activity yet.",
    close: "Close",
    details: "Details",
    role: "Role",
    before: "Before",
    after: "After",
    ip: "Network",
    lastLogin: "Last sign-in",
    hoursPeople: "Active time by person",
    hoursRoles: "Active time by role",
    actions: "Most common actions",
    note: "Active time counts only while the office is open and someone is using it. Idle time is left out. Older activity is not turned into hours.",
    neverTitle: "Never signed in",
    page: "Page",
    prev: "Previous",
    next: "Next",
    never: "No sign-in yet",
    events: "actions",
  },
  fr: {
    title: "Journal d'audit",
    subtitle: "Qui a fait quoi, et quand.",
    analysis: "Analyse d'activité",
    actor: "Personne",
    what: "Ce qui s'est passé",
    when: "Quand",
    search: "Rechercher",
    today: "Aujourd'hui",
    week: "Cette semaine",
    month: "Ce mois",
    year: "Cette année",
    all: "Tout",
    anyRole: "Tous les rôles",
    count: "Événements",
    overTime: "Événements dans le temps",
    empty: "Aucune activité.",
    close: "Fermer",
    details: "Détails",
    role: "Rôle",
    before: "Avant",
    after: "Après",
    ip: "Réseau",
    lastLogin: "Dernière connexion",
    hoursPeople: "Temps actif par personne",
    hoursRoles: "Temps actif par rôle",
    actions: "Actions les plus fréquentes",
    note: "Le temps actif compte seulement quand le bureau est ouvert et que quelqu'un s'en sert. Le temps d'inactivité est ignoré. L'activité plus ancienne n'est pas convertie en heures.",
    neverTitle: "Jamais connecté",
    page: "Page",
    prev: "Précédent",
    next: "Suivant",
    never: "Jamais connecté",
    events: "actions",
  },
  rw: {
    title: "Igitabo cy'ibikorwa",
    subtitle: "Ninde yakoze iki, n'igihe.",
    analysis: "Isesengura ry'ibikorwa",
    actor: "Umuntu",
    what: "Ibyabaye",
    when: "Igihe",
    search: "Shakisha",
    today: "Uyu munsi",
    week: "Iki cyumweru",
    month: "Uku kwezi",
    year: "Uyu mwaka",
    all: "Byose",
    anyRole: "Uruhare rwose",
    count: "Ibikorwa",
    overTime: "Ibikorwa mu gihe",
    empty: "Nta gikorwa.",
    close: "Funga",
    details: "Ibisobanuro",
    role: "Uruhare",
    before: "Mbere",
    after: "Nyuma",
    ip: "Umuyoboro",
    lastLogin: "Kwinjira bwa nyuma",
    hoursPeople: "Igihe cy'ibikorwa ku muntu",
    hoursRoles: "Igihe cy'ibikorwa ku ruhare",
    actions: "Ibikorwa bikunze kugaragara",
    note: "Igihe kibarwa gusa igihe ibiro bifunguye kandi umuntu akoresha sisitemu. Igihe cy'ikiruhuko ntikibarwa. Ibikorwa bya kera ntibihindurwa amasaha.",
    neverTitle: "Ntabwo yigeze yinjira",
    page: "Paji",
    prev: "Ibanze",
    next: "Ikurikira",
    never: "Ntabwo yigeze yinjira",
    events: "ibikorwa",
  },
};

function when(value) {
  return String(value || "").replace("T", " ").replace("Z", "").slice(0, 16);
}

function clock(seconds) {
  const total = Number(seconds) || 0;
  const h = Math.floor(total / 3600);
  const m = Math.floor((total % 3600) / 60);
  if (h) return `${h}h ${String(m).padStart(2, "0")}m`;
  if (m) return `${m}m`;
  return total ? `${total}s` : "0m";
}

function Bars({ rows, labelKey, valueKey }) {
  const max = Math.max(1, ...rows.map((row) => Number(row[valueKey]) || 0));
  return (
    <div className="audit-bars">
      {rows.map((row) => (
        <div key={row[labelKey]} className="audit-bar-row">
          <span>{String(row[labelKey] || "").replaceAll("_", " ")}</span>
          <i style={{ width: `${Math.max(6, (Number(row[valueKey]) / max) * 100)}%` }} />
          <b>{valueKey === "seconds" ? clock(row[valueKey]) : row[valueKey]}</b>
        </div>
      ))}
    </div>
  );
}

export function AuditOffice() {
  const { locale } = useLocale();
  const t = COPY[locale] || COPY.en;
  const [rows, setRows] = useState([]);
  const [page, setPage] = useState(0);
  const [pages, setPages] = useState(0);
  const [total, setTotal] = useState(0);
  const [query, setQuery] = useState("");
  const [preset, setPreset] = useState("all");
  const [role, setRole] = useState("");
  const [series, setSeries] = useState([]);
  const [error, setError] = useState("");
  const [open, setOpen] = useState(null);
  const [insights, setInsights] = useState(null);
  const [insightError, setInsightError] = useState("");

  useEffect(() => {
    const timer = window.setTimeout(() => {
      getData("/api/audit", { page, size: 25, q: query, preset: preset === "all" ? "" : preset, role }).then((body) => {
        setRows(body.content || []);
        setPages(body.totalPages || 0);
        setTotal(body.totalElements || 0);
        setError("");
      }).catch((err) => setError(err.message));
    }, 200);
    return () => window.clearTimeout(timer);
  }, [page, query, preset, role]);

  const openAnalysis = () => {
    setInsightError("");
    setInsights({ loading: true });
    getData("/api/audit/insights").then(setInsights).catch((err) => { setInsights(null); setInsightError(err.message); });
    getData("/api/audit", { page: 0, size: 100, preset: preset === "all" ? "" : preset, role }).then((body) => {
      const counts = new Map();
      (body.content || []).forEach((row) => {
        const day = String(row.at || "").slice(0, 10);
        if (day) counts.set(day, (counts.get(day) || 0) + 1);
      });
      setSeries([...counts.entries()].sort().map(([label, count]) => ({ label, count })));
    }).catch(() => setSeries([]));
  };

  return (
    <section className="crud-page">
      <header className="crud-head">
        <div>
          <h1>{t.title}</h1>
          <p>{t.subtitle}</p>
        </div>
        <div className="crud-tools">
          <input className="vfc-control" value={query} aria-label={t.search} placeholder={t.search} onChange={(event) => { setPage(0); setQuery(event.target.value); }} />
          <Button variant="primary" onClick={openAnalysis}>{t.analysis}</Button>
        </div>
      </header>
      <div className="crud-filters">
        {["all", "today", "week", "month", "year"].map((key) => (
          <Button key={key} variant={preset === key ? "primary" : "secondary"} onClick={() => { setPage(0); setPreset(key); }}>{t[key]}</Button>
        ))}
        <select className="vfc-control" aria-label={t.role} value={role} onChange={(event) => { setPage(0); setRole(event.target.value); }}>
          <option value="">{t.anyRole}</option>
          {["PRESIDENT", "GENERAL_MANAGER", "FINANCE_MANAGER", "ASSISTANT_MANAGER", "HEAD_COACH", "DOCTOR", "KIT_MANAGER"].map((code) => (
            <option key={code} value={code}>{roleLabel(locale, code)}</option>
          ))}
        </select>
      </div>
      <div className="crud-kpis">
        <article className="crud-kpi"><strong>{total}</strong><span>{t.count}</span></article>
      </div>
      {error ? <p className="crud-error">{error}</p> : null}
      {!error && !rows.length ? <p className="crud-empty">{t.empty}</p> : null}
      {rows.length ? (
        <div className="crud-scroll">
          <table className="crud-table">
            <thead><tr><th>{t.actor}</th><th>{t.role}</th><th>{t.what}</th><th>{t.when}</th></tr></thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.id}>
                  <td>{row.actorName || row.actor}</td>
                  <td>{roleLabel(locale, row.role) || "—"}</td>
                  <td><button type="button" className="audit-link" onClick={() => setOpen(row)}>{activitySentence(locale, row)}</button></td>
                  <td>{when(row.at)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
      {pages > 1 ? (
        <div className="audit-pager">
          <Button variant="secondary" disabled={page <= 0} onClick={() => setPage((value) => value - 1)}>{t.prev}</Button>
          <span>{t.page} {page + 1} / {pages} · {total}</span>
          <Button variant="secondary" disabled={page + 1 >= pages} onClick={() => setPage((value) => value + 1)}>{t.next}</Button>
        </div>
      ) : null}
      {open ? (
        <Modal title={t.details} subtitle={when(open.at)} onClose={() => setOpen(null)} closeLabel={t.close}>
          <div className="crud-grid">
            <article className="view-card"><small>{t.actor}</small><strong>{open.actorName || open.actor}</strong></article>
            <article className="view-card"><small>{t.role}</small><strong>{roleLabel(locale, open.role) || "—"}</strong></article>
            <article className="view-card span-2"><small>{t.what}</small><strong>{activitySentence(locale, open)}</strong></article>
            <article className="view-card"><small>{t.before}</small><strong>{open.before || "—"}</strong></article>
            <article className="view-card"><small>{t.after}</small><strong>{open.after || "—"}</strong></article>
            <article className="view-card"><small>{t.ip}</small><strong>{open.ip || "—"}</strong></article>
          </div>
        </Modal>
      ) : null}
      {insights ? (
        <Modal title={t.analysis} subtitle={t.note} size="lg" onClose={() => setInsights(null)} closeLabel={t.close}>
          {insights.loading ? <p>{t.search}…</p> : null}
          {insightError ? <p className="crud-error">{insightError}</p> : null}
          {insights.lastLogins ? (
            <div className="audit-board">
              <section className="span-2">
                <h3>{t.overTime}</h3>
                {series.length ? (
                  <div className="audit-columns" aria-label={t.overTime}>
                    {series.map((item) => (
                      <div key={item.label} className="audit-col" title={`${item.label}: ${item.count}`}>
                        <i style={{ height: `${Math.max(8, (item.count / Math.max(...series.map((row) => row.count))) * 100)}%` }} />
                        <small>{item.label.slice(5)}</small>
                      </div>
                    ))}
                  </div>
                ) : <p>{t.empty}</p>}
              </section>
              <section>
                <h3>{t.lastLogin}</h3>
                {(insights.lastLogins || []).map((row) => (
                  <p key={row.email}><strong>{row.name}</strong> <span>{roleLabel(locale, row.role)}</span> <small>{when(row.at)}</small></p>
                ))}
                {!insights.lastLogins?.length ? <p>{t.never}</p> : null}
              </section>
              <section>
                <h3>{t.neverTitle}</h3>
                {(insights.never || []).map((row) => (
                  <p key={row.email}><strong>{row.name}</strong> <span>{roleLabel(locale, row.role)}</span></p>
                ))}
                {!insights.never?.length ? <p>{t.never}</p> : null}
              </section>
              <section>
                <h3>{t.hoursPeople}</h3>
                {(insights.people || []).length ? <Bars rows={insights.people} labelKey="name" valueKey="seconds" /> : <p>{t.note}</p>}
              </section>
              <section>
                <h3>{t.hoursRoles}</h3>
                {(insights.roles || []).length ? <Bars rows={(insights.roles || []).map((row) => ({ ...row, label: roleLabel(locale, row.role) }))} labelKey="label" valueKey="seconds" /> : <p>{t.note}</p>}
              </section>
              <section>
                <h3>{t.actions}</h3>
                <Bars rows={(insights.actions || []).map((row) => ({ ...row, label: activitySentence(locale, { actor: "", action: row.action, after: "" }).replace(/^Someone /, "") }))} labelKey="label" valueKey="count" />
              </section>
            </div>
          ) : null}
        </Modal>
      ) : null}
    </section>
  );
}
