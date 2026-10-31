"use client";

import { Bar, BarChart, CartesianGrid, Cell, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { useTheme } from "@/components/providers/ThemeProvider";
import { Button } from "@/ui/kit";
import { POSITION, label, money } from "./playersCopy";

const PALETTE = ["#a3283a", "#c4364a", "#e0707f", "#f2b6bd", "#8d8d8d", "#5c5c5c"];

export function PlayerAnalytics({ data, copy, locale, executive, onComplete }) {
  const { theme } = useTheme();
  const ink = theme === "dark" ? "#f4f4f5" : "#161616";
  const grid = theme === "dark" ? "#2a2a2a" : "#ececec";
  if (!data) return <div className="squad-skeleton" />;
  const sample = data.withDateOfBirth || 0;
  const missing = data.missingDateOfBirth || 0;
  const tick = { fill: ink, fontSize: 12 };
  const tip = { contentStyle: { background: theme === "dark" ? "#111" : "#fff", border: `1px solid ${grid}`, borderRadius: 12, color: ink } };

  return (
    <div className="analytics-view">
      {missing > 0 ? (
        <div className="squad-empty">
          <strong>{missing} / {data.count} {copy.noDob}</strong>
          <p>{copy.sample} {sample}</p>
          <Button variant="primary" onClick={onComplete}>{copy.completeData}</Button>
        </div>
      ) : null}
      <div className="kpi-strip">
        <article><small>{copy.squad}</small><strong>{data.count}</strong></article>
        <article><small>{copy.avg}</small><strong>{data.averageAge == null ? "–" : Number(data.averageAge).toFixed(1)}</strong></article>
        <article><small>Median</small><strong>{data.medianAge ?? "–"}</strong></article>
        <article><small>{copy.range}</small><strong>{data.youngest ?? "–"} / {data.oldest ?? "–"}</strong></article>
      </div>
      <div className="chart-grid">
        <ChartCard title={copy.age} sample={sample} total={data.count}>
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={data.ageBands || []}>
              <CartesianGrid stroke={grid} strokeDasharray="4 4" vertical={false} />
              <XAxis dataKey="name" tick={tick} axisLine={false} tickLine={false} />
              <YAxis allowDecimals={false} tick={tick} axisLine={false} tickLine={false} />
              <Tooltip {...tip} /><Bar dataKey="count" fill="#a3283a" radius={[8, 8, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </ChartCard>
        <ChartCard title={copy.position} sample={sample} total={data.count}>
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={Object.keys(POSITION).map((key) => {
              const found = (data.averageByPosition || []).find((row) => row.name === key);
              return { name: label(POSITION, key, locale), age: found ? Math.round(Number(found.age)) : 0 };
            })} layout="vertical" margin={{ left: 8, right: 16 }}>
              <CartesianGrid stroke={grid} strokeDasharray="4 4" horizontal={false} />
              <XAxis type="number" domain={[0, 50]} tick={tick} axisLine={false} tickLine={false} />
              <YAxis type="category" dataKey="name" width={132} tick={tick} axisLine={false} tickLine={false} />
              <Tooltip {...tip} /><Bar dataKey="age" fill="#c4364a" barSize={14} radius={[0, 8, 8, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </ChartCard>
        <ChartCard title="Players by line" sample={data.count}>
          <ResponsiveContainer width="100%" height="100%">
            <PieChart>
              <Pie data={data.byLine || []} dataKey="count" nameKey="name" innerRadius={58} outerRadius={84} paddingAngle={2}>
                {(data.byLine || []).map((row, index) => <Cell key={row.name} fill={PALETTE[index % PALETTE.length]} />)}
              </Pie>
              <Tooltip {...tip} />
            </PieChart>
          </ResponsiveContainer>
        </ChartCard>
        <ChartCard title={copy.depth} sample={data.count}>
          <div className="vfc-depth">
            {(data.depth || []).map((row) => {
              const width = Math.min(100, (row.count / Math.max(row.minimum, 1)) * 70);
              return (
                <div key={row.name}>
                  <span>{label(POSITION, row.name, locale)}</span>
                  <div className={`vfc-bar${row.thin ? " is-thin" : ""}`}><span style={{ width: `${width}%` }} /></div>
                  <b>{row.count}/{row.minimum}</b>
                </div>
              );
            })}
          </div>
        </ChartCard>
        <ChartCard title={copy.birthdayChart} sample={sample}>
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={data.birthdaysByMonth || []}>
              <CartesianGrid stroke={grid} strokeDasharray="4 4" vertical={false} />
              <XAxis dataKey="name" tick={tick} axisLine={false} tickLine={false} />
              <YAxis allowDecimals={false} tick={tick} axisLine={false} tickLine={false} />
              <Tooltip {...tip} /><Bar dataKey="count" fill="#e0707f" radius={[8, 8, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </ChartCard>
        <ChartCard title={copy.quarters} sample={data.count}>
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={data.contractQuarters || []}>
              <CartesianGrid stroke={grid} strokeDasharray="4 4" vertical={false} />
              <XAxis dataKey="name" tick={tick} axisLine={false} tickLine={false} />
              <YAxis allowDecimals={false} tick={tick} axisLine={false} tickLine={false} />
              <Tooltip {...tip} /><Bar dataKey="count" fill="#a3283a" radius={[8, 8, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </ChartCard>
        <ChartCard title={copy.nationalitySplit} sample={data.count}>
          <Donut rows={data.byNationality || []} tip={tip} />
        </ChartCard>
        <ChartCard title={copy.footSplit} sample={data.count}>
          <Donut rows={data.byFoot || []} tip={tip} />
        </ChartCard>
        <ChartCard title={copy.statusSplit} sample={data.count}>
          <Donut rows={data.byStatus || []} tip={tip} />
        </ChartCard>
        {executive ? (
          <ChartCard title={copy.pay} sample={data.count}>
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={(data.payByLine || []).map((row) => ({ name: row.name, amount: Number(row.amount) }))} layout="vertical">
                <XAxis type="number" tick={tick} axisLine={false} tickLine={false} />
                <YAxis type="category" dataKey="name" width={110} tick={tick} axisLine={false} tickLine={false} />
                <Tooltip {...tip} formatter={(value) => money(value)} />
                <Bar dataKey="amount" fill="#a3283a" radius={[0, 8, 8, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </ChartCard>
        ) : null}
      </div>
    </div>
  );
}

function Donut({ rows, tip }) {
  return (
    <ResponsiveContainer width="100%" height="100%">
      <PieChart>
        <Pie data={rows} dataKey="count" nameKey="name" innerRadius={52} outerRadius={80}>
          {rows.map((row, index) => <Cell key={row.name} fill={PALETTE[index % PALETTE.length]} />)}
        </Pie>
        <Tooltip {...tip} />
      </PieChart>
    </ResponsiveContainer>
  );
}

function ChartCard({ title, sample, total, children }) {
  return (
    <section className="vfc-chart">
      <header><h3>{title}</h3></header>
      <p>{sample == null ? "" : `Based on ${sample} of ${total || sample}`}</p>
      <div className="plot">{children}</div>
    </section>
  );
}
