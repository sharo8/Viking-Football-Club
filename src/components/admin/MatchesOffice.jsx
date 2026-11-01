"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { Eye, Pencil, Plus, Trash2 } from "lucide-react";
import { api, getData } from "@/services/api";
import { useLocale } from "@/components/providers/LocaleProvider";
import { localizedName } from "./TranslatedName";
import { Badge, Button, Field, IconButton, Modal, SelectInput, TextInput } from "@/ui/kit";
import "./matches.css";

function when(iso) {
  if (!iso) return "";
  return new Intl.DateTimeFormat("en-GB", { timeZone: "Africa/Kigali", weekday: "short", day: "numeric", month: "short", hour: "2-digit", minute: "2-digit", hourCycle: "h23" }).format(new Date(iso));
}

function relative(iso) {
  const hours = Math.round((new Date(iso).getTime() - Date.now()) / 3600000);
  if (hours < 0) return "Played";
  if (hours < 24) return `In ${hours}h`;
  return `In ${Math.round(hours / 24)} days`;
}

function kigali(value) {
  if (!value) return null;
  return value.length === 16 ? `${value}:00+02:00` : value;
}

async function send(method, url, data) {
  const response = await api({ method, url, data });
  if (!response.data?.success) throw new Error(response.data?.message || "Request failed");
  return response.data.data;
}

