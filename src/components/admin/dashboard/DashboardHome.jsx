"use client";

import { useState } from "react";
import dynamic from "next/dynamic";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { motion, useReducedMotion } from "framer-motion";
import { CalendarPlus, Inbox } from "lucide-react";
import { useAuth } from "@/components/providers/AuthProvider";
import { useLocale } from "@/components/providers/LocaleProvider";
import { dashScope } from "@/lib/admin/access";
import { dashText } from "@/lib/admin/dashCopy";
import { activitySentence, roleLabel } from "@/lib/admin/activityText";
import { Modal } from "@/ui/kit";
import "@/crud/crud.css";
import { getData } from "@/services/api";
import { PagedCard } from "./PagedCard";
import { Countdown, KpiCard, Leaderboard, Mark, MatchDrawer } from "./parts";
import "./dashboard.css";

const ChartBoard = dynamic(() => import("./charts"), {
  ssr: false,
  loading: () => <div className="dash-skel dash-skel--block" role="status" />,
});

const PERIODS = [
  ["season", "season"],
  ["30d", "last30"],
  ["5m", "last5"],
];

const KPI = [
  { key: "players", href: "/admin/players", format: "num", when: "people" },
  { key: "staff", href: "/admin/staff", format: "num", when: "people" },
  { key: "played", href: "/admin/matches", format: "num", when: "sport" },
  { key: "goalsFor", href: "/admin/matches", format: "num", when: "sport" },
  { key: "goalsAgainst", href: "/admin/matches", format: "num", when: "sport" },
  { key: "winRate", href: "/admin/matches", format: "pct", when: "sport" },
  { key: "attendanceRate", href: "/admin/attendance", format: "pct", when: "attendance" },
  { key: "injured", href: "/admin/medical", format: "num", when: "injuries" },
  { key: "payroll", href: "/admin/finance", format: "rwf", when: "finance" },
  { key: "paidPct", href: "/admin/finance", format: "pct", when: "finance" },
  { key: "balance", href: "/admin/finance", format: "rwf", when: "finance" },
  { key: "fines", href: "/admin/fines", format: "rwf", when: "fines" },
];

function EmptyNote({ text, href, action }) {
  return (
    <div className="dash-empty">
      <Inbox size={22} aria-hidden="true" />
      <p>{text}</p>
      {href ? <Link href={href}>{action}</Link> : null}
    </div>
  );
}

