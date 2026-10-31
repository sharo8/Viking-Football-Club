"use client";

import { useMemo, useRef } from "react";
import { useQuery } from "@tanstack/react-query";
import { useReducedMotion } from "framer-motion";
import { Info } from "lucide-react";
import { useTheme } from "@/components/providers/ThemeProvider";
import { getData } from "@/services/api";
import { money } from "./parts";
import {
  ResponsiveContainer, BarChart, Bar, XAxis, YAxis, Tooltip, Legend, PieChart, Pie, Cell,
  AreaChart, Area, Line, ComposedChart, ReferenceLine,
} from "recharts";

function useColors() {
  const { theme } = useTheme();
  return useMemo(() => {
    const read = (name, fallback) => {
      if (typeof document === "undefined") return fallback;
      return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || fallback;
    };
    const dark = theme === "dark";
    return {
      red: dark ? read("--red-light", "#e0707f") : read("--red", "#a3283a"),
      gray: dark ? "#8d8d8d" : "#6b6b6b",
      green: dark ? "#7dcea0" : "#157347",
      amber: dark ? "#e0b15a" : "#9a6700",
      text: read("--text", dark ? "#fff" : "#000"),
      muted: read("--text-muted", "#888"),
      card: read("--card", dark ? "#0a0a0a" : "#fff"),
      border: read("--border", "#e4e4e4"),
      grid: dark ? "#262626" : "#ececec",
    };
  }, [theme]);
}

function tip(colors) {
  return { background: colors.card, border: `1px solid ${colors.border}`, borderRadius: 10, color: colors.text };
}

function download(filename, text, type) {
  const blob = new Blob([text], { type });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}

function toCsv(rows) {
  if (!rows?.length) return "";
  const keys = Object.keys(rows[0]);
  const esc = (value) => `"${String(value ?? "").replaceAll('"', '""')}"`;
  return [keys.join(","), ...rows.map((row) => keys.map((key) => esc(row[key])).join(","))].join("\n");
}

function Tile({ title, hint, name, period, enabled, t, children, isEmpty }) {
  const reduced = useReducedMotion();
  const ref = useRef(null);
  const query = useQuery({
    queryKey: ["dashboard", "chart", name, period],
    queryFn: () => getData(`/api/dashboard/charts/${name}`, { period }),
    enabled,
  });
  const empty = query.isSuccess && isEmpty(query.data);
  const rows = Array.isArray(query.data) ? query.data : query.data ? [query.data] : [];
  const png = () => {
    const svg = ref.current?.querySelector("svg");
    if (!svg) return;
    const xml = new XMLSerializer().serializeToString(svg);
    const image = new Image();
    const url = URL.createObjectURL(new Blob([xml], { type: "image/svg+xml" }));
    image.onload = () => {
      const canvas = document.createElement("canvas");
      canvas.width = svg.clientWidth * 2 || 640;
      canvas.height = svg.clientHeight * 2 || 360;
      const ctx = canvas.getContext("2d");
      ctx.fillStyle = getComputedStyle(document.documentElement).getPropertyValue("--card") || "#fff";
      ctx.fillRect(0, 0, canvas.width, canvas.height);
      ctx.drawImage(image, 0, 0, canvas.width, canvas.height);
      URL.revokeObjectURL(url);
      const link = document.createElement("a");
      link.href = canvas.toDataURL("image/png");
      link.download = `${name}.png`;
      link.click();
    };
    image.src = url;
  };
  return (
    <section className="dash-card chart-tile" aria-label={title}>
      <header className="chart-tile__head">
        <h2>{title}</h2>
        <span className="chart-tools">
          <button type="button" className="icon-btn" aria-label={hint} title={hint}><Info size={16} /></button>
          <button type="button" className="dash-retry" onClick={() => download(`${name}.csv`, toCsv(rows), "text/csv")} disabled={!rows.length}>CSV</button>
          <button type="button" className="dash-retry" onClick={png} disabled={empty || !query.isSuccess}>PNG</button>
        </span>
      </header>
      <div ref={ref} className="chart-tile__body">
        {query.isLoading ? <div className="dash-skel" role="status" aria-label={t("loading")} /> : null}
        {query.isError ? <button type="button" className="dash-retry" onClick={() => query.refetch()}>{t("retry")}: {query.error.message}</button> : null}
        {empty ? <p className="dash-empty">{t("empty")}</p> : null}
        {query.isSuccess && !empty ? children(query.data, { reduced }) : null}
      </div>
    </section>
  );
}

