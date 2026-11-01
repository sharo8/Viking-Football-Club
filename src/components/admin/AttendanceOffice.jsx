"use client";

import { useEffect, useMemo, useState } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { motion } from "framer-motion";
import { Bar, BarChart, CartesianGrid, Cell, Legend, Line, LineChart, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { Download, Eye, FileSpreadsheet, FileText, MoreHorizontal, Pencil } from "lucide-react";
import { api, getData } from "@/services/api";
import { useAdminToast } from "@/components/admin/toast";
import { ConfirmDialog, DataTable } from "@/crud/kit";
import { Avatar, Badge, Button, Field, IconButton, Modal, SelectInput, Tabs, TextArea, TextInput } from "@/ui/kit";
import { LINES, attendanceText, rwf } from "./attendanceCopy";
import { TakeAttendance } from "./TakeAttendance";
import { ExportDialog } from "./ExportDialog";
import { PlayerCombo } from "./PlayerCombo";

const POSITION_VALUES = ["Goalkeeper", "Center-Back", "Left-Back", "Right-Back", "Defensive Midfielder", "Central Midfielder", "Offensive Midfielder", "Winger", "Forward"];
const COLORS = { P: "#176b34", L: "#c4841d", A: "#a3283a", E: "#1d4e89" };
const reduce = () => typeof window !== "undefined" && window.matchMedia("(prefers-reduced-motion: reduce)").matches;

function filtersOf(params) {
  const read = (key) => params.get(key) || undefined;
  return {
    audience: params.get("audience") || "players",
    preset: params.get("preset") || "month",
    from: read("from"),
    to: read("to"),
    grain: params.get("grain") || "day",
    q: read("q"),
    position: read("position"),
    line: read("line"),
    role: read("role"),
    department: read("department"),
    mark: read("mark"),
    kind: read("kind"),
    sessionStatus: read("sessionStatus"),
    hasFine: read("hasFine"),
    edited: read("edited"),
    recordedBy: read("recordedBy"),
    compare: read("compare"),
    players: read("players"),
  };
}

async function download(path, params, filename) {
  const response = await api.get(path, { params, responseType: "blob" });
  const url = URL.createObjectURL(response.data);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}

function CountUp({ value }) {
  const target = Number(value || 0);
  const [shown, setShown] = useState(reduce() ? target : 0);
  useEffect(() => {
    if (reduce()) {
      setShown(target);
      return undefined;
    }
    const start = performance.now();
    let frame;
    const tick = (now) => {
      const progress = Math.min(1, (now - start) / 600);
      setShown(Math.round(target * progress));
      if (progress < 1) frame = requestAnimationFrame(tick);
    };
    frame = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(frame);
  }, [target]);
  return shown;
}

function markLabel(t, code) {
  return { P: t.present, L: t.late, A: t.absent, E: t.excused, I: t.injured, N: t.notSelected }[code] || code;
}

export function AttendanceOffice({ locale = "en" }) {
  const t = attendanceText(locale);
  const params = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();
  const toast = useAdminToast();
  const filter = filtersOf(params);
  const view = params.get("view") || "grid";
  const [grid, setGrid] = useState(null);
  const [sessions, setSessions] = useState(null);
  const [stats, setStats] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [taking, setTaking] = useState(false);
  const [editing, setEditing] = useState(null);
  const [person, setPerson] = useState(null);
  const [session, setSession] = useState(null);
  const [history, setHistory] = useState(null);
  const [menu, setMenu] = useState(null);
  const [exportOpen, setExportOpen] = useState(false);
  const [importer, setImporter] = useState(false);
  const [justify, setJustify] = useState(null);
  const [confirm, setConfirm] = useState(null);
  const [reason, setReason] = useState("");
  const [viewName, setViewName] = useState("");
  const [views, setViews] = useState([]);
  const [exporting, setExporting] = useState("");
  const [exportDialog, setExportDialog] = useState(false);
  const [exportFormat, setExportFormat] = useState("pdf");
  const [roster, setRoster] = useState([]);
  const [viewsOpen, setViewsOpen] = useState(false);

  const query = params.toString();
  useEffect(() => {
    const stored = window.localStorage.getItem("vikingfc.attendance.views");
    setViews(stored ? JSON.parse(stored) : []);
  }, []);

  function update(patch) {
    const next = new URLSearchParams(params.toString());
    Object.entries(patch).forEach(([key, value]) => {
      if (value === undefined || value === null || value === "" || value === "all") next.delete(key);
      else next.set(key, String(value));
    });
    router.replace(`${pathname}?${next.toString()}`, { scroll: false });
  }

  function load() {
    setLoading(true);
    setError("");
    const current = filtersOf(new URLSearchParams(query));
    const request = view === "sessions"
      ? getData("/api/attendance/sessions", { ...current, status: current.sessionStatus, page: 0, size: 50 }).then((data) => setSessions(data))
      : view === "stats"
        ? Promise.all([getData("/api/attendance/stats", current), getData("/api/attendance/grid", current)]).then(([data, board]) => { setStats(data); setGrid(board); })
        : getData("/api/attendance/grid", current).then((data) => setGrid(data));
    request.then(() => setLoading(false)).catch((err) => {
      setError(err.message);
      setLoading(false);
    });
  }

  useEffect(() => { load(); }, [query, view]);

  useEffect(() => {
    const path = filter.audience === "staff" ? "/api/staff" : "/api/players";
    getData(path).then((rows) => {
      const list = (rows || []).filter((person) => {
        if (filter.audience !== "staff") return true;
        const role = `${person.roleName || ""} ${person.role || ""}`.toLowerCase();
        return String(person.department || "").toLowerCase() !== "medical" && !role.includes("doctor");
      });
      setRoster(list);
    }).catch(() => setRoster([]));
  }, [filter.audience]);

  useEffect(() => {
    if (!exportOpen) return undefined;
    function close(event) {
      if (!event.target.closest(".att-export")) setExportOpen(false);
    }
    function escape(event) {
      if (event.key === "Escape") setExportOpen(false);
    }
    document.addEventListener("mousedown", close);
    document.addEventListener("keydown", escape);
    return () => {
      document.removeEventListener("mousedown", close);
      document.removeEventListener("keydown", escape);
    };
  }, [exportOpen]);

  const access = grid?.access || sessions?.access || {};
  const counts = grid?.counts || sessions?.counts || { players: 0, staff: 0 };
  const locked = Boolean((view === "sessions" ? sessions : view === "stats" ? stats : grid)?.locked);
  const canTake = filter.audience === "staff" ? access.staff : access.players !== false;
  const people = grid?.people || [];
  const columns = grid?.sessions || [];
  const shownCount = view === "sessions" ? (sessions?.items?.length || 0) : people.length;

  function saveFile(path, params, filename, key) {
    setExporting(key);
    setError("");
    download(path, params, filename).catch((err) => setError(err.message)).finally(() => setExporting(""));
  }

  const chipLabel = { q: t.search, position: t.position, line: t.line, role: t.role, department: t.department, mark: t.status, kind: t.kind, sessionStatus: t.sessionStatus, hasFine: t.hasFine, edited: t.edited, recordedBy: t.recordedBy, from: t.from, to: t.to, players: t.player };
  const chips = useMemo(() => Object.entries(filter).filter(([key, value]) => value && !["audience", "preset", "grain", "compare", "players"].includes(key)), [query]);

  async function openHistory(row, sessionId) {
    const rows = await getData(`/api/attendance/people/${row.personType}/${row.personId}/records`);
    setHistory({ name: row.name, row: rows.find((item) => String(item.sessionId) === String(sessionId)) });
  }

  async function runConfirm() {
    try {
      if (confirm.kind === "delete") await api.delete(`/api/attendance/sessions/${confirm.id}`, { data: { reason } });
      if (confirm.kind === "reopen") await api.post(`/api/attendance/sessions/${confirm.id}/reopen`, { reason });
      if (confirm.kind === "close") await api.post(`/api/attendance/sessions/${confirm.id}/close`);
      toast(t.saved);
      setConfirm(null);
      setReason("");
      setSession(null);
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <section className="crud-page att-page">
      <header className="att-head">
        <div>
          <h1>{t.title} <span className="view-code">{filter.audience === "staff" ? counts.staff : counts.players}</span></h1>
          <p>{t.subtitle}</p>
        </div>
        <div className="att-actions">
          <Button variant="primary" disabled={!canTake} title={!canTake ? t.locked : t.take} onClick={() => { setEditing(null); setTaking(true); }}>{t.take}</Button>
          <div className="vfc-pop att-pop-end att-export">
            <Button icon={<Download size={16} />} onClick={() => setExportOpen((value) => !value)}>{t.export}</Button>
            {exportOpen ? (
              <div className="vfc-popover att-menu">
                {["xlsx", "pdf", "csv"].map((format) => (
                  <Button key={format} icon={format === "xlsx" ? <FileSpreadsheet size={16} /> : format === "pdf" ? <FileText size={16} /> : <Download size={16} />} onClick={() => { setExportOpen(false); setExportFormat(format); setExportDialog(true); }}>{t[format === "xlsx" ? "excel" : format]}</Button>
                ))}
              </div>
            ) : null}
          </div>
          <Button onClick={() => setImporter(true)}>{t.import}</Button>
        </div>
      </header>

      <Tabs label={t.title} value={filter.audience} onChange={(value) => update({ audience: value })} items={[{ id: "players", label: `${t.players} ${counts.players}` }, { id: "staff", label: `${t.staff} ${counts.staff}` }]} />
      <Tabs label={t.sessions} id="att-view" value={view} onChange={(value) => update({ view: value })} items={[{ id: "sessions", label: t.sessions }, { id: "grid", label: t.grid }, { id: "stats", label: t.statistics }]} />

      <div className="att-filters">
        <div className="att-filter-group">
          <span>{t.period}</span>
          <div>
            {[["today", t.today], ["week", t.week], ["month", t.month], ["year", t.year], ["all", t.all], ["custom", t.custom]].map(([id, label]) => (
              <button key={id} type="button" className={`vfc-chip${filter.preset === id ? " is-on" : ""}`} onClick={() => update({ preset: id })}>{label}</button>
            ))}
          </div>
        </div>
        <div className="att-filter-group">
          <span>{t.groupBy}</span>
          <div>
            {[["day", t.day], ["week", t.grainWeek], ["month", t.grainMonth], ["year", t.grainYear]].map(([id, label]) => (
              <button key={id} type="button" className={`vfc-chip${filter.grain === id ? " is-on" : ""}`} onClick={() => update({ grain: id })}>{label}</button>
            ))}
          </div>
        </div>
        <TextInput aria-label={t.search} placeholder={t.search} value={params.get("q") || ""} onChange={(event) => update({ q: event.target.value })} />
        <PlayerCombo locale={locale} players={roster} selected={(params.get("players") || "").split(",").filter(Boolean)} onChange={(ids) => update({ players: ids.join(",") })} />
        {filter.audience === "players" ? (
          <>
            <SelectInput aria-label={t.position} value={params.get("position") || ""} onChange={(event) => update({ position: event.target.value })}>
              <option value="">{t.position}</option>
              {POSITION_VALUES.map((value, index) => <option key={value} value={value}>{t.positions[index]}</option>)}
            </SelectInput>
            <SelectInput aria-label={t.line} value={params.get("line") || ""} onChange={(event) => update({ line: event.target.value })}>
              <option value="">{t.line}</option>
              {LINES.map(([value, key]) => <option key={value} value={value}>{t[key]}</option>)}
            </SelectInput>
          </>
        ) : (
          <>
            <TextInput aria-label={t.role} placeholder={t.role} value={params.get("role") || ""} onChange={(event) => update({ role: event.target.value })} />
            <TextInput aria-label={t.department} placeholder={t.department} value={params.get("department") || ""} onChange={(event) => update({ department: event.target.value })} />
          </>
        )}
        <SelectInput aria-label={t.status} value={params.get("mark") || ""} onChange={(event) => update({ mark: event.target.value })}>
          <option value="">{t.status}</option>
          {["P", "L", "A", "E", "I", "N"].map((code) => <option key={code} value={code}>{markLabel(t, code)}</option>)}
        </SelectInput>
        <SelectInput aria-label={t.kind} value={params.get("kind") || ""} onChange={(event) => update({ kind: event.target.value })}>
          <option value="">{t.kind}</option>
          <option value="TRAINING">{t.training}</option>
          <option value="MATCH">{t.match}</option>
        </SelectInput>
        <SelectInput aria-label={t.sessionStatus} value={params.get("sessionStatus") || ""} onChange={(event) => update({ sessionStatus: event.target.value })}>
          <option value="">{t.sessionStatus}</option>
          <option value="OPEN">{t.open}</option>
          <option value="CLOSED">{t.closed}</option>
        </SelectInput>
        <button type="button" className={`vfc-chip${params.get("hasFine") ? " is-on" : ""}`} onClick={() => update({ hasFine: params.get("hasFine") ? "" : "true" })}>{t.hasFine}</button>
        <button type="button" className={`vfc-chip${params.get("edited") ? " is-on" : ""}`} onClick={() => update({ edited: params.get("edited") ? "" : "true" })}>{t.edited}</button>
        <TextInput aria-label={t.recordedBy} placeholder={t.recordedBy} value={params.get("recordedBy") || ""} onChange={(event) => update({ recordedBy: event.target.value })} />
        {filter.preset === "custom" ? (
          <>
            <TextInput aria-label={t.from} type="date" value={params.get("from") || ""} onChange={(event) => update({ from: event.target.value })} />
            <TextInput aria-label={t.to} type="date" value={params.get("to") || ""} onChange={(event) => update({ to: event.target.value })} />
          </>
        ) : null}
        <Button onClick={() => router.replace(pathname, { scroll: false })}>{t.clear}</Button>
        <div className="vfc-pop">
          <Button onClick={() => setViewsOpen((value) => !value)}>{t.views}</Button>
          {viewsOpen ? (
            <div className="vfc-popover att-menu">
              <TextInput aria-label={t.viewName} placeholder={t.viewName} value={viewName} onChange={(event) => setViewName(event.target.value)} />
              <Button onClick={() => {
                if (!viewName.trim()) return;
                const next = [...views.filter((item) => item.name !== viewName.trim()), { name: viewName.trim(), query }];
                setViews(next);
                window.localStorage.setItem("vikingfc.attendance.views", JSON.stringify(next));
                setViewName("");
                setViewsOpen(false);
              }}>{t.saveView}</Button>
              {views.map((item) => <Button key={item.name} onClick={() => { setViewsOpen(false); router.replace(`${pathname}?${item.query}`, { scroll: false }); }}>{item.name}</Button>)}
            </div>
          ) : null}
        </div>
      </div>
      <p className="att-count-line">{t.showing.replace("{shown}", String(shownCount)).replace("{total}", String(filter.audience === "staff" ? counts.staff : counts.players))}</p>
      {chips.length ? <div className="att-chips">{chips.map(([key, value]) => <Button key={key} onClick={() => update({ [key]: "" })}>{chipLabel[key] || key}: {key === "players" ? value.split(",").map((id) => roster.find((person) => String(person.id) === id)?.fullName || id).join(", ") : value} ×</Button>)}</div> : null}
      <p className="vfc-muted">{t.signature}</p>

      {loading ? <div className="att-skeleton" /> : null}
      {error ? <Button onClick={load}>{t.retry}: {error}</Button> : null}
      {locked ? <p className="crud-empty">{t.locked}</p> : null}

      {!loading && !error && !locked && view === "sessions" ? (
        <div className="att-sessions">
          {(sessions?.items || []).map((item, index) => (
            <motion.article key={item.id} className="att-card" initial={reduce() ? false : { opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: index * 0.03 }}>
              <button type="button" className="att-card-hit" onClick={() => setSession(item)}>
                <strong>{String(item.date).slice(5)} · {item.kind === "MATCH" ? t.match : t.training}</strong>
                <span>{String(item.startTime || "").slice(0, 5)} {item.place}</span>
                <span>{t.present} {item.present} · {t.late} {item.late} · {t.absent} {item.absent} · {t.excused} {item.excused}</span>
                <Badge tone={item.status === "CLOSED" ? "neutral" : "good"}>{item.status === "CLOSED" ? t.closed : t.open}</Badge>
                <span>{t.fines} {item.fines} · {rwf(item.fineTotal)}</span>
                <small>{t.recordedBy} {item.recordedBy || "—"}</small>
              </button>
            </motion.article>
          ))}
          {!sessions?.items?.length ? <div className="crud-empty"><p>{t.empty}</p><Button variant="primary" disabled={!canTake} onClick={() => setTaking(true)}>{t.emptyCta}</Button></div> : null}
        </div>
      ) : null}

      {!loading && !error && !locked && view === "grid" ? (
        people.length ? (
          <div className="att-scroll">
            <table className="crud-table att-table">
              <thead>
                {filter.grain !== "day" && grid?.groups?.length ? (
                  <tr>
                    <th /><th />
                    {(grid.groups || []).map((group) => <th key={group.label} colSpan={group.span}>{group.label}</th>)}
                    <th colSpan={8} />
                  </tr>
                ) : null}
                <tr>
                  <th>{filter.audience === "staff" ? t.staff : t.players}</th>
                  <th>{t.rate}</th>
                  {columns.map((column) => <th key={column.id}><button type="button" className="audit-link" onClick={() => setSession(column)}>{column.label}</button></th>)}
                  <th>{t.held}</th><th>{t.present}</th><th>{t.late}</th><th>{t.absent}</th><th>{t.excused}</th>
                  <th>{t.recordedBy}</th><th>{t.modified}</th><th>{t.actions}</th>
                </tr>
              </thead>
              <tbody>
                {people.map((row, index) => (
                  <tr key={row.personId} style={reduce() ? undefined : { animationDelay: `${index * 30}ms` }} className="att-row">
                    <td><span className="att-name"><Avatar name={row.name} src={row.photoUrl} /> <span>{row.name}<small>{row.jersey ? ` #${row.jersey}` : ""} {row.role}</small></span></span></td>
                    <td><div className="vfc-bar att-rate"><span style={{ width: `${row.rate || 0}%` }} /></div> {row.rate}%</td>
                    {columns.map((column) => {
                      const cell = row.cells?.[column.id] || row.cells?.[String(column.id)];
                      const title = cell ? `${markLabel(t, cell.mark)} ${cell.arrival || ""} ${cell.minutesLate ? `${cell.minutesLate}m` : ""} ${Number(cell.fine) ? rwf(cell.fine) : ""} ${cell.note || ""}` : "";
                      return (
                        <td key={column.id}>
                          {cell?.mark ? <button type="button" className={`mark-chip ${cell.mark}`} title={title} onClick={() => openHistory(row, column.id)}>{cell.mark}</button> : "—"}
                          {Number(cell?.fine) > 0 ? <small className="att-fine">{rwf(cell.fine)}</small> : null}
                        </td>
                      );
                    })}
                    <td>{row.counted}</td><td>{row.present}</td><td>{row.late}</td><td>{row.absent}</td><td>{row.excused}</td>
                    <td><button type="button" className="audit-link" onClick={() => openHistory(row, columns[0]?.id)}>{row.recordedBy || "—"}</button></td>
                    <td>{row.modifiedBy || "—"} {row.edited ? <Badge tone="warn">{t.editedBadge}</Badge> : null}</td>
                    <td>
                      <div className="crud-actions">
                        <IconButton label={t.viewPerson} onClick={() => setPerson(row)}><Eye size={16} /></IconButton>
                        <IconButton label={t.editMarks} disabled={!canTake} title={!canTake ? t.locked : t.editMarks} onClick={() => { setEditing(columns[columns.length - 1]?.id || null); setTaking(true); }}><Pencil size={16} /></IconButton>
                        <IconButton label={t.justify} disabled={!access.approve} title={access.approve ? t.justify : t.approveHint} onClick={() => setJustify(row)}><FileText size={16} /></IconButton>
                        <IconButton label={t.download} onClick={() => download(`/api/attendance/people/${row.personType}/${row.personId}/export`, { format: "pdf", lang: locale }, `${row.name}.pdf`)}><Download size={16} /></IconButton>
                        <div className="vfc-pop att-pop-end">
                          <IconButton label={t.actions} onClick={() => setMenu(menu === row.personId ? null : row.personId)}><MoreHorizontal size={16} /></IconButton>
                          {menu === row.personId ? (
                            <div className="vfc-popover att-menu">
                              <Button icon={<FileSpreadsheet size={16} />} loading={exporting === `xlsx-${row.personId}`} onClick={() => saveFile(`/api/attendance/people/${row.personType}/${row.personId}/export`, { format: "xlsx", lang: locale }, `${row.name}.xlsx`, `xlsx-${row.personId}`)}>{t.excel}</Button>
                            </div>
                          ) : null}
                        </div>
                      </div>
                    </td>
                  </tr>
                ))}
                <tr>
                  <td>{t.totals}</td><td />
                  {columns.map((column) => <td key={column.id}>{["P", "L", "A", "E"].map((code) => `${code}${grid?.totals?.[column.id]?.[code] || grid?.totals?.[String(column.id)]?.[code] || 0}`).join(" ")}</td>)}
                  <td colSpan={8} />
                </tr>
              </tbody>
            </table>
          </div>
        ) : <div className="crud-empty"><p>{t.empty}</p><Button variant="primary" disabled={!canTake} onClick={() => setTaking(true)}>{t.emptyCta}</Button></div>
      ) : null}

      {!loading && !error && !locked && view === "stats" && stats ? <StatsPanel t={t} stats={stats} people={people.length ? people : []} compare={params.get("compare") || ""} onCompare={(id) => {
        const current = (params.get("compare") || "").split(",").filter(Boolean);
        const next = current.includes(String(id)) ? current.filter((item) => item !== String(id)) : [...current, String(id)].slice(0, 5);
        update({ compare: next.join(",") });
      }} /> : null}

      {taking ? <TakeAttendance t={t} audience={filter.audience} sessionId={editing} onClose={() => setTaking(false)} onDone={() => { setTaking(false); toast(t.saved); load(); }} /> : null}
      {person ? <PersonDialog t={t} locale={locale} row={person} onClose={() => setPerson(null)} onEdit={() => { setEditing(null); setPerson(null); setTaking(true); }} /> : null}
      {session ? (
        <Modal title={`${session.date || session.label || ""}`} subtitle={session.kind === "MATCH" ? t.match : t.training} onClose={() => setSession(null)} closeLabel={t.close} footer={(
          <>
            <Button variant="danger" disabled={!access.closed} title={access.closed ? t.delete : t.locked} onClick={() => setConfirm({ kind: "delete", id: session.id })}>{t.delete}</Button>
            <div className="vfc-foot-right">
              {session.status === "CLOSED" ? <Button disabled={!access.closed} onClick={() => setConfirm({ kind: "reopen", id: session.id })}>{t.reopen}</Button> : <Button disabled={!canTake} onClick={() => setConfirm({ kind: "close", id: session.id })}>{t.closeSession}</Button>}
              <Button variant="primary" disabled={!canTake} onClick={() => { setEditing(session.id); setSession(null); setTaking(true); }}>{t.editMarks}</Button>
            </div>
          </>
        )}>
          <p>{t.present} {session.present ?? "—"} · {t.late} {session.late ?? "—"} · {t.absent} {session.absent ?? "—"} · {t.excused} {session.excused ?? "—"}</p>
          <p>{session.status === "CLOSED" ? t.closed : t.open}{session.closedBy ? ` · ${session.closedBy}` : ""}</p>
        </Modal>
      ) : null}
      {history ? (
        <Modal title={t.history} subtitle={history.name} onClose={() => setHistory(null)} closeLabel={t.close}>
          {history.row ? (
            <DataTable
              columns={[
                { key: "oldStatus", label: t.old },
                { key: "newStatus", label: t.nextStatus },
                { key: "who", label: t.who },
                { key: "when", label: t.when, render: (row) => row.when ? new Intl.DateTimeFormat(locale, { dateStyle: "medium", timeStyle: "short", timeZone: "Africa/Kigali" }).format(new Date(row.when)) : "—" },
                { key: "reason", label: t.reason },
              ]}
              rows={(history.row.history || []).map((item, index) => ({ ...item, id: index }))}
              empty={t.empty}
            />
          ) : <p>{t.empty}</p>}
          {history.row ? <p>{t.recordedBy} {history.row.recordedBy || "—"} · {t.modified} {history.row.modifiedBy || "—"}</p> : null}
        </Modal>
      ) : null}
      {justify ? <JustifyDialog t={t} row={justify} onClose={() => setJustify(null)} onDone={() => { setJustify(null); toast(t.saved); load(); }} /> : null}
      {importer ? <ImportDialog t={t} locale={locale} audience={filter.audience} onClose={() => setImporter(false)} onDone={() => { setImporter(false); toast(t.saved); load(); }} /> : null}
      {confirm ? (
        <ConfirmDialog title={confirm.kind === "delete" ? t.delete : confirm.kind === "reopen" ? t.reopen : t.closeSession} body={confirm.kind === "delete" ? t.confirmDelete : confirm.kind === "reopen" ? t.confirmReopen : t.confirmClose} confirmLabel={t[confirm.kind === "close" ? "closeSession" : confirm.kind === "reopen" ? "reopen" : "delete"]} cancelLabel={t.cancel} onClose={() => setConfirm(null)} onConfirm={runConfirm} confirmDisabled={confirm.kind !== "close" && !reason.trim()}>
          {confirm.kind === "close" ? null : <Field label={t.reason} required><TextArea value={reason} onChange={(event) => setReason(event.target.value)} /></Field>}
        </ConfirmDialog>
      ) : null}
      <ExportDialog key={`${exportFormat}-${exportDialog}`} open={exportDialog} askContent defaultResource="ATTENDANCE" defaultFormat={exportFormat} players={roster.map((person) => ({ id: person.id, fullName: person.fullName, jerseyNumber: person.jerseyNumber, photoUrl: person.photoUrl }))} busy={Boolean(exporting)} onClose={() => setExportDialog(false)} onDownload={(body) => {
        setExporting(body.format);
        api.post("/api/exports", { ...body, scope: { ...(body.scope || {}), preset: body.scope?.preset || filter.preset, from: filter.from, to: filter.to } }, { responseType: "blob" }).then((response) => {
          const match = String(response.headers["content-disposition"] || "").match(/filename="([^"]+)"/);
          const link = document.createElement("a");
          link.href = URL.createObjectURL(response.data);
          link.download = match ? match[1] : `attendance.${body.format}`;
          link.click();
          URL.revokeObjectURL(link.href);
          setExportDialog(false);
        }).catch((err) => setError(err.message)).finally(() => setExporting(""));
      }} />
    </section>
  );
}

function StatsPanel({ t, stats, people, compare, onCompare }) {
  const donut = (stats.donut || []).map((item) => ({ name: markLabel(t, item.key), value: item.value, key: item.key }));
  const names = people.length ? people : [...(stats.best || []), ...(stats.worst || [])];
  const unique = [];
  names.forEach((row) => { if (!unique.some((item) => item.personId === row.personId)) unique.push(row); });
  const lines = {};
  (stats.compare || []).forEach((series) => {
    (series.points || []).forEach((point) => {
      lines[point.label] = { ...(lines[point.label] || { label: point.label }), [series.name]: Number(point.rate) };
    });
  });
  return (
    <div className="att-stats">
      <div className="att-kpis">
        {[[t.rate, `${stats.rate}%`], [t.punctuality, `${stats.punctuality}%`], [t.held, stats.sessions], [t.absences, stats.absences], [t.lateArrivals, stats.late], [t.finesIssued, rwf(stats.finesIssued)], [t.atRisk, stats.atRisk]].map(([label, value]) => (
          <article key={label} className="vfc-tile"><small>{label}</small><strong>{typeof value === "number" ? <CountUp value={value} /> : value}</strong></article>
        ))}
      </div>
      <div className="att-charts">
        <Chart title={t.status}><ResponsiveContainer width="100%" height={220}><BarChart data={stats.stacked || []}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="label" /><YAxis allowDecimals={false} /><Tooltip /><Legend /><Bar dataKey="P" stackId="a" fill={COLORS.P} /><Bar dataKey="L" stackId="a" fill={COLORS.L} /><Bar dataKey="A" stackId="a" fill={COLORS.A} /><Bar dataKey="E" stackId="a" fill={COLORS.E} /></BarChart></ResponsiveContainer></Chart>
        <Chart title={t.rate}><ResponsiveContainer width="100%" height={220}><LineChart data={stats.trend || []}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="label" /><YAxis /><Tooltip /><Line dataKey="rate" stroke="#a3283a" strokeWidth={2} dot={false} /></LineChart></ResponsiveContainer></Chart>
        <Chart title={t.status}><ResponsiveContainer width="100%" height={220}><PieChart><Pie data={donut} dataKey="value" nameKey="name" innerRadius={50} outerRadius={80}>{donut.map((item) => <Cell key={item.key} fill={COLORS[item.key] || "#888"} />)}</Pie><Tooltip /></PieChart></ResponsiveContainer></Chart>
        <Chart title={t.position}><ResponsiveContainer width="100%" height={220}><BarChart data={stats.byPosition || []} layout="vertical"><XAxis type="number" /><YAxis type="category" dataKey="name" width={120} /><Tooltip /><Bar dataKey="rate" fill="#a3283a" /></BarChart></ResponsiveContainer></Chart>
      </div>
      <div className="att-board">
        <article className="att-card">
          <h3>{t.onTrack} ≥ {stats.onTrack}% · {t.watch} ≥ {stats.watch}%</h3>
          <Heatmap rows={stats.heatmap || []} />
        </article>
        <article className="att-card">
          <h3>{t.rate}</h3>
          {(stats.best || []).map((row) => <Leader key={row.personId} row={row} />)}
          <h3>{t.atRisk}</h3>
          {(stats.atRiskList || []).map((row) => <Leader key={`risk-${row.personId}`} row={row} />)}
        </article>
      </div>
      <div className="att-card">
        <h3>{t.compare}</h3>
        <div className="att-filters">
          {unique.slice(0, 12).map((row) => <button key={row.personId} type="button" className={`vfc-chip${compare.split(",").includes(String(row.personId)) ? " is-on" : ""}`} onClick={() => onCompare(row.personId)}>{row.name}</button>)}
        </div>
        <ResponsiveContainer width="100%" height={220}><LineChart data={Object.values(lines)}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="label" /><YAxis /><Tooltip /><Legend />{(stats.compare || []).map((series, index) => <Line key={series.personId} dataKey={series.name} stroke={["#a3283a", "#176b34", "#1d4e89", "#c4841d", "#6b21a8"][index]} />)}</LineChart></ResponsiveContainer>
      </div>
    </div>
  );
}

function Chart({ title, children }) {
  return <article className="vfc-chart"><h3>{title}</h3><div className="plot">{children}</div></article>;
}

function Heatmap({ rows }) {
  const days = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];
  const hours = [...new Set(rows.map((row) => row.hour))].sort((a, b) => a - b);
  return (
    <div className="att-heat" style={{ gridTemplateColumns: `88px repeat(${Math.max(hours.length, 1)}, 1fr)` }}>
      <span />
      {hours.map((hour) => <small key={hour}>{hour}:00</small>)}
      {days.map((day) => [
        <small key={day}>{day.slice(0, 3)}</small>,
        ...hours.map((hour) => {
          const cell = rows.find((row) => row.weekday === day && row.hour === hour);
          const rate = cell && cell.total ? Math.round((cell.present / cell.total) * 100) : 0;
          return <span key={`${day}-${hour}`} title={cell ? `${cell.present}/${cell.total}` : ""} style={{ background: `rgba(163,40,58,${cell ? 0.15 + rate / 160 : 0.04})` }}>{cell ? rate : ""}</span>;
        }),
      ])}
    </div>
  );
}