export function DashboardHome() {
  const { user } = useAuth();
  const { locale } = useLocale();
  const copy = dashText(locale);
  const text = (key) => copy[key] || key;
  const scope = dashScope(user?.role || "");
  const reduced = useReducedMotion();
  const [period, setPeriod] = useState("season");
  const [open, setOpen] = useState(null);
  const [detail, setDetail] = useState(null);
  const summary = useQuery({
    queryKey: ["dashboard", "summary", period],
    queryFn: () => getData("/api/dashboard/summary", { period }),
  });
  const next = useQuery({
    queryKey: ["dashboard", "next-match"],
    queryFn: () => getData("/api/dashboard/next-match"),
    enabled: scope.next,
  });
  const cards = KPI.filter((card) => scope[card.when]);
  const tabs = [{ id: "season", label: copy.season }, { id: "last5", label: copy.last5 }];
  const fade = reduced ? { opacity: 1 } : { opacity: 0, y: 12 };

  return (
    <section className="dash-page">
      <header className="dash-head">
        <div>
          <h1>{copy.dashboard}</h1>
          <p>{copy.subtitle}</p>
        </div>
        <div className="dash-periods" role="tablist" aria-label={copy.season}>
          {PERIODS.map(([id, label]) => (
            <button key={id} type="button" className={period === id ? "is-on" : ""} aria-pressed={period === id} onClick={() => setPeriod(id)}>
              {copy[label]}
            </button>
          ))}
        </div>
      </header>

      <div className="dash-kpis">
        {summary.isLoading ? cards.map((card) => <div key={card.key} className="dash-skel" />) : null}
        {summary.isError ? <button type="button" className="dash-retry" onClick={() => summary.refetch()}>{copy.retry}: {summary.error.message}</button> : null}
        {summary.isSuccess ? cards.map((card, index) => (
          summary.data[card.key] == null ? null : (
            <motion.div key={card.key} initial={fade} animate={{ opacity: 1, y: 0 }} transition={{ delay: reduced ? 0 : index * 0.04 }}>
              <KpiCard
                label={copy[card.key === "attendanceRate" ? "attendance" : card.key]}
                value={summary.data[card.key]}
                format={card.format}
                trend={summary.data.trends?.[card.key]}
                spark={summary.data.sparks?.[card.key]}
                href={card.href}
                locale={locale}
              />
            </motion.div>
          )
        )) : null}
      </div>

      {scope.next ? (
        <section className="dash-hero dash-card" aria-label={copy.nextMatch}>
          {next.isLoading ? <div className="dash-skel" role="status" aria-label={copy.loading} /> : null}
          {next.isError ? <button type="button" className="dash-retry" onClick={() => next.refetch()}>{copy.retry}: {next.error.message}</button> : null}
          {next.isSuccess && next.data ? (
            <>
              <div className="dash-hero__top">
                <h2>{copy.nextMatch}</h2>
                <Countdown iso={next.data.kickoff} labels={{ days: "D", hours: "H", mins: "M", secs: "S" }} />
              </div>
              <div className="dash-hero__clubs">
                <Mark name="Viking FC" logo="/logo/viking-fc.png" />
                <div className="dash-hero__names">
                  <strong>VIKING FC</strong>
                  <span>{copy.vs}</span>
                  <strong>{next.data.opponent}</strong>
                </div>
                <Mark name={next.data.opponent} logo={next.data.logoUrl} />
              </div>
              <p className="dash-hero__meta">
                <span>{next.data.date} · {next.data.time}</span>
                <span>{next.data.venue}</span>
                <span>{next.data.home ? copy.home : copy.away}</span>
                <span>{next.data.competition}</span>
                <span>{copy.lineupStatus}: {next.data.lineup === "published" ? copy.published : copy.unpublished}</span>
              </p>
              <div className="dash-actions">
                <Link href="/admin/lineup">{copy.buildLineup}</Link>
                <Link className="ghost" href="/admin/matches">{copy.viewMatch}</Link>
              </div>
            </>
          ) : null}
          {next.isSuccess && !next.data ? (
            <>
              <h2>{copy.nextMatch}</h2>
              <EmptyNote text={copy.noMatch} href="/admin/matches" action={<><CalendarPlus size={16} /> {copy.schedule}</>} />
            </>
          ) : null}
        </section>
      ) : null}

      <div className="dash-row">
        {scope.sport ? (
          <PagedCard
            title={copy.recent}
            pageSize={5}
            queryKey={["dashboard", "recent-matches", period]}
            resetKey={period}
            fetcher={({ page, size }) => getData("/api/dashboard/recent-matches", { period, page, size })}
            empty={<EmptyNote text={copy.noMatches} href="/admin/matches" action={copy.schedule} />}
            renderItem={(match) => (
              <button key={match.id} type="button" className="dash-match" onClick={() => setOpen(match)} aria-label={`${match.result} ${match.scoreFor}-${match.scoreAgainst} ${match.opponent}`}>
                <span className={`dash-badge ${match.result}`}>{match.result}</span>
                <Mark name={match.opponent} logo={match.logoUrl} />
                <span className="grow">
                  <strong>{match.scoreFor} – {match.scoreAgainst} {copy.vs} {match.opponent}</strong>
                  <div className="meta">{match.date} · {(match.scorers || []).join(", ") || "—"}</div>
                </span>
              </button>
            )}
          />
        ) : null}
        {scope.sport ? (
          <PagedCard
            title={copy.scorers}
            tabs={tabs}
            pageSize={5}
            queryKey={["dashboard", "top-scorers"]}
            resetKey={period}
            fetcher={({ page, size, scope: board }) => getData("/api/dashboard/top-scorers", { scope: board, page, size })}
            empty={<EmptyNote text={copy.empty} />}
            renderItem={(row) => <Leaderboard key={row.id} row={row} metric="goals" />}
          />
        ) : null}
        {scope.sport ? (
          <PagedCard
            title={copy.assists}
            tabs={tabs}
            pageSize={5}
            queryKey={["dashboard", "top-assists"]}
            resetKey={period}
            fetcher={({ page, size, scope: board }) => getData("/api/dashboard/top-assists", { scope: board, page, size })}
            empty={<EmptyNote text={copy.empty} />}
            renderItem={(row) => <Leaderboard key={row.id} row={row} metric="assists" />}
          />
        ) : null}
      </div>

      <div className="dash-row">
        {scope.sport ? (
          <PagedCard
            title={copy.goalsFeed}
            pageSize={5}
            queryKey={["dashboard", "recent-goals", period]}
            resetKey={period}
            fetcher={({ page, size }) => getData("/api/dashboard/recent-goals", { period, page, size })}
            empty={<EmptyNote text={copy.empty} />}
            renderItem={(item) => (
              <p key={item.id} className="dash-line">{item.minute}&apos; {copy.goal} – {item.scorer}{item.assist ? ` (${copy.assist}: ${item.assist})` : ""} {copy.vs} {item.opponent}</p>
            )}
          />
        ) : null}
        <PagedCard
          title={copy.activity}
          pageSize={5}
          queryKey={["dashboard", "activity"]}
          resetKey="activity"
          fetcher={({ page, size }) => getData("/api/dashboard/activity", { page, size })}
          empty={<EmptyNote text={copy.empty} />}
          renderItem={(item) => (
            <button key={item.id} type="button" className="dash-line" onClick={() => setDetail({ kind: "activity", item })}>
              <strong>{activitySentence(locale, item)}</strong>
              <span className="meta">{String(item.at || "").replace("T", " ").slice(0, 16)}</span>
            </button>
          )}
        />
        <PagedCard
          title={copy.alerts}
          pageSize={5}
          queryKey={["dashboard", "alerts", user?.role]}
          resetKey={user?.role || ""}
          fetcher={async ({ page, size }) => {
            const rows = await getData("/api/dashboard/alerts");
            const start = page * size;
            const content = rows.slice(start, start + size);
            return { content, page, size, totalElements: rows.length, totalPages: Math.ceil(rows.length / size) || 0 };
          }}
          empty={<EmptyNote text={copy.empty} />}
          renderItem={(item) => (
            <button key={item.id} type="button" className="dash-line" onClick={() => setDetail({ kind: "alert", item })}>
              <span className={`dash-badge ${item.tone}`}>{text(item.key)}</span> {item.name} {item.detail}
            </button>
          )}
        />
      </div>

      <ChartBoard period={period} scope={scope} t={text} locale={locale} />
      {open ? <MatchDrawer match={open} onClose={() => setOpen(null)} text={text} /> : null}
      {detail ? (
        <Modal title={detail.kind === "alert" ? text(detail.item.key) : copy.activity} subtitle={String((detail.item.at || "")).replace("T", " ").slice(0, 16)} onClose={() => setDetail(null)} closeLabel={copy.close || "Close"}>
          {detail.kind === "alert" ? (
            <div className="crud-grid">
              <article className="view-card span-2"><small>{text(detail.item.key)}</small><strong>{detail.item.name} {detail.item.detail}</strong></article>
              <p className="span-2">{detail.item.key === "alertJersey" ? text("alertJersey") : text(detail.item.key)}. {detail.item.name}. {detail.item.detail}</p>
            </div>
          ) : (
            <div className="crud-grid">
              <article className="view-card span-2"><small>{copy.activity}</small><strong>{activitySentence(locale, detail.item)}</strong></article>
              <article className="view-card"><small>{copy.role || "Role"}</small><strong>{roleLabel(locale, detail.item.role) || "—"}</strong></article>
              <article className="view-card"><small>{copy.email || "Email"}</small><strong>{detail.item.actorName || detail.item.actor || "—"}</strong></article>
              <article className="view-card"><small>Before</small><strong>{detail.item.before || "—"}</strong></article>
              <article className="view-card"><small>After</small><strong>{detail.item.after || detail.item.text || "—"}</strong></article>
            </div>
          )}
        </Modal>
      ) : null}
    </section>
  );
}
