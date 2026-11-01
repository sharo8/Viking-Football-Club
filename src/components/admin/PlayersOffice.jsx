"use client";

import { useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { BarChart3, Eye, LayoutGrid, Mail, Pencil, Shield, Table2, Trash2 } from "lucide-react";
import { api, getData } from "@/services/api";
import { useAuth } from "@/components/providers/AuthProvider";
import { useLocale } from "@/components/providers/LocaleProvider";
import { isExecutive } from "@/lib/admin/access";
import { COPY, POSITION, RANKS, STATUS, label, money } from "./playersCopy";
import { PlayerForm } from "./PlayerForm";
import { PlayerAnalytics } from "./PlayerAnalytics";
import { CaptainModal, ColumnsMenu, ConfirmModal, MessageModal, ProfileModal } from "./PlayerDialogs";
import { ImportWizard } from "./ImportWizard";
import { Avatar, Button, IconButton } from "@/ui/kit";

export function PlayersOffice() {
  const { locale } = useLocale();
  const { user } = useAuth();
  const router = useRouter();
  const params = useSearchParams();
  const copy = COPY[locale] || COPY.en;
  const executive = user ? isExecutive(user.role) : false;
  const canEdit = executive || user?.role === "ASSISTANT_MANAGER";
  const canCreate = executive;
  const canCaptain = canEdit || user?.role === "HEAD_COACH";
  const page = Number(params.get("page") || 0);
  const size = Number(params.get("size") || 8);
  const line = params.get("line") || "";
  const view = params.get("view") || "table";
  const q = params.get("q") || "";
  const status = params.get("status") || "";
  const position = params.get("position") || "";
  const birthday = params.get("birthday") || "";
  const missing = params.get("missing") === "1";
  const expiring = params.get("expiring") === "1";
  const archived = params.get("archived") === "1";
  const sort = params.get("sort") || "jerseyNumber";
  const dir = params.get("dir") || "asc";
  const [draft, setDraft] = useState(q);
  const [data, setData] = useState(null);
  const [counts, setCounts] = useState({});
  const [captains, setCaptains] = useState([]);
  const [analytics, setAnalytics] = useState(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(true);
  const [form, setForm] = useState(null);
  const [viewer, setViewer] = useState(null);
  const [mail, setMail] = useState(null);
  const [confirm, setConfirm] = useState(null);
  const [notice, setNotice] = useState("");
  const [extra, setExtra] = useState({ nationality: false, phone: false, email: false, foot: false, pay: false });
  const [picker, setPicker] = useState(null);
  const [importer, setImporter] = useState(false);
  const [squad, setSquad] = useState([]);

  function setParam(patch) {
    const next = new URLSearchParams(params.toString());
    for (const [key, value] of Object.entries(patch)) {
      if (value === "" || value == null || value === false) next.delete(key);
      else next.set(key, String(value));
    }
    if (!("page" in patch)) next.set("page", "0");
    const main = document.querySelector(".admin-main");
    const top = main ? main.scrollTop : 0;
    router.replace(`/admin/players?${next.toString()}`, { scroll: false });
    const hold = () => { if (main) main.scrollTop = top; };
    requestAnimationFrame(hold);
    window.setTimeout(hold, 60);
  }

  useEffect(() => { setDraft(q); }, [q]);
  useEffect(() => {
    const timer = window.setTimeout(() => { if (draft !== q) setParam({ q: draft }); }, 300);
    return () => window.clearTimeout(timer);
  }, [draft]);

  function load() {
    setBusy(true);
    setError("");
    Promise.all([
      getData("/api/players/directory", { q, line, position, status, birthday, missing, expiring, archived, sort, dir, page, size }),
      getData("/api/players/counts", { q, status, birthday, missing }),
      getData("/api/captains"),
      getData("/api/players/analytics"),
    ]).then(([directory, chipCounts, captainRows, stats]) => {
      setData(directory);
      setCounts(chipCounts);
      setCaptains(captainRows || []);
      setAnalytics(stats);
    }).catch((err) => setError(err.message)).finally(() => setBusy(false));
  }

  useEffect(() => { load(); }, [q, line, position, status, birthday, missing, expiring, archived, sort, dir, page, size]);

  const rows = data?.content || [];
  const total = data?.totalElements || 0;
  const from = total === 0 ? 0 : page * size + 1;
  const to = Math.min(total, (page + 1) * size);
  const pages = data?.totalPages || 0;

  function toast(message) {
    setNotice(message);
    window.setTimeout(() => setNotice(""), 4000);
  }

  useEffect(() => {
    if (!picker) return undefined;
    getData("/api/players/directory", { size: 100 }).then((page) => setSquad(page.content || [])).catch(() => {});
    return undefined;
  }, [picker]);

  async function archive(row, body) {
    await api.post(`/api/players/${row.id}/former`, body);
    setConfirm(null);
    load();
  }

  async function restore(row) {
    await api.post(`/api/players/${row.id}/restore`);
    load();
  }

  async function assign(rank, playerId) {
    const current = captains.find((item) => item.rank === rank);
    if (current && !window.confirm(`${copy.replace} ${current.fullName}?`)) return;
    await api.put(`/api/captains/${rank}`, { playerId });
    setPicker(null);
    load();
  }

  async function clearRank(rank) {
    if (!window.confirm(copy.remove)) return;
    await api.delete(`/api/captains/rank/${rank}`);
    load();
  }

  function sortBy(key) {
    setParam({ sort: key, dir: sort === key && dir === "asc" ? "desc" : "asc", page });
  }

  async function importCsv(event) {
    const file = event.target.files?.[0];
    if (!file) return;
    const preview = new FormData();
    preview.append("file", file);
    const look = await api.post("/api/players/import", preview);
    if (!window.confirm(`${look.data.data.ready} rows. Import them?`)) return;
    const body = new FormData();
    body.append("file", file);
    await api.post("/api/players/import?confirm=true", body);
    toast(copy.saved);
    load();
  }

  async function exportFile(format) {
    const response = await api.get("/api/players/export", { params: { format, lang: locale }, responseType: "blob" });
    const url = URL.createObjectURL(response.data);
    const link = document.createElement("a");
    link.href = url;
    link.download = `viking-fc-players.${format === "pdf" ? "pdf" : format === "xlsx" ? "xlsx" : "csv"}`;
    link.click();
    URL.revokeObjectURL(url);
  }

  return (
    <section className={`squad${view === "analytics" ? " is-analytics" : ""}`}>
      <header className="squad-head">
        <div>
          <h1>{copy.title} <span className="squad-count">{total}</span></h1>
          <p>{copy.sub}</p>
        </div>
        <div className="squad-head-actions">
          {canCreate ? <Button variant="primary" onClick={() => setForm({ player: null, step: 0 })}>+ {copy.add}</Button> : null}
          <select className="vfc-control" aria-label={copy.export} defaultValue="" onChange={(event) => { if (event.target.value) { exportFile(event.target.value); event.target.value = ""; } }}>
            <option value="">{copy.export}</option>
            <option value="xlsx">Excel</option>
            <option value="pdf">PDF</option>
            <option value="csv">CSV</option>
          </select>
          {canCreate ? <Button onClick={() => setImporter(true)}>Import</Button> : null}
        </div>
      </header>
      {analytics ? (
        <div className="kpi-strip">
          <article><small>{copy.squad}</small><strong>{analytics.count}</strong></article>
          <article><small>{copy.avg}</small><strong>{analytics.averageAge == null ? "–" : Number(analytics.averageAge).toFixed(1)}</strong></article>
          <article><small>{copy.range}</small><strong>{analytics.youngest ?? "–"} / {analytics.oldest ?? "–"}</strong></article>
          <article><small>{copy.expiringKpi}</small><strong>{counts.expiring ?? 0}</strong></article>
          <article><small>{copy.complete}</small><strong>{analytics.completeness ? `${analytics.completeness.percent}%` : "–"}</strong></article>
        </div>
      ) : null}
      <div className="captain-bar">
        {RANKS.map(([rank, badge]) => {
          const holder = captains.find((item) => item.rank === rank);
          return (
            <article key={rank}>
              <b>{badge}</b>
              {holder ? (
                <>
                  <span>{holder.knownAs || holder.fullName}</span>
                  <small>#{holder.jerseyNumber}</small>
                  {canCaptain ? <button type="button" onClick={() => clearRank(rank)}>{copy.remove}</button> : null}
                </>
              ) : canCaptain ? <button type="button" onClick={() => setPicker(rank)}>+ {copy.assign}</button> : <span>–</span>}
            </article>
          );
        })}
      </div>
      <div className="squad-tools">
        {view !== "analytics" ? <input className="table-only" value={draft} onChange={(event) => setDraft(event.target.value)} placeholder={copy.search} aria-label={copy.search} /> : null}
        {[["", copy.all, counts.all], ["Goalkeepers", copy.gk, counts.Goalkeepers], ["Defenders", copy.def, counts.Defenders], ["Midfielders", copy.mid, counts.Midfielders], ["Attackers", copy.att, counts.Attackers]].map(([key, text, count]) => (
          <button key={key || "all"} type="button" className={line === key ? "is-on" : ""} onClick={() => setParam({ line: key })}>{text} <small>{count ?? 0}</small></button>
        ))}
        <select value={position} onChange={(event) => setParam({ position: event.target.value })} aria-label={copy.position}>
          <option value="">{copy.anyPosition}</option>
          {Object.keys(POSITION).map((key) => <option key={key} value={key}>{label(POSITION, key, locale)}</option>)}
        </select>
        {view !== "analytics" ? <>
        <select value={status} onChange={(event) => setParam({ status: event.target.value })} aria-label={copy.status}>
          <option value="">{copy.anyStatus}</option>
          {Object.keys(STATUS).map((key) => <option key={key} value={key}>{label(STATUS, key, locale)}</option>)}
        </select>
        <select value={birthday} onChange={(event) => setParam({ birthday: event.target.value })} aria-label={copy.birthdays}>
          <option value="">{copy.birthdays}</option>
          <option value="today">{copy.today}</option>
          <option value="week">{copy.week}</option>
          <option value="month">{copy.month}</option>
          <option value="30">{copy.next30}</option>
        </select>
        <button type="button" className={expiring ? "is-on" : ""} onClick={() => setParam({ expiring: expiring ? "" : "1" })}>{copy.expiring}</button>
        <button type="button" className={missing ? "is-on" : ""} onClick={() => setParam({ missing: missing ? "" : "1" })}>{copy.missing}</button>
        <button type="button" className={archived ? "is-on" : ""} onClick={() => setParam({ archived: archived ? "" : "1" })}>{copy.archived}</button>
        <ColumnsMenu copy={copy} extra={extra} executive={executive} onToggle={(key) => setExtra((current) => ({ ...current, [key]: !current[key] }))} />
        </> : null}
        <div className="segmented" role="group">
          <button type="button" className={view === "table" ? "is-on" : ""} onClick={() => setParam({ view: "table", page })}><Table2 size={16} /> {copy.table}</button>
          <button type="button" className={view === "cards" ? "is-on" : ""} onClick={() => setParam({ view: "cards", page })}><LayoutGrid size={16} /> {copy.cards}</button>
          <button type="button" className={view === "analytics" ? "is-on" : ""} onClick={() => setParam({ view: "analytics", page })}><BarChart3 size={16} /> {copy.analytics}</button>
        </div>
      </div>
      {error ? <p className="squad-error">{error} <button type="button" onClick={load}>{copy.retry}</button></p> : null}
      {busy && !data ? <div className="squad-skeleton" /> : null}
      {!busy && view !== "analytics" && rows.length === 0 ? <p className="squad-empty">{copy.empty}</p> : null}
      {view === "analytics" ? <PlayerAnalytics data={analytics} copy={copy} locale={locale} executive={executive} onComplete={() => setParam({ view: "table", missing: "1" })} /> : null}
      {view === "cards" ? (
        <div className="squad-cards">
          {rows.map((row) => (
            <article key={row.id}>
              <span className="squad-avatar">{row.photoUrl ? <img src={row.photoUrl} alt="" /> : (row.knownAs || "?").slice(0, 1)}</span>
              <strong>#{row.jersey} {row.fullName}</strong>
              {row.captain ? <em>{badge(row.captain)}</em> : null}
              <p>{label(POSITION, row.position, locale)} · {row.age ?? copy.addLink}</p>
              <p>{label(STATUS, row.status, locale)}</p>
              <Actions row={row} copy={copy} canEdit={canEdit} canCaptain={canCaptain} archived={archived} onView={setViewer} onEdit={(item, step) => setForm({ player: item, step })} onMail={setMail} onCaptain={() => setPicker({ playerId: row.id })} onArchive={setConfirm} onRestore={restore} />
            </article>
          ))}
        </div>
      ) : null}
      {view === "table" && rows.length ? (
        <div className="squad-table-wrap">
          <table className="squad-table">
            <thead>
              <tr>
                <th className="sticky-name">{copy.name}</th>
                <th><button type="button" onClick={() => sortBy("jerseyNumber")}>{copy.jersey}</button></th>
                <th><button type="button" onClick={() => sortBy("position")}>{copy.position}</button></th>
                <th><button type="button" onClick={() => sortBy("age")}>{copy.age}</button></th>
                <th><button type="button" onClick={() => sortBy("contract")}>{copy.contract}</button></th>
                <th>{copy.status}</th>
                <th>{copy.emergency}</th>
                {extra.nationality ? <th>{copy.nationality}</th> : null}
                {extra.phone ? <th>{copy.phone}</th> : null}
                {extra.email ? <th>{copy.email}</th> : null}
                {extra.pay && executive ? <th>{copy.pay}</th> : null}
                <th className="sticky-act">{copy.actions}</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.id} className={row.daysUntilBirthday === 0 ? "is-birthday" : ""}>
                  <td className="sticky-name">
                    <span className="name-cell">
                      <Avatar name={row.fullName} src={row.photoUrl} />
                      <span className="name-block">
                        <strong>{row.fullName} {row.captain ? <b className="captain">{badge(row.captain)}</b> : null} {row.missingCount > 0 ? <em className="vfc-chip">{copy.incomplete} · {row.missingCount}</em> : null}</strong>
                        <small>{row.knownAs}{row.duplicate ? <button type="button" className="dup" onClick={() => setForm({ player: row, step: 0 })}> {copy.duplicate}</button> : null}</small>
                      </span>
                    </span>
                  </td>
                  <td>{row.jersey ?? "–"}</td>
                  <td>{label(POSITION, row.position, locale)}</td>
                  <td>{row.age ?? <button type="button" className="add-link" onClick={() => setForm({ player: row, step: 0 })}>{copy.addLink}</button>}{row.dateOfBirth ? <small> {row.dateOfBirth}</small> : null}</td>
                  <td>{row.contractEnd || "–"} {row.expiringSoon ? <em>{copy.expiring}</em> : null}</td>
                  <td>{label(STATUS, row.status, locale)}</td>
                  <td>{row.emergencyName ? `${row.emergencyName} · ${row.emergencyPhone || ""}` : canEdit ? <button type="button" className="add-link" onClick={() => setForm({ player: row, step: 1 })}>{copy.addLink}</button> : "–"}</td>
                  {extra.nationality ? <td>{row.nationality || "–"}</td> : null}
                  {extra.phone ? <td>{row.phone || "–"}</td> : null}
                  {extra.email ? <td>{row.email || <button type="button" className="add-link" onClick={() => setForm({ player: row, step: 1 })}>{copy.addLink}</button>}</td> : null}
                  {extra.pay && executive ? <td>{money(row.seasonalPay)}</td> : null}
                  <td className="sticky-act">
                    <Actions row={row} copy={copy} canEdit={canEdit} canCaptain={canCaptain} archived={archived} onView={setViewer} onEdit={(item, step) => setForm({ player: item, step })} onMail={setMail} onCaptain={() => setPicker({ playerId: row.id })} onArchive={setConfirm} onRestore={restore} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
      {view !== "analytics" ? (
        <footer className="squad-pager">
          <span>{copy.showing} {from}–{to} {copy.of} {total}</span>
          <label>{copy.rows}
            <select value={size} onChange={(event) => setParam({ size: event.target.value, page: 0 })}>
              {[8, 16, 25, 50].map((value) => <option key={value} value={value}>{value}</option>)}
            </select>
          </label>
          <button type="button" disabled={page === 0} onMouseDown={(event) => event.preventDefault()} onClick={() => setParam({ page: page - 1 })}>{copy.prev}</button>
          <PageButtons page={page} pages={pages} onPick={(value) => setParam({ page: value })} />
          <button type="button" disabled={page + 1 >= pages} onMouseDown={(event) => event.preventDefault()} onClick={() => setParam({ page: page + 1 })}>{copy.next}</button>
        </footer>
      ) : null}
      {form ? <PlayerForm copy={copy} locale={locale} executive={executive} player={form.player} initialStep={form.step || 0} onClose={() => setForm(null)} onSaved={() => { setForm(null); toast(copy.saved); load(); }} /> : null}
      {viewer ? <ProfileModal row={viewer} copy={copy} locale={locale} executive={executive} onClose={() => setViewer(null)} onEdit={(row) => { setViewer(null); setForm({ player: row, step: 0 }); }} onMail={(row) => { setViewer(null); setMail(row); }} onCaptain={(row) => { setViewer(null); setPicker({ playerId: row.id }); }} onArchive={(row) => { setViewer(null); setConfirm(row); }} /> : null}
      {mail ? <MessageModal player={mail} copy={copy} locale={locale} userEmail={user?.email} onClose={() => setMail(null)} onSent={() => { setMail(null); toast(copy.sent); }} /> : null}
      {confirm ? <ConfirmModal row={confirm} copy={copy} onClose={() => setConfirm(null)} onArchive={archive} /> : null}
      {picker ? <CaptainModal picker={picker} captains={captains} squad={squad.length ? squad : rows} copy={copy} onClose={() => setPicker(null)} onAssign={(rank, playerId) => assign(rank, playerId)} onRemove={clearRank} /> : null}
      {importer ? <ImportWizard copy={copy} onClose={() => setImporter(false)} onDone={load} /> : null}
      {notice ? <p className="toast" role="status">{notice}</p> : null}
    </section>
  );
}

function badge(rank) {
  return { CAPTAIN: "C", VICE_CAPTAIN: "VC", THIRD_CAPTAIN: "3C", FOURTH_CAPTAIN: "4C" }[rank] || rank;
}

function Actions({ row, copy, canEdit, canCaptain, archived, onView, onEdit, onMail, onCaptain, onArchive, onRestore }) {
  return (
    <div className="row-icons">
      <IconButton label={copy.view} onClick={() => onView(row)}><Eye size={16} /></IconButton>
      {canEdit ? <IconButton label={copy.edit} onClick={() => onEdit(row, 0)}><Pencil size={16} /></IconButton> : null}
      <IconButton label={row.email ? copy.message : copy.noEmail} disabled={!row.email} onClick={() => onMail(row)}><Mail size={16} /></IconButton>
      {canCaptain && !archived ? <IconButton label={copy.captain} onClick={() => onCaptain(row)}><Shield size={16} /></IconButton> : null}
      {canEdit && !archived ? <IconButton label={copy.moveFormer} onClick={() => onArchive(row)}><Trash2 size={16} /></IconButton> : null}
      {canEdit && archived ? <Button size="sm" onClick={() => onRestore(row)}>{copy.restore}</Button> : null}
    </div>
  );
}

function PageButtons({ page, pages, onPick }) {
  const items = [];
  for (let index = 0; index < pages; index += 1) {
    if (index === 0 || index === pages - 1 || Math.abs(index - page) <= 1) items.push(index);
    else if (items[items.length - 1] !== "…") items.push("…");
  }
  return items.map((item, index) => item === "…" ? <span key={`gap-${index}`}>…</span> : (
    <button key={item} type="button" className={item === page ? "is-on" : ""} onClick={() => onPick(item)}>{item + 1}</button>
  ));
}

