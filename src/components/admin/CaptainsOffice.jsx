"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { motion } from "framer-motion";
import { ArrowLeftRight, Plus, Trash2 } from "lucide-react";
import { api, API_URL, getData } from "@/services/api";
import { useAuth } from "@/components/providers/AuthProvider";
import { useLocale } from "@/components/providers/LocaleProvider";
import { isExecutive } from "@/lib/admin/access";
import { Avatar, Button, Field, Modal, TextArea, TextInput } from "@/ui/kit";
import { POSITION, STATUS, label } from "./playersCopy";
import { CAPTAIN_COPY, CAPTAIN_RANKS } from "./captainsCopy";

const LINES = [["", "all"], ["Goalkeepers", "gk"], ["Defenders", "def"], ["Midfielders", "mid"], ["Attackers", "att"]];

function still() {
  return typeof window !== "undefined" && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

function photo(url) {
  if (!url) return "";
  return url.startsWith("http") ? url : `${API_URL}${url}`;
}

function when(value, locale) {
  if (!value) return "";
  const tag = locale === "fr" ? "fr-FR" : locale === "rw" ? "en-GB" : "en-GB";
  return new Intl.DateTimeFormat(tag, { day: "2-digit", month: "short", year: "numeric" }).format(new Date(value));
}

function span(start, end, copy) {
  if (!start) return "–";
  const from = new Date(start);
  const to = end ? new Date(end) : new Date();
  const hours = Math.max(0, (to.getTime() - from.getTime()) / 36e5);
  if (hours < 24) return from.toDateString() === to.toDateString() ? copy.sameDay : copy.lessThanDay;
  let months = (to.getFullYear() - from.getFullYear()) * 12 + (to.getMonth() - from.getMonth());
  let dayCount = to.getDate() - from.getDate();
  if (dayCount < 0) {
    months -= 1;
    dayCount += new Date(to.getFullYear(), to.getMonth(), 0).getDate();
  }
  const daysLabel = (count) => (count === 1 ? copy.oneDay : `${count} ${copy.days}`);
  if (months > 0) {
    const month = months === 1 ? copy.oneMonth : `${months} ${copy.months}`;
    return dayCount ? `${month} ${daysLabel(dayCount)}` : month;
  }
  if (dayCount >= 7) {
    const weeks = Math.floor(dayCount / 7);
    const rest = dayCount % 7;
    const week = weeks === 1 ? copy.oneWeek : `${weeks} ${copy.weeks}`;
    return rest ? `${week} ${daysLabel(rest)}` : week;
  }
  return daysLabel(Math.max(dayCount, 1));
}

function useCount(value) {
  const [shown, setShown] = useState(0);
  useEffect(() => {
    const target = Number(value || 0);
    if (still()) {
      setShown(target);
      return undefined;
    }
    const start = performance.now();
    let frame = 0;
    const tick = (now) => {
      const progress = Math.min(1, (now - start) / 700);
      setShown(Math.round(target * progress));
      if (progress < 1) frame = requestAnimationFrame(tick);
    };
    frame = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(frame);
  }, [value]);
  return shown;
}

export function CaptainsOffice() {
  const { locale } = useLocale();
  const { user } = useAuth();
  const copy = CAPTAIN_COPY[locale] || CAPTAIN_COPY.en;
  const canEdit = user && (isExecutive(user.role) || user.role === "ASSISTANT_MANAGER" || user.role === "HEAD_COACH");
  const [seasonId, setSeasonId] = useState("");
  const [seasons, setSeasons] = useState([]);
  const [summary, setSummary] = useState(null);
  const [captains, setCaptains] = useState([]);
  const [history, setHistory] = useState({ content: [], page: 0, totalPages: 0 });
  const [squad, setSquad] = useState([]);
  const [ready, setReady] = useState(false);
  const [toast, setToast] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState("");
  const [picker, setPicker] = useState(null);
  const [confirm, setConfirm] = useState(null);
  const [over, setOver] = useState("");
  const [query, setQuery] = useState("");
  const [line, setLine] = useState("");
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(8);
  const [historyRank, setHistoryRank] = useState("");
  const [historyPage, setHistoryPage] = useState(0);
  const [historySize, setHistorySize] = useState(5);
  const [historySort, setHistorySort] = useState("startedAt");
  const [historyDir, setHistoryDir] = useState("desc");
  const [historyCurrent, setHistoryCurrent] = useState(false);
  const [historyDraft, setHistoryDraft] = useState("");
  const [historyQuery, setHistoryQuery] = useState("");
  const [historyBusy, setHistoryBusy] = useState(true);
  const [sheet, setSheet] = useState(null);
  const readOnly = Boolean(summary?.readOnly) || seasons.find((item) => String(item.id) === String(seasonId))?.current === false;

  function load(nextSeason = seasonId) {
    const season = nextSeason || undefined;
    return Promise.all([
      getData("/api/captains/seasons"),
      getData("/api/captains/summary", { season }),
      getData("/api/captains", { season }),
      getData("/api/captains/history", { season, rank: historyRank || undefined, current: historyCurrent || undefined, q: historyQuery || undefined, sort: historySort, dir: historyDir, page: historyPage, size: historySize }),
      getData("/api/players/directory", { size: 100, sort: "jerseyNumber", dir: "asc" }),
    ]).then(([seasonRows, summaryRow, current, past, directory]) => {
      setSeasons(seasonRows || []);
      setSummary(summaryRow);
      if (!nextSeason && summaryRow?.seasonId) setSeasonId(String(summaryRow.seasonId));
      setCaptains(current || []);
      setHistory(past || { content: [], page: 0, totalPages: 0 });
      setSquad(directory?.content || []);
      setReady(true);
      setHistoryBusy(false);
    }).catch((err) => { setError(err.message); setHistoryBusy(false); });
  }

  useEffect(() => { setHistoryBusy(true); load(); }, [seasonId, historyRank, historyPage, historySize, historySort, historyDir, historyCurrent, historyQuery]);
  useEffect(() => {
    const timer = window.setTimeout(() => { setHistoryQuery(historyDraft); setHistoryPage(0); }, 300);
    return () => window.clearTimeout(timer);
  }, [historyDraft]);

  async function assign(rank, playerId, note) {
    setBusy(rank);
    setError("");
    try {
      const response = await api.put(`/api/captains/${rank}`, { playerId, note });
      setToast("");
      setConfirm(null);
      await load();
      return response.data?.data || true;
    } catch (err) {
      setError(err.message);
      return false;
    } finally {
      setBusy("");
    }
  }

  async function clearRank(rank) {
    setBusy(rank);
    setError("");
    try {
      await api.delete(`/api/captains/rank/${rank}`);
      setToast(copy.removed);
      setConfirm(null);
      setPicker(null);
      await load();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy("");
    }
  }

  async function exportPdf(preview) {
    const response = await api.get("/api/captains/export", { params: { season: seasonId || undefined, lang: locale }, responseType: "blob" });
    const blob = new Blob([response.data], { type: "application/pdf" });
    const url = URL.createObjectURL(blob);
    const match = /filename="([^"]+)"/.exec(response.headers["content-disposition"] || "");
    const name = match?.[1] || "Viking_FC_Captains.pdf";
    if (preview) {
      setSheet({ url, name });
      return;
    }
    const link = document.createElement("a");
    link.href = url;
    link.download = name;
    link.click();
    URL.revokeObjectURL(url);
  }

  async function exportHistory() {
    const past = await getData("/api/captains/history", { season: seasonId || undefined, rank: historyRank || undefined, current: historyCurrent || undefined, q: historyQuery || undefined, sort: historySort, dir: historyDir, page: 0, size: 500 });
    const lines = [["player", "rank", "start", "end", "duration", "by", "note"].join(",")];
    for (const item of past?.content || []) {
      const rankLabel = copy[CAPTAIN_RANKS.find(([rank]) => rank === item.rank)?.[2]] || item.rank;
      lines.push([item.fullName, rankLabel, item.startedAt || "", item.endedAt || "", span(item.startedAt, item.endedAt, copy), item.assignedBy || "", item.note || ""].map((value) => `"${String(value).replaceAll('"', "'")}"`).join(","));
    }
    const url = URL.createObjectURL(new Blob([lines.join("\n")], { type: "text/csv" }));
    const link = document.createElement("a");
    link.href = url;
    link.download = "viking-fc-captain-history.csv";
    link.click();
    URL.revokeObjectURL(url);
  }

  function sortHistory(key) {
    setHistoryDir(historySort === key && historyDir === "desc" ? "asc" : "desc");
    setHistorySort(key);
    setHistoryPage(0);
  }

  function dropOn(rank, event) {
    event.preventDefault();
    setOver("");
    if (!canEdit || readOnly) return;
    const playerId = Number(event.dataTransfer.getData("playerId"));
    const player = squad.find((row) => row.id === playerId);
    if (!player || player.status !== "ACTIVE") {
      setError(copy.inactive);
      return;
    }
    const holder = captains.find((item) => item.rank === rank);
    if (holder && holder.playerId !== playerId) {
      setConfirm({ type: "replace", rank, player, holder });
      return;
    }
    assign(rank, playerId);
  }

  const eligible = useMemo(() => squad.filter((row) => {
    const hay = `${row.fullName} ${row.knownAs || ""} ${row.jersey} ${row.email || ""}`.toLowerCase();
    return hay.includes(query.toLowerCase()) && (!line || row.line === line);
  }), [squad, query, line]);
  const pages = Math.max(1, Math.ceil(eligible.length / size));
  const visible = eligible.slice(page * size, page * size + size);
  const counts = LINES.map(([key]) => squad.filter((row) => !key || row.line === key).length);

  return (
    <section className="cap-page">
      <header className="cap-head">
        <div>
          <h1>{copy.title}</h1>
          <p>{copy.sub}{summary?.seasonName ? `, ${summary.seasonName}` : ""}</p>
        </div>
        <div className="cap-actions">
          <label className="vfc-field">
            <span className="vfc-label">{copy.season}</span>
            <select className="vfc-control" value={seasonId} onChange={(event) => { setSeasonId(event.target.value); setHistoryPage(0); }}>
              {seasons.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
            </select>
          </label>
          {canEdit && !readOnly ? <Button variant="primary" onClick={() => setPicker({ rank: "CAPTAIN" })}>{copy.assign}</Button> : null}
          <Button onClick={() => exportPdf(true)}>{copy.preview}</Button>
          <Button onClick={() => exportPdf(false)}>{copy.download}</Button>
        </div>
      </header>
      {toast ? <p className="cap-toast" role="status">{toast}</p> : null}
      {error ? <p className="vfc-error" role="alert">{error}</p> : null}
      {readOnly ? <p className="vfc-hint">{copy.readOnly}</p> : null}
      <Kpis summary={summary} copy={copy} />
      <div className="cap-grid">
        {CAPTAIN_RANKS.map(([rank, code, key], index) => {
          const holder = captains.find((item) => item.rank === rank);
          return (
            <CaptainCard key={rank} rank={rank} code={code} label={copy[key]} holder={holder} copy={copy} locale={locale}
              index={index} canEdit={canEdit && !readOnly} busy={busy === rank} over={over === rank}
              onChange={() => setPicker({ rank, playerId: holder?.playerId })}
              onRemove={() => setConfirm({ type: "remove", rank, holder })}
              onOpen={() => canEdit && !readOnly && setPicker({ rank })}
              onDragOver={(event) => { event.preventDefault(); setOver(rank); }}
              onDragLeave={() => setOver("")}
              onDrop={(event) => dropOn(rank, event)} />
          );
        })}
      </div>
      <HistoryTable copy={copy} locale={locale} history={history} busy={historyBusy && !ready} seasons={seasons} seasonId={seasonId}
        rank={historyRank} currentOnly={historyCurrent} draft={historyDraft} sort={historySort} dir={historyDir}
        page={historyPage} size={historySize}
        onSeason={(value) => { setSeasonId(value); setHistoryPage(0); }}
        onRank={(value) => { setHistoryRank(value); setHistoryPage(0); }}
        onCurrent={(value) => { setHistoryCurrent(value); setHistoryPage(0); }}
        onDraft={setHistoryDraft} onSort={sortHistory} onPage={setHistoryPage} onSize={(value) => { setHistorySize(value); setHistoryPage(0); }}
        onExport={exportHistory} />
      <section className="cap-panel">
        <h2>{copy.eligible}</h2>
        <div className="squad-tools">
          <input value={query} onChange={(event) => { setQuery(event.target.value); setPage(0); }} placeholder={copy.search} aria-label={copy.search} />
          {LINES.map(([key, text], index) => (
            <button key={text} type="button" className={line === key ? "is-on" : ""} onClick={() => { setLine(key); setPage(0); }}>{copy[text]} <small>{counts[index]}</small></button>
          ))}
        </div>
        <div className="cap-players">
          {visible.map((row) => {
            const held = captains.find((item) => item.playerId === row.id);
            const blocked = row.status !== "ACTIVE";
            return (
              <article key={row.id} draggable={!blocked && canEdit && !readOnly} onDragStart={(event) => event.dataTransfer.setData("playerId", String(row.id))} className={blocked ? "is-off" : ""} title={blocked ? copy.inactive : ""}>
                <span className="cap-water">{row.jersey ?? "–"}</span>
                <Avatar name={row.fullName} src={photo(row.photoUrl)} />
                <strong>{row.knownAs || row.fullName}</strong>
                <small>#{row.jersey} · {label(POSITION, row.position, locale)}{row.age != null ? ` · ${row.age}` : ""}</small>
                {held ? <span className="cap-pill">{copy[CAPTAIN_RANKS.find(([rank]) => rank === held.rank)?.[2]]}</span> : null}
                {canEdit && !readOnly && !blocked ? (
                  <Button size="sm" variant="primary" onClick={() => setPicker({ playerId: row.id, rank: held?.rank || "CAPTAIN" })}>{held ? copy.change : copy.assign}</Button>
                ) : null}
              </article>
            );
          })}
        </div>
        <div className="cap-pager">
          <label>{copy.rows}
            <select value={size} onChange={(event) => { setSize(Number(event.target.value)); setPage(0); }}>
              {[8, 16, 25].map((value) => <option key={value} value={value}>{value}</option>)}
            </select>
          </label>
          <Button disabled={page === 0} onClick={() => setPage((value) => value - 1)}>{copy.prev}</Button>
          <Button disabled={page + 1 >= pages} onClick={() => setPage((value) => value + 1)}>{copy.next}</Button>
        </div>
      </section>
      {picker ? (
        <AssignModal copy={copy} locale={locale} picker={picker} captains={captains} squad={squad} busy={Boolean(busy)}
          onClose={() => setPicker(null)} onAssign={assign} onRemove={(rank) => setConfirm({ type: "remove", rank, holder: captains.find((item) => item.rank === rank) })} />
      ) : null}
      {confirm?.type === "remove" ? (
        <Modal size="sm" title={copy.remove} onClose={() => setConfirm(null)} closeLabel={copy.close}
          footer={<><Button variant="ghost" onClick={() => setConfirm(null)}>{copy.cancel}</Button><div className="vfc-foot-right"><Button variant="danger" loading={Boolean(busy)} onClick={() => clearRank(confirm.rank)}>{copy.remove}</Button></div></>}>
          <p>{copy.confirmRemove} {confirm.holder?.fullName}. {copy[CAPTAIN_RANKS.find(([rank]) => rank === confirm.rank)?.[2]]}</p>
        </Modal>
      ) : null}
      {sheet ? (
        <Modal size="lg" title={copy.preview} onClose={() => { URL.revokeObjectURL(sheet.url); setSheet(null); }} closeLabel={copy.close}
          footer={<><Button variant="secondary" onClick={() => { const link = document.createElement("a"); link.href = sheet.url; link.download = sheet.name; link.click(); }}>{copy.download}</Button><div className="vfc-foot-right"><Button variant="primary" onClick={() => { URL.revokeObjectURL(sheet.url); setSheet(null); }}>{copy.done}</Button></div></>}>
          <iframe className="cap-frame" title={sheet.name} src={sheet.url} />
        </Modal>
      ) : null}
      {confirm?.type === "replace" ? (
        <Modal size="sm" title={copy.replace} onClose={() => setConfirm(null)} closeLabel={copy.close}
          footer={<><Button variant="ghost" onClick={() => setConfirm(null)}>{copy.cancel}</Button><div className="vfc-foot-right"><Button variant="primary" loading={Boolean(busy)} onClick={() => assign(confirm.rank, confirm.player.id)}>{copy.replace} {confirm.holder.knownAs || confirm.holder.fullName}?</Button></div></>}>
          <p>{confirm.player.fullName} {copy.willReplace} {confirm.holder.fullName}.</p>
        </Modal>
      ) : null}
    </section>
  );
}