function Leader({ row }) {
  return <div className="att-leader"><Avatar name={row.name} src={row.photoUrl} /><span>{row.name}<small>{row.role}</small></span><div className="vfc-bar"><span style={{ width: `${row.rate || 0}%` }} /></div><strong>{row.rate}%</strong></div>;
}

function PersonDialog({ t, locale, row, onClose, onEdit }) {
  const [tab, setTab] = useState("summary");
  const [summary, setSummary] = useState(null);
  const [records, setRecords] = useState([]);
  const [error, setError] = useState("");
  useEffect(() => {
    Promise.all([
      getData(`/api/attendance/people/${row.personType}/${row.personId}/summary`),
      getData(`/api/attendance/people/${row.personType}/${row.personId}/records`),
    ]).then(([life, rows]) => { setSummary(life); setRecords(rows); }).catch((err) => setError(err.message));
  }, [row]);
  const months = {};
  records.forEach((item) => {
    const key = String(item.date).slice(0, 7);
    months[key] = months[key] || { label: key, P: 0, L: 0, A: 0, E: 0 };
    if (months[key][item.status] !== undefined) months[key][item.status] += 1;
  });
  return (
    <Modal title={row.name} subtitle={row.role} leading={<Avatar name={row.name} src={row.photoUrl} size="96" />} size="lg" onClose={onClose} closeLabel={t.close} footer={(
      <>
        <Button onClick={onEdit}>{t.editMarks}</Button>
        <div className="vfc-foot-right">
          <Button onClick={() => download(`/api/attendance/people/${row.personType}/${row.personId}/export`, { format: "xlsx", lang: locale }, `${row.name}.xlsx`)}>{t.excel}</Button>
          <Button onClick={() => download(`/api/attendance/people/${row.personType}/${row.personId}/export`, { format: "pdf", lang: locale }, `${row.name}.pdf`)}>{t.pdf}</Button>
          <Button variant="primary" onClick={onClose}>{t.close}</Button>
        </div>
      </>
    )}>
      {error ? <p>{error}</p> : null}
      {summary ? <p>{summary.band}{summary.suspended ? ` · ${t.closed}` : ""} · {t.since} {summary.since || "—"}</p> : null}
      <Tabs label={t.summary} id="person-tab" value={tab} onChange={setTab} items={[{ id: "summary", label: t.summary }, { id: "charts", label: t.charts }, { id: "history", label: t.history }, { id: "fines", label: t.fines }, { id: "just", label: t.justifications }]} />
      {tab === "summary" && summary ? (
        <div className="att-kpis">
          {[[t.held, summary.sessions], [t.present, summary.present], [t.late, summary.late], [t.absent, summary.absent], [t.excused, summary.excused], [t.rate, `${summary.rate}%`], [t.punctuality, `${summary.punctuality}%`], [t.streak, summary.streak], [t.longest, summary.longest], [t.bestMonth, summary.bestMonth || "—"], [t.worstMonth, summary.worstMonth || "—"], [t.finesIssued, rwf(summary.finesTotal)], [t.paid, rwf(summary.finesPaid)], [t.outstanding, rwf(summary.finesOutstanding)]].map(([label, value]) => <article key={label} className="vfc-tile"><small>{label}</small><strong>{value}</strong></article>)}
        </div>
      ) : null}
      {tab === "charts" ? <ResponsiveContainer width="100%" height={240}><BarChart data={Object.values(months)}><CartesianGrid strokeDasharray="3 3" /><XAxis dataKey="label" /><YAxis allowDecimals={false} /><Tooltip /><Bar dataKey="P" stackId="a" fill={COLORS.P} /><Bar dataKey="L" stackId="a" fill={COLORS.L} /><Bar dataKey="A" stackId="a" fill={COLORS.A} /><Bar dataKey="E" stackId="a" fill={COLORS.E} /></BarChart></ResponsiveContainer> : null}
      {tab === "history" ? <DataTable columns={[{ key: "date", label: t.day }, { key: "kind", label: t.kind }, { key: "status", label: t.status, render: (item) => markLabel(t, item.status) }, { key: "arrival", label: t.arrival }, { key: "minutesLate", label: t.minutes }, { key: "fine", label: t.fine, render: (item) => Number(item.fine) ? rwf(item.fine) : "—" }, { key: "recordedBy", label: t.recordedBy }, { key: "modifiedBy", label: t.modified }]} rows={records} empty={t.empty} /> : null}
      {tab === "fines" ? <DataTable columns={[{ key: "date", label: t.day }, { key: "rule", label: t.rule, render: (item) => item.rule ? `${t.rule} ${item.rule}` : "—" }, { key: "amount", label: t.amount, render: (item) => rwf(item.amount) }, { key: "status", label: t.status }]} rows={summary?.fines || []} empty={t.noFines} /> : null}
      {tab === "just" ? <DataTable columns={[{ key: "date", label: t.day }, { key: "reason", label: t.reason }, { key: "status", label: t.status }, { key: "submittedBy", label: t.who }]} rows={summary?.justifications || []} empty={t.empty} /> : null}
    </Modal>
  );
}