function Donut({ data, colors, center, palette, reduced }) {
  return (
    <div className="dash-donut" role="img">
      <ResponsiveContainer width="100%" height="100%">
        <PieChart>
          <Pie data={data} dataKey="value" nameKey="name" innerRadius={58} outerRadius={82} paddingAngle={2} isAnimationActive={!reduced} animationDuration={1000}>
            {data.map((entry, index) => <Cell key={entry.name} fill={palette[index % palette.length]} />)}
          </Pie>
          <Tooltip contentStyle={tip(colors)} />
          <Legend />
        </PieChart>
      </ResponsiveContainer>
      {center ? <div className="dash-donut__center">{center}</div> : null}
    </div>
  );
}

export default function ChartBoard({ period, scope, t, locale }) {
  const colors = useColors();
  const palette = [colors.red, colors.gray, colors.green, colors.amber, "#7d1c2b"];
  const label = (name) => t(name) || name;
  const listEmpty = (data) => !Array.isArray(data) || data.length === 0;

  return (
    <div className="dash-grid">
      {scope.analysis ? (
        <Tile title={t("goalsChart")} hint={t("hintGoals")} name="goalsPerMatch" period={period} enabled={scope.analysis} t={t} isEmpty={listEmpty}>
          {(data, { reduced }) => (
            <div className="dash-chart" role="img" aria-label={t("goalsChart")}>
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={data}>
                  <XAxis dataKey="name" tick={{ fill: colors.muted, fontSize: 11 }} interval={0} angle={-28} textAnchor="end" height={54} axisLine={false} tickLine={false} />
                  <YAxis tick={{ fill: colors.muted, fontSize: 11 }} axisLine={false} tickLine={false} width={28} />
                  <Tooltip contentStyle={tip(colors)} />
                  <Legend />
                  <Bar dataKey="scored" name={t("scored")} fill={colors.red} radius={[6, 6, 0, 0]} isAnimationActive={!reduced} animationDuration={1000} />
                  <Bar dataKey="conceded" name={t("conceded")} fill={colors.gray} radius={[6, 6, 0, 0]} isAnimationActive={!reduced} animationDuration={1000} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </Tile>
      ) : null}
      {scope.analysis ? (
        <Tile title={t("results")} hint={t("hintResults")} name="results" period={period} enabled={scope.analysis} t={t} isEmpty={listEmpty}>
          {(data, { reduced }) => {
            const total = data.reduce((sum, row) => sum + Number(row.value), 0);
            const wins = data.find((row) => row.name === "wins")?.value || 0;
            const pct = total ? Math.round((wins / total) * 1000) / 10 : 0;
            return <Donut data={data.map((row) => ({ ...row, name: label(row.name) }))} colors={colors} palette={[colors.green, colors.gray, colors.red]} center={`${pct}%`} reduced={reduced} />;
          }}
        </Tile>
      ) : null}
      {scope.people ? (
        <Tile title={t("squad")} hint={t("hintSquad")} name="squad" period={period} enabled={scope.people} t={t} isEmpty={listEmpty}>
          {(data, { reduced }) => <Donut data={data.map((row) => ({ ...row, name: label(row.name) }))} colors={colors} palette={palette} center={String(data.reduce((sum, row) => sum + Number(row.value), 0))} reduced={reduced} />}
        </Tile>
      ) : null}
      {scope.analysis ? (
        <Tile title={t("minuteGoals")} hint={t("hintMinutes")} name="minuteGoals" period={period} enabled={scope.analysis} t={t} isEmpty={listEmpty}>
          {(data, { reduced }) => (
            <div className="dash-chart" role="img" aria-label={t("minuteGoals")}>
              <ResponsiveContainer width="100%" height="100%"><BarChart data={data}><XAxis dataKey="name" tick={{ fill: colors.muted, fontSize: 11 }} axisLine={false} tickLine={false} /><YAxis tick={{ fill: colors.muted, fontSize: 11 }} axisLine={false} tickLine={false} width={28} /><Tooltip contentStyle={tip(colors)} /><Bar dataKey="value" name={t("goalsFor")} fill={colors.red} radius={[6, 6, 0, 0]} isAnimationActive={!reduced} animationDuration={1000} /></BarChart></ResponsiveContainer>
            </div>
          )}
        </Tile>
      ) : null}
      {scope.people ? (
        <Tile title={t("ages")} hint={t("hintAges")} name="ages" period={period} enabled={scope.people} t={t} isEmpty={listEmpty}>
          {(data, { reduced }) => (
            <div className="dash-chart" role="img" aria-label={t("ages")}>
              <ResponsiveContainer width="100%" height="100%"><BarChart data={data}><XAxis dataKey="name" tick={{ fill: colors.muted, fontSize: 11 }} axisLine={false} tickLine={false} /><YAxis tick={{ fill: colors.muted, fontSize: 11 }} axisLine={false} tickLine={false} width={28} /><Tooltip contentStyle={tip(colors)} /><Bar dataKey="value" name={t("players")} fill={colors.red} radius={[6, 6, 0, 0]} isAnimationActive={!reduced} animationDuration={1000} /></BarChart></ResponsiveContainer>
            </div>
          )}
        </Tile>
      ) : null}
      {scope.attendance ? (
        <Tile title={t("attendanceTrend")} hint={t("hintAttendance")} name="attendance" period={period} enabled={scope.attendance} t={t} isEmpty={listEmpty}>
          {(data, { reduced }) => (
            <div className="dash-chart" role="img" aria-label={t("attendanceTrend")}>
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={data}>
                  <XAxis dataKey="name" tick={{ fill: colors.muted, fontSize: 11 }} axisLine={false} tickLine={false} />
                  <YAxis domain={[0, 100]} tick={{ fill: colors.muted, fontSize: 11 }} axisLine={false} tickLine={false} width={32} />
                  <Tooltip contentStyle={tip(colors)} />
                  <ReferenceLine y={80} stroke={colors.green} strokeDasharray="4 4" />
                  <ReferenceLine y={60} stroke={colors.amber} strokeDasharray="4 4" />
                  <Area type="monotone" dataKey="rate" name={t("rate")} stroke={colors.red} fill={colors.red} fillOpacity={0.18} isAnimationActive={!reduced} animationDuration={1000} />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          )}
        </Tile>
      ) : null}
      {scope.attendance ? (
        <Tile title={t("attendanceStatus")} hint={t("hintStatus")} name="attendanceStatus" period={period} enabled={scope.attendance} t={t} isEmpty={listEmpty}>
          {(data, { reduced }) => <Donut data={data.map((row) => ({ ...row, name: label(row.name) }))} colors={colors} palette={[colors.green, colors.amber, colors.red]} reduced={reduced} />}
        </Tile>
      ) : null}
      {scope.analysis ? (
        <Tile title={t("contribution")} hint={t("hintContribution")} name="contribution" period={period} enabled={scope.analysis} t={t} isEmpty={listEmpty}>
          {(data, { reduced }) => (
            <div className="dash-chart" role="img" aria-label={t("contribution")}>
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={data} layout="vertical" margin={{ left: 8 }}>
                  <XAxis type="number" tick={{ fill: colors.muted, fontSize: 11 }} axisLine={false} tickLine={false} />
                  <YAxis type="category" dataKey="name" width={110} tick={{ fill: colors.muted, fontSize: 11 }} axisLine={false} tickLine={false} />
                  <Tooltip contentStyle={tip(colors)} /><Legend />
                  <Bar dataKey="goals" name={t("goalsFor")} stackId="c" fill={colors.red} isAnimationActive={!reduced} animationDuration={1000} />
                  <Bar dataKey="assists" name={t("assists")} stackId="c" fill={colors.gray} isAnimationActive={!reduced} animationDuration={1000} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </Tile>
      ) : null}
      {scope.analysis ? (
        <Tile title={t("minutes")} hint={t("hintPlayed")} name="minutes" period={period} enabled={scope.analysis} t={t} isEmpty={listEmpty}>
          {(data, { reduced }) => (
            <div className="dash-chart" role="img" aria-label={t("minutes")}>
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={data} layout="vertical"><XAxis type="number" tick={{ fill: colors.muted, fontSize: 11 }} axisLine={false} tickLine={false} /><YAxis type="category" dataKey="name" width={120} tick={{ fill: colors.muted, fontSize: 11 }} axisLine={false} tickLine={false} /><Tooltip contentStyle={tip(colors)} /><Bar dataKey="minutes" name={t("minutes")} fill={colors.red} isAnimationActive={!reduced} animationDuration={1000} /></BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </Tile>
      ) : null}
      {scope.analysis ? (
        <Tile title={t("least")} hint={t("hintLeast")} name="leastUsed" period={period} enabled={scope.analysis} t={t} isEmpty={listEmpty}>
          {(data) => <ul className="dash-least">{data.map((player) => <li key={player.id}>#{player.jersey} {player.name} — {player.minutes}</li>)}</ul>}
        </Tile>
      ) : null}
      {scope.injuries ? (
        <Tile title={t("injuries")} hint={t("hintInjuries")} name="injuryParts" period={period} enabled={scope.injuries} t={t} isEmpty={listEmpty}>
          {(data, { reduced }) => <Donut data={data.map((row) => ({ ...row, name: label(row.name) }))} colors={colors} palette={palette} center={String(data.reduce((sum, row) => sum + Number(row.value), 0))} reduced={reduced} />}
        </Tile>
      ) : null}
      {scope.injuries ? (
        <Tile title={t("returnOn")} hint={t("hintReturn")} name="injured" period={period} enabled={scope.injuries} t={t} isEmpty={listEmpty}>
          {(data) => <ul className="dash-least">{data.map((row) => <li key={row.playerId || row.id}>{row.name} · {row.bodyPart || ""} · {row.expectedReturn || "—"}</li>)}</ul>}
        </Tile>
      ) : null}
      {scope.fines ? (
        <Tile title={t("finesChart")} hint={t("hintFines")} name="fines" period={period} enabled={scope.fines} t={t} isEmpty={listEmpty}>
          {(data, { reduced }) => (
            <div className="dash-chart" role="img" aria-label={t("finesChart")}>
              <ResponsiveContainer width="100%" height="100%"><BarChart data={data}><XAxis dataKey="name" tick={{ fill: colors.muted, fontSize: 11 }} axisLine={false} tickLine={false} /><YAxis tick={{ fill: colors.muted, fontSize: 11 }} axisLine={false} tickLine={false} width={36} /><Tooltip contentStyle={tip(colors)} /><Bar dataKey="amount" name={t("amount")} fill={colors.red} radius={[6, 6, 0, 0]} isAnimationActive={!reduced} animationDuration={1000} /></BarChart></ResponsiveContainer>
            </div>
          )}
        </Tile>
      ) : null}
      {scope.finance ? (
        <Tile title={t("payrollChart")} hint={t("hintPayroll")} name="payroll" period={period} enabled={scope.finance} t={t} isEmpty={listEmpty}>
          {(data, { reduced }) => <Donut data={data.map((row) => ({ ...row, name: label(row.name) }))} colors={colors} palette={palette} center={money(data.reduce((sum, row) => sum + Number(row.value), 0), locale)} reduced={reduced} />}
        </Tile>
      ) : null}
      {scope.finance ? (
        <Tile title={t("bands")} hint={t("hintBands")} name="salaryBands" period={period} enabled={scope.finance} t={t} isEmpty={listEmpty}>
          {(data, { reduced }) => (
            <div className="dash-chart" role="img" aria-label={t("bands")}><ResponsiveContainer width="100%" height="100%"><BarChart data={data}><XAxis dataKey="name" tick={{ fill: colors.muted, fontSize: 11 }} axisLine={false} tickLine={false} /><YAxis tick={{ fill: colors.muted, fontSize: 11 }} axisLine={false} tickLine={false} width={28} /><Tooltip contentStyle={tip(colors)} /><Bar dataKey="value" name={t("players")} fill={colors.red} isAnimationActive={!reduced} animationDuration={1000} /></BarChart></ResponsiveContainer></div>
          )}
        </Tile>
      ) : null}
      {scope.finance ? (
        <Tile title={t("payment")} hint={t("hintPayment")} name="payment" period={period} enabled={scope.finance} t={t} isEmpty={(data) => !data}>
          {(data, { reduced }) => <Donut data={[{ name: t("paid"), value: Number(data.paid) }, { name: t("balance"), value: Number(data.balance) }]} colors={colors} palette={[colors.green, colors.red]} center={`${data.pct}%`} reduced={reduced} />}
        </Tile>
      ) : null}
      {scope.finance ? (
        <Tile title={t("cashout")} hint={t("hintCash")} name="cashout" period={period} enabled={scope.finance} t={t} isEmpty={listEmpty}>
          {(data, { reduced }) => (
            <div className="dash-chart" role="img" aria-label={t("cashout")}>
              <ResponsiveContainer width="100%" height="100%">
                <ComposedChart data={data.map((row) => ({ name: `M${row.month}`, bill: Number(row.wageBill), cumulative: Number(row.cumulative) }))}>
                  <XAxis dataKey="name" tick={{ fill: colors.muted, fontSize: 11 }} axisLine={false} tickLine={false} />
                  <YAxis tick={{ fill: colors.muted, fontSize: 11 }} axisLine={false} tickLine={false} width={48} />
                  <Tooltip contentStyle={tip(colors)} /><Legend />
                  <Bar dataKey="bill" name={t("bill")} fill={colors.red} radius={[6, 6, 0, 0]} isAnimationActive={!reduced} animationDuration={1000} />
                  <Line dataKey="cumulative" name={t("cumulative")} stroke={colors.gray} strokeWidth={2} dot={false} isAnimationActive={!reduced} animationDuration={1000} />
                </ComposedChart>
              </ResponsiveContainer>
            </div>
          )}
        </Tile>
      ) : null}
      {scope.finance ? (
        <Tile title={t("earners")} hint={t("hintEarners")} name="earners" period={period} enabled={scope.finance} t={t} isEmpty={listEmpty}>
          {(data, { reduced }) => (
            <div className="dash-chart" role="img" aria-label={t("earners")}>
              <ResponsiveContainer width="100%" height="100%"><BarChart data={data} layout="vertical"><XAxis type="number" tick={{ fill: colors.muted, fontSize: 11 }} axisLine={false} tickLine={false} /><YAxis type="category" dataKey="name" width={120} tick={{ fill: colors.muted, fontSize: 11 }} axisLine={false} tickLine={false} /><Tooltip contentStyle={tip(colors)} /><Bar dataKey="wage" name={t("payroll")} fill={colors.red} isAnimationActive={!reduced} animationDuration={1000} /></BarChart></ResponsiveContainer>
            </div>
          )}
        </Tile>
      ) : null}
      {scope.finance ? (
        <Tile title={t("bonusChart")} hint={t("hintBonus")} name="bonuses" period={period} enabled={scope.finance} t={t} isEmpty={listEmpty}>
          {(data, { reduced }) => (
            <div className="dash-chart" role="img" aria-label={t("bonusChart")}>
              <ResponsiveContainer width="100%" height="100%"><BarChart data={data}><XAxis dataKey="name" tick={{ fill: colors.muted, fontSize: 11 }} axisLine={false} tickLine={false} /><YAxis tick={{ fill: colors.muted, fontSize: 11 }} axisLine={false} tickLine={false} width={36} /><Tooltip contentStyle={tip(colors)} /><Legend /><Bar dataKey="earned" name={t("earned")} fill={colors.red} isAnimationActive={!reduced} animationDuration={1000} /><Bar dataKey="paid" name={t("paid")} fill={colors.gray} isAnimationActive={!reduced} animationDuration={1000} /></BarChart></ResponsiveContainer>
            </div>
          )}
        </Tile>
      ) : null}
      {scope.partners ? (
        <Tile title={t("partners")} hint={t("hintPartners")} name="partners" period={period} enabled={scope.partners} t={t} isEmpty={listEmpty}>
          {(data, { reduced }) => (
            <div className="dash-chart" role="img" aria-label={t("partners")}>
              <ResponsiveContainer width="100%" height="100%"><BarChart data={data}><XAxis dataKey="name" tick={{ fill: colors.muted, fontSize: 11 }} axisLine={false} tickLine={false} /><YAxis tick={{ fill: colors.muted, fontSize: 11 }} axisLine={false} tickLine={false} width={40} /><Tooltip contentStyle={tip(colors)} /><Bar dataKey="income" name={t("partners")} fill={colors.red} isAnimationActive={!reduced} animationDuration={1000} /></BarChart></ResponsiveContainer>
            </div>
          )}
        </Tile>
      ) : null}
    </div>
  );
}