function HistoryTable({ copy, locale, history, busy, seasons, seasonId, rank, currentOnly, draft, sort, dir, page, size, onSeason, onRank, onCurrent, onDraft, onSort, onPage, onSize, onExport }) {
  const rows = history.content || [];
  const total = history.totalElements || 0;
  const from = total === 0 ? 0 : page * size + 1;
  const to = Math.min(total, from + rows.length - 1);
  const pages = history.totalPages || 0;
  const mark = (key) => (sort === key ? (dir === "asc" ? " ↑" : " ↓") : "");
  return (
    <section className="cap-panel">
      <header className="cap-panel-head">
        <h2>{copy.history}</h2>
        <Button onClick={onExport}>{copy.export}</Button>
      </header>
      <div className="cap-filters">
        <label className="vfc-field"><span className="vfc-label">{copy.season}</span>
          <select className="vfc-control" value={seasonId} onChange={(event) => onSeason(event.target.value)}>
            {seasons.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
          </select>
        </label>
        <label className="vfc-field"><span className="vfc-label">{copy.rank}</span>
          <select className="vfc-control" value={rank} onChange={(event) => onRank(event.target.value)}>
            <option value="">{copy.anyRank}</option>
            {CAPTAIN_RANKS.map(([value, , key]) => <option key={value} value={value}>{copy[key]}</option>)}
          </select>
        </label>
        <label className="cap-switch"><input type="checkbox" checked={currentOnly} onChange={(event) => onCurrent(event.target.checked)} /> {copy.currentOnly}</label>
        <input className="vfc-control" value={draft} onChange={(event) => onDraft(event.target.value)} placeholder={copy.search} aria-label={copy.search} />
      </div>
      {busy ? <div className="squad-skeleton" /> : rows.length === 0 ? <p className="squad-empty">{copy.none}</p> : (
        <div className="cap-table-wrap">
          <table className="squad-table cap-table">
            <thead>
              <tr>
                <th><button type="button" onClick={() => onSort("player")}>{copy.player}{mark("player")}</button></th>
                <th><button type="button" onClick={() => onSort("rank")}>{copy.rank}{mark("rank")}</button></th>
                <th><button type="button" onClick={() => onSort("startedAt")}>{copy.started}{mark("startedAt")}</button></th>
                <th><button type="button" onClick={() => onSort("endedAt")}>{copy.ended}{mark("endedAt")}</button></th>
                <th><button type="button" onClick={() => onSort("duration")}>{copy.duration}{mark("duration")}</button></th>
                <th><button type="button" onClick={() => onSort("assignedBy")}>{copy.changedBy}{mark("assignedBy")}</button></th>
                <th>{copy.note}</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((item) => (
                <tr key={item.id} className={item.endedAt ? "" : "is-current"}>
                  <td>
                    <span className="name-cell">
                      <Avatar name={item.fullName} src={photo(item.photoUrl)} />
                      <span className="name-block"><strong>{item.knownAs || item.fullName}</strong><small>{item.fullName}</small></span>
                    </span>
                  </td>
                  <td><span className="cap-pill">{copy[CAPTAIN_RANKS.find(([value]) => value === item.rank)?.[2]] || item.rank}</span></td>
                  <td>{when(item.startedAt, locale)}</td>
                  <td>{item.endedAt ? when(item.endedAt, locale) : <span className="cap-now">{copy.current}</span>}</td>
                  <td>{span(item.startedAt, item.endedAt, copy)}</td>
                  <td>{item.assignedBy || "–"}</td>
                  <td className="cap-note" title={item.note || ""}>{item.note ? item.note : "–"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <div className="cap-pager">
        <span>{copy.showing} {from}-{to} {copy.of} {total}</span>
        <label>{copy.rows}
          <select value={size} onChange={(event) => onSize(Number(event.target.value))}>
            {[5, 10, 25].map((value) => <option key={value} value={value}>{value}</option>)}
          </select>
        </label>
        <Button disabled={page === 0} onClick={() => onPage(page - 1)}>{copy.prev}</Button>
        {Array.from({ length: pages }, (_, index) => (
          <Button key={index} variant={index === page ? "primary" : "secondary"} onClick={() => onPage(index)}>{index + 1}</Button>
        ))}
        <Button disabled={page + 1 >= pages} onClick={() => onPage(page + 1)}>{copy.next}</Button>
      </div>
    </section>
  );
}

function Kpis({ summary, copy }) {
  const assigned = useCount(summary?.assigned || 0);
  const matches = useCount(summary?.matches || 0);
  const contributions = useCount((summary?.goals || 0) + (summary?.assists || 0));
  return (
    <div className="cap-kpis">
      <article><small>{copy.assigned}</small><strong>{assigned} / {summary?.slots || 4}</strong></article>
      <article><small>{copy.avg}</small><strong>{summary?.averageAge == null ? "–" : summary.averageAge}</strong><span>{copy.sample} {summary?.withDateOfBirth || 0}</span></article>
      <article><small>{copy.matches}</small><strong>{matches}</strong></article>
      <article><small>{copy.goals} + {copy.assists}</small><strong>{contributions}</strong></article>
    </div>
  );
}

function CaptainCard({ rank, code, label: rankLabel, holder, copy, locale, index, canEdit, busy, over, onChange, onRemove, onOpen, onDragOver, onDragLeave, onDrop }) {
  const motionProps = still() ? {} : { initial: { opacity: 0, y: 12 }, animate: { opacity: 1, y: 0 }, transition: { delay: index * 0.06 } };
  if (!holder) {
    return (
      <motion.button type="button" className={`cap-card is-empty${over ? " is-over" : ""}`} {...motionProps} onClick={onOpen} onDragOver={onDragOver} onDragLeave={onDragLeave} onDrop={onDrop} disabled={!canEdit}>
        <span className="cap-plus"><Plus /></span>
        <strong>{copy.empty} {rankLabel}</strong>
        <small>{copy.drag}</small>
      </motion.button>
    );
  }
  return (
    <motion.article layout={still() ? false : true} layoutId={still() ? undefined : `captain-${holder.playerId}`} className={`cap-card${rank === "CAPTAIN" ? " is-lead" : ""}${over ? " is-over" : ""}`} {...motionProps}
      tabIndex={0} onKeyDown={(event) => { if (event.key === "Enter") onChange(); }} onDragOver={onDragOver} onDragLeave={onDragLeave} onDrop={onDrop}>
      <header>
        <span className="cap-pill">{rankLabel}</span>
        <b>{code}</b>
      </header>
      <div className="cap-face">
        <span className="cap-water">{holder.jerseyNumber ?? "–"}</span>
        <Avatar name={holder.fullName} src={photo(holder.photoUrl)} size="112" />
      </div>
      <h3>{holder.knownAs || holder.fullName}</h3>
      <p>{holder.fullName}</p>
      <div className="cap-badges">
        <span>{label(POSITION, holder.positionCode, locale) || holder.position}</span>
        {holder.age != null ? <span>{holder.age}</span> : null}
        {holder.status ? <span>{label(STATUS, holder.status, locale)}</span> : null}
      </div>
      <small>{copy.since} {when(holder.startedAt, locale)} · {holder.seasonName}</small>
      <dl>
        <div><dt>{copy.matches}</dt><dd>{holder.matches ?? "–"}</dd></div>
        <div><dt>{copy.goals}</dt><dd>{holder.goals ?? "–"}</dd></div>
        <div><dt>{copy.assists}</dt><dd>{holder.assists ?? "–"}</dd></div>
      </dl>
      <small>{copy.asCaptain}: {holder.matchesAsCaptain ?? "–"}</small>
      {canEdit ? (
        <div className="cap-card-actions">
          <Button icon={<ArrowLeftRight size={16} />} loading={busy} title={copy.change} onClick={onChange}>{copy.change}</Button>
          <Button variant="danger" icon={<Trash2 size={16} />} title={copy.remove} onClick={onRemove}>{copy.remove}</Button>
        </div>
      ) : null}
    </motion.article>
  );
}

function AssignModal({ copy, locale, picker, captains, squad, busy, onClose, onAssign, onRemove }) {
  const preset = squad.find((row) => row.id === picker.playerId);
  const [query, setQuery] = useState("");
  const [player, setPlayer] = useState(preset || null);
  const [rank, setRank] = useState(picker.rank || "CAPTAIN");
  const [note, setNote] = useState("");
  const [done, setDone] = useState(null);
  const [hold, setHold] = useState(false);
  const holder = captains.find((item) => item.rank === rank);
  const held = captains.find((item) => item.playerId === player?.id);
  const choices = squad.filter((row) => `${row.fullName} ${row.jersey}`.toLowerCase().includes(query.toLowerCase())).slice(0, 6);
  const rankIndex = CAPTAIN_RANKS.findIndex(([key]) => key === rank);

  function move(step) {
    const next = CAPTAIN_RANKS[(rankIndex + step + CAPTAIN_RANKS.length) % CAPTAIN_RANKS.length][0];
    setRank(next);
  }

  return (
    <Modal size="md" title={done ? copy.saved : copy.assign} subtitle={done ? "" : player ? `${player.fullName} · #${player.jersey}` : copy.search} onClose={onClose} closeLabel={copy.close}
      footer={done ? (
        <div className="cap-success-actions" onMouseEnter={() => setHold(true)} onMouseLeave={() => setHold(false)}>
          <Button variant="secondary" onClick={() => { setDone(null); setPlayer(null); setQuery(""); setNote(""); }}>{copy.another}</Button>
          <Button variant="primary" onClick={onClose}>{copy.done}</Button>
        </div>
      ) : (
        <>
          <Button variant="ghost" onClick={onClose}>{copy.cancel}</Button>
          <div className="vfc-foot-right">
            {held ? <Button variant="danger" onClick={() => onRemove(held.rank)}>{copy.removeRank}</Button> : null}
            <Button variant="primary" loading={busy} disabled={!player || player.status !== "ACTIVE"} onClick={async () => { const saved = await onAssign(rank, player.id, note); if (saved) setDone({ ...player, ...saved, rank }); }}>
              {copy.assignAs} {copy[CAPTAIN_RANKS.find(([key]) => key === rank)?.[2]]}
            </Button>
          </div>
        </>
      )}>
      {done ? <AssignSuccess copy={copy} locale={locale} result={done} hold={hold} setHold={setHold} onDone={onClose} /> : (
        <>
          {player ? (
            <div className="name-cell">
              <Avatar name={player.fullName} src={photo(player.photoUrl)} />
              <span className="name-block"><strong>{player.fullName}</strong><small>#{player.jersey} · {label(POSITION, player.position, locale)}</small></span>
            </div>
          ) : (
            <Field label={copy.search}><TextInput value={query} onChange={(event) => setQuery(event.target.value)} /></Field>
          )}
          {!player ? choices.map((row) => (
            <button key={row.id} type="button" className="cap-pick" disabled={row.status !== "ACTIVE"} title={row.status !== "ACTIVE" ? copy.inactive : ""} onClick={() => setPlayer(row)}>
              <Avatar name={row.fullName} src={photo(row.photoUrl)} /> #{row.jersey} {row.fullName}
            </button>
          )) : null}
          <div className="vfc-slots" onKeyDown={(event) => { if (event.key === "ArrowRight") move(1); if (event.key === "ArrowLeft") move(-1); }}>
            {CAPTAIN_RANKS.map(([key, code, name]) => {
              const current = captains.find((item) => item.rank === key);
              return (
                <button key={key} type="button" className={`vfc-slot${rank === key ? " is-on" : ""}`} onClick={() => setRank(key)}>
                  <Avatar name={current?.fullName || code} src={photo(current?.photoUrl)} />
                  <span><b>{copy[name]}</b><br />{current ? current.knownAs || current.fullName : copy.free}</span>
                </button>
              );
            })}
          </div>
          {holder && holder.playerId !== player?.id ? <p className="vfc-error">{copy.replace} {holder.fullName}? {player?.fullName} {copy.willReplace} {holder.knownAs || holder.fullName}.</p> : null}
          <Field label={copy.note}><TextArea value={note} onChange={(event) => setNote(event.target.value)} />          </Field>
        </>
      )}
    </Modal>
  );
}

function AssignSuccess({ copy, locale, result, hold, setHold, onDone }) {
  const [progress, setProgress] = useState(0);
  const close = useRef(onDone);
  close.current = onDone;
  const rankLabel = copy[CAPTAIN_RANKS.find(([rank]) => rank === result.rank)?.[2]] || result.rank;
  useEffect(() => {
    if (hold || still()) return undefined;
    const started = performance.now() - progress * 3000;
    let frame = 0;
    const tick = (now) => {
      const value = Math.min(1, (now - started) / 3000);
      setProgress(value);
      if (value >= 1) close.current();
      else frame = requestAnimationFrame(tick);
    };
    frame = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(frame);
  }, [hold]);
  const motionProps = still() ? {} : { initial: { opacity: 0, scale: 0.92, y: 12 }, animate: { opacity: 1, scale: 1, y: 0 }, transition: { type: "spring", stiffness: 280, damping: 22 } };
  return (
    <motion.div className="cap-success" role="status" onMouseEnter={() => setHold(true)} onMouseLeave={() => setHold(false)} {...motionProps}>
      {!still() ? <span className="cap-confetti" aria-hidden="true" /> : null}
      <span className="cap-ring" aria-hidden="true">
        <svg viewBox="0 0 96 96" width="96" height="96">
          <circle cx="48" cy="48" r="40" fill="#a3283a" />
          <motion.path d="M30 49 l12 12 l24-26" fill="none" stroke="#fff" strokeWidth="6" strokeLinecap="round" strokeLinejoin="round"
            initial={still() ? false : { pathLength: 0 }} animate={{ pathLength: 1 }} transition={{ duration: 0.5, ease: "easeOut" }} />
        </svg>
      </span>
      <h3>{copy.saved}</h3>
      <p><Avatar name={result.fullName} src={photo(result.photoUrl)} /> <strong>{result.fullName}</strong> {copy.isNow} {rankLabel}</p>
      <p className="cap-meta"><b>#{result.jerseyNumber ?? result.jersey}</b> <span className="cap-pill">{rankLabel}</span></p>
      <small>{result.seasonName || ""} · {when(result.startedAt, locale)}</small>
      <i className="cap-progress" style={{ transform: `scaleX(${still() ? 0 : progress})` }} />
    </motion.div>
  );
}