function JustifyDialog({ t, row, onClose, onDone }) {
  const [reason, setReason] = useState("");
  const [evidence, setEvidence] = useState("");
  const [error, setError] = useState("");
  async function save() {
    try {
      await api.post("/api/attendance/justifications", { personType: row.personType, personId: row.personId, reason, evidenceName: evidence });
      onDone();
    } catch (err) {
      setError(err.message);
    }
  }
  return (
    <Modal title={t.justify} subtitle={row.name} onClose={onClose} closeLabel={t.close} footer={<Button variant="primary" disabled={!reason.trim()} onClick={save}>{t.justify}</Button>}>
      <Field label={t.reason} required><TextArea value={reason} onChange={(event) => setReason(event.target.value)} /></Field>
      <Field label={t.evidence}><TextInput value={evidence} placeholder={t.evidence} onChange={(event) => setEvidence(event.target.value)} /></Field>
      {error ? <p className="crud-error">{error}</p> : null}
    </Modal>
  );
}

function ImportDialog({ t, locale, audience, onClose, onDone }) {
  const [rows, setRows] = useState(null);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  async function preview(file) {
    const body = new FormData();
    body.append("file", file);
    try {
      const response = await api.post(`/api/attendance/import/preview?audience=${audience}&kind=TRAINING`, body);
      setRows(response.data?.data?.rows || []);
    } catch (err) {
      setError(err.message);
    }
  }
  async function commit() {
    setSaving(true);
    try {
      await api.post("/api/attendance/import/commit", {
        audience: audience === "staff" ? "STAFF" : "PLAYERS",
        kind: "TRAINING",
        rows: (rows || []).filter((row) => row.personId && row.status && !row.error).map((row) => ({ personId: row.personId, date: row.date, status: row.status, minutesLate: row.minutesLate || 0 })),
      });
      onDone();
    } catch (err) {
      setError(err.message);
      setSaving(false);
    }
  }
  return (
    <Modal title={t.import} size="lg" onClose={onClose} closeLabel={t.close} footer={(
      <>
        <Button onClick={() => download("/api/attendance/template", { audience }, `attendance-template-${audience}.xlsx`)}>{t.template}</Button>
        <Button variant="primary" loading={saving} disabled={!rows?.some((row) => row.personId && !row.error)} onClick={commit}>{t.commit}</Button>
      </>
    )}>
      <Field label={t.import}><TextInput type="file" accept=".xlsx,.csv" onChange={(event) => event.target.files?.[0] && preview(event.target.files[0])} /></Field>
      <p>{t.errors}</p>
      {error ? <p className="crud-error">{error}</p> : null}
      {rows ? (
        <div className="att-scroll">
          <table className="crud-table">
            <thead><tr><th>{t.row}</th><th>{t.search}</th><th>{t.day}</th><th>{t.status}</th><th>{t.fine}</th><th>{t.errors}</th></tr></thead>
            <tbody>
              {rows.map((row) => (
                <tr key={`${row.line}-${row.date}`}>
                  <td>{row.line}</td><td>{row.personName || row.name}{row.suggestion && !row.personId ? <Button onClick={() => setRows(rows.map((item) => item === row ? { ...item, personId: row.suggestionId, personName: row.suggestion, error: "" } : item))}>{t.applySuggestion}: {row.suggestion}</Button> : null}</td>
                  <td>{row.date}</td><td>{row.status}</td><td>{Number(row.fine) ? `${t.rule} ${row.rule} · ${rwf(row.fine)}` : "—"}</td><td>{row.error}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
    </Modal>
  );
}