export function MatchesOffice() {
  const { locale } = useLocale();
  const [rows, setRows] = useState([]);
  const [competitions, setCompetitions] = useState([]);
  const [opponents, setOpponents] = useState([]);
  const [players, setPlayers] = useState([]);
  const [query, setQuery] = useState("");
  const [type, setType] = useState("");
  const [status, setStatus] = useState("");
  const [home, setHome] = useState("");
  const [attention, setAttention] = useState(false);
  const [page, setPage] = useState(0);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [editor, setEditor] = useState(null);
  const [viewer, setViewer] = useState(null);
  const [result, setResult] = useState(null);
  const [remove, setRemove] = useState(null);
  const [menu, setMenu] = useState(null);

  async function load() {
    setLoading(true);
    try {
      const [matches, comps, teams, squad] = await Promise.all([
        getData("/api/matches", { q: query || undefined, type: type || undefined, status: status || undefined, home: home === "" ? undefined : home === "home", needsAttention: attention || undefined }),
        getData("/api/competitions"),
        getData("/api/opponents"),
        getData("/api/players"),
      ]);
      setRows(matches || []);
      setCompetitions(comps || []);
      setOpponents(teams || []);
      setPlayers(squad || []);
      setError("");
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, [query, type, status, home, attention]);

  const counts = useMemo(() => ({
    all: rows.length,
    LEAGUE: rows.filter((row) => row.competitionType === "LEAGUE").length,
    CUP: rows.filter((row) => row.competitionType === "CUP").length,
    FRIENDLY: rows.filter((row) => row.competitionType === "FRIENDLY").length,
    OTHER: rows.filter((row) => row.competitionType === "OTHER").length,
  }), [rows]);
  const pageSize = 8;
  const slice = rows.slice(page * pageSize, page * pageSize + pageSize);
  const pages = Math.max(1, Math.ceil(rows.length / pageSize));

  async function exportFile(format) {
    if (format === "csv") {
      exportCsv();
      return;
    }
    const lang = document.documentElement.lang || "en";
    const response = await api.get("/api/matches/export", { params: { format, lang, q: query || undefined }, responseType: "blob" });
    const url = URL.createObjectURL(response.data);
    const link = document.createElement("a");
    link.href = url;
    link.download = `viking-fc-matches.${format === "pdf" ? "pdf" : "xlsx"}`;
    link.click();
    URL.revokeObjectURL(url);
  }

  function exportCsv() {
    const header = ["Date", "Competition", "Opponent", "Home", "Venue", "Result", "Status", "Lineup"];
    const body = rows.map((row) => [when(row.kickoff), row.competition, row.opponent, row.home ? "Home" : "Away", row.venue || "", `${row.goalsFor} - ${row.goalsAgainst}`, row.status, row.lineupStatus]);
    const csv = [header, ...body].map((line) => line.map((cell) => `"${String(cell ?? "").replaceAll('"', '""')}"`).join(",")).join("\n");
    const blob = new Blob([csv], { type: "text/csv" });
    const link = document.createElement("a");
    link.href = URL.createObjectURL(blob);
    link.download = "viking-fc-matches.csv";
    link.click();
  }

  return (
    <section className="matches-page">
      <header className="matches-head">
        <div>
          <h1>Matches <Badge tone="bad">{rows.length}</Badge></h1>
          <p>League, cup and friendly fixtures for this season.</p>
        </div>
        <div className="matches-actions">
          <TextInput aria-label="Search matches" placeholder="Search opponent or competition" value={query} onChange={(event) => { setQuery(event.target.value); setPage(0); }} />
          <Link href="/admin/calendar"><Button>Calendar</Button></Link>
          <select className="vfc-control" aria-label="Export" defaultValue="" onChange={(event) => { if (event.target.value) { exportFile(event.target.value); event.target.value = ""; } }}>
            <option value="">Export</option>
            <option value="xlsx">Excel</option>
            <option value="pdf">PDF</option>
            <option value="csv">CSV</option>
          </select>
          <Button variant="primary" icon={<Plus size={16} />} onClick={() => setEditor({ competitionId: competitions[0]?.id || "", opponentId: opponents[0]?.id || "", kickoff: "", venue: "", homeMatch: true, matchday: "", referee: "", stage: "" })}>Add match</Button>
        </div>
      </header>
      <div className="match-filters">
        {["", "LEAGUE", "CUP", "FRIENDLY", "OTHER"].map((key) => (
          <button key={key || "all"} type="button" className={type === key ? "on" : ""} onClick={() => { setType(key); setPage(0); }}>{key || "All"} {counts[key || "all"] ?? 0}</button>
        ))}
        <SelectInput aria-label="Status" value={status} onChange={(event) => setStatus(event.target.value)}>
          <option value="">Any status</option>
          {["SCHEDULED", "LIVE", "FINISHED", "POSTPONED", "CANCELLED"].map((item) => <option key={item}>{item}</option>)}
        </SelectInput>
        <SelectInput aria-label="Home or away" value={home} onChange={(event) => setHome(event.target.value)}>
          <option value="">Home and away</option>
          <option value="home">Home</option>
          <option value="away">Away</option>
        </SelectInput>
        <label className="tool-switch"><input type="checkbox" checked={attention} onChange={(event) => setAttention(event.target.checked)} /> Needs attention</label>
      </div>
      {error ? <p className="banner" role="alert">{error}</p> : null}
      {loading ? <div className="skel" /> : null}
      {!loading && !rows.length ? <p className="matches-empty">No matches yet. Add the first one.</p> : null}
      {slice.length ? (
        <div className="admin-table-wrap">
          <table className="admin-table match-grid">
            <thead>
              <tr>
                <th>Date and time</th><th>Competition</th><th>Opponent</th><th>Side</th><th>Venue</th><th>Result</th><th>Status</th><th>Lineup</th><th>Call-ups</th><th>Alerts</th><th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {slice.map((row) => (
                <tr key={row.id}>
                  <td>{when(row.kickoff)}<br /><Badge>{relative(row.kickoff)}</Badge></td>
                  <td><Badge tone={row.official ? "bad" : "neutral"}>{row.competition}</Badge></td>
                  <td>{row.opponent}</td>
                  <td><Badge tone={row.home ? "good" : "warn"}>{row.home ? "Home" : "Away"}</Badge></td>
                  <td>{row.venue || "—"}</td>
                  <td>{row.status === "FINISHED" || row.status === "LIVE" ? `${row.goalsFor} – ${row.goalsAgainst}` : when(row.kickoff).split(", ").pop()}</td>
                  <td><Badge>{row.status}</Badge></td>
                  <td><Badge tone={row.lineupStatus === "PUBLISHED" ? "good" : "warn"}>{row.lineupStatus === "NONE" ? "No lineup" : row.lineupStatus}</Badge></td>
                  <td>{row.calledCount || 0} called, {row.confirmedCount || 0} confirmed</td>
                  <td>{(row.alerts || []).length ? <span title={(row.alerts || []).join(", ")}>{row.alerts.length}</span> : "—"}</td>
                  <td className="row-actions">
                    <IconButton label="View" onClick={() => setViewer(row)}><Eye size={16} /></IconButton>
                    <IconButton label="Edit" onClick={() => setEditor({ ...row, kickoff: "", competitionId: row.competitionId || "", opponentId: row.opponentId })}><Pencil size={16} /></IconButton>
                    <IconButton label="Delete" onClick={() => setRemove(row)}><Trash2 size={16} /></IconButton>
                    <button type="button" className="vfc-icon" aria-label="More" onClick={() => setMenu(menu === row.id ? null : row.id)}>⋯</button>
                    {menu === row.id ? (
                      <div className="match-menu">
                        <Link href={`/admin/lineup?matchId=${row.id}`}>Open lineup</Link>
                        <Link href={`/admin/lineup?matchId=${row.id}&tab=callups`}>Call-ups</Link>
                        <button type="button" onClick={() => { setResult(row); setMenu(null); }}>Enter result</button>
                        <button type="button" onClick={async () => { const response = await api.get(`/api/lineups/${row.id}/pdf?lang=en`, { responseType: "blob" }); const url = URL.createObjectURL(response.data); const link = document.createElement("a"); link.href = url; link.download = "match-sheet.pdf"; link.click(); }}>Download match sheet</button>
                      </div>
                    ) : null}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
      <footer className="pager">
        <Button disabled={page <= 0} onClick={() => setPage((value) => value - 1)}>Previous</Button>
        <span>{page + 1} / {pages}</span>
        <Button disabled={page + 1 >= pages} onClick={() => setPage((value) => value + 1)}>Next</Button>
      </footer>

      {editor ? (
        <Modal title={editor.id ? "Edit match" : "Add match"} onClose={() => setEditor(null)} closeLabel="Close"
          footer={<><Button onClick={() => setEditor(null)}>Cancel</Button><Button variant="primary" onClick={async () => {
            try {
              const competition = competitions.find((item) => String(item.id) === String(editor.competitionId));
              const payload = {
                competition: competition?.name || editor.competition || "Friendly",
                competitionId: editor.competitionId || null,
                opponentId: Number(editor.opponentId),
                kickoff: kigali(editor.kickoff) || editor.kickoff,
                venue: editor.venue,
                homeMatch: Boolean(editor.homeMatch),
                matchday: editor.matchday ? Number(editor.matchday) : null,
                referee: editor.referee,
                stage: editor.stage,
                status: editor.status || "SCHEDULED",
                squadSizeOverride: editor.squadSize ? Number(editor.squadSize) : 0,
              };
              if (editor.id) await send("put", `/api/matches/${editor.id}`, payload);
              else await send("post", "/api/matches", payload);
              setEditor(null);
              await load();
            } catch (err) { setError(err.message); }
          }}>Save match</Button></>}>
          <Field label="Competition">
            <SelectInput aria-label="Competition" value={editor.competitionId || ""} onChange={(event) => setEditor({ ...editor, competitionId: event.target.value })}>
              {competitions.map((item) => <option key={item.id} value={item.id}>{localizedName(item, locale) || item.name} · {item.type}</option>)}
            </SelectInput>
          </Field>
          <Field label="Opponent">
            <SelectInput aria-label="Opponent" value={editor.opponentId || ""} onChange={(event) => setEditor({ ...editor, opponentId: event.target.value })}>
              {opponents.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
            </SelectInput>
          </Field>
          <Field label="Kick-off"><TextInput type="datetime-local" value={editor.kickoff || ""} onChange={(event) => setEditor({ ...editor, kickoff: event.target.value })} /></Field>
          <Field label="Squad size for this match" hint="Empty uses the competition default"><TextInput type="number" min="12" max="23" value={editor.squadSize || ""} onChange={(event) => setEditor({ ...editor, squadSize: event.target.value })} /></Field>
          <Field label="Venue"><TextInput value={editor.venue || ""} onChange={(event) => setEditor({ ...editor, venue: event.target.value })} /></Field>
          <label className="tool-switch"><input type="checkbox" checked={Boolean(editor.homeMatch)} onChange={(event) => setEditor({ ...editor, homeMatch: event.target.checked })} /> Home</label>
          <Field label="Matchday"><TextInput value={editor.matchday || ""} onChange={(event) => setEditor({ ...editor, matchday: event.target.value })} /></Field>
          <Field label="Referee"><TextInput value={editor.referee || ""} onChange={(event) => setEditor({ ...editor, referee: event.target.value })} /></Field>
        </Modal>
      ) : null}

      {viewer ? (
        <Modal title={`${viewer.home ? "Viking FC" : viewer.opponent} vs ${viewer.home ? viewer.opponent : "Viking FC"}`} size="lg" onClose={() => setViewer(null)} closeLabel="Close"
          footer={<><Button onClick={() => setViewer(null)}>Close</Button><Link href={`/admin/lineup?matchId=${viewer.id}`}><Button variant="primary">Open lineup</Button></Link></>}>
          <p>{when(viewer.kickoff)} · {viewer.venue || "Venue to be confirmed"}</p>
          <div className="view-badges"><Badge>{viewer.competition}</Badge> <Badge>{viewer.status}</Badge> <Badge>{viewer.lineupStatus}</Badge></div>
          <p>{viewer.status === "FINISHED" ? `${viewer.goalsFor} – ${viewer.goalsAgainst}` : relative(viewer.kickoff)}</p>
          {(viewer.alerts || []).length ? <p>Alerts: {viewer.alerts.join(", ")}</p> : null}
        </Modal>
      ) : null}

      {result ? (
        <Modal title={`Result vs ${result.opponent}`} onClose={() => setResult(null)} closeLabel="Close"
          footer={<><Button onClick={() => setResult(null)}>Cancel</Button><Button variant="primary" onClick={async () => {
            try {
              const goalsFor = Number(result.goalsForDraft || 0);
              const scorers = Array.from({ length: goalsFor }, (_, index) => ({ minute: Number(result[`minute${index}`] || index + 1), playerId: Number(result[`scorer${index}`]) }));
              await send("post", `/api/matches/${result.id}/result`, { goalsFor, goalsAgainst: Number(result.goalsAgainstDraft || 0), crowd: result.crowd ? Number(result.crowd) : null, scorers });
              setResult(null);
              await load();
            } catch (err) { setError(err.message); }
          }}>Save result</Button></>}>
          <Field label="Goals for"><TextInput type="number" min="0" value={result.goalsForDraft || ""} onChange={(event) => setResult({ ...result, goalsForDraft: event.target.value })} /></Field>
          <Field label="Goals against"><TextInput type="number" min="0" value={result.goalsAgainstDraft || ""} onChange={(event) => setResult({ ...result, goalsAgainstDraft: event.target.value })} /></Field>
          {Array.from({ length: Number(result.goalsForDraft || 0) }, (_, index) => (
            <Field key={index} label={`Scorer ${index + 1}`}>
              <SelectInput aria-label={`Scorer ${index + 1}`} value={result[`scorer${index}`] || ""} onChange={(event) => setResult({ ...result, [`scorer${index}`]: event.target.value })}>
                <option value="">Player</option>
                {players.map((player) => <option key={player.id} value={player.id}>{player.knownAs || player.fullName}</option>)}
              </SelectInput>
            </Field>
          ))}
        </Modal>
      ) : null}

      {remove ? (
        <Modal title="Delete match" onClose={() => setRemove(null)} closeLabel="Close"
          footer={<><Button onClick={() => setRemove(null)}>Cancel</Button><Button variant="danger" onClick={async () => {
            try { await send("delete", `/api/matches/${remove.id}`); setRemove(null); await load(); }
            catch (err) { setError(err.message); setRemove(null); }
          }}>Delete</Button></>}>
          <p>Delete {remove.opponent} on {when(remove.kickoff)}? If a lineup or result exists, cancel the match instead.</p>
        </Modal>
      ) : null}
    </section>
  );
}
