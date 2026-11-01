"use client";

import { useEffect, useState } from "react";
import { Download, Eye, Pencil, Star, Users } from "lucide-react";
import { api, API_URL } from "@/services/api";
import { Avatar, Badge, Button, IconButton, Modal, SelectInput, Tabs, TextInput } from "@/ui/kit";

function photo(url) {
  if (!url) return "";
  return url.startsWith("http") ? url : `${API_URL}${url}`;
}

function when(locale, iso) {
  if (!iso) return "";
  const tag = locale === "fr" ? "fr-FR" : locale === "rw" ? "rw-RW" : "en-GB";
  return new Intl.DateTimeFormat(tag, { timeZone: "Africa/Kigali", dateStyle: "medium", timeStyle: "short" }).format(new Date(iso));
}

function resultBadge(row) {
  if (row.matchStatus !== "FINISHED" && row.matchStatus !== "LIVE") return null;
  if (row.goalsFor > row.goalsAgainst) return "W";
  if (row.goalsFor < row.goalsAgainst) return "L";
  return "D";
}

export function LineupMatches({ flow, copy, locale, onOpen, setError, catalog = [] }) {
  const [page, setPage] = useState(0);
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("ANY");
  const [competition, setCompetition] = useState("");
  const [home, setHome] = useState("");
  const [body, setBody] = useState(null);
  const [loading, setLoading] = useState(true);
  const [players, setPlayers] = useState(null);
  const [tab, setTab] = useState("played");
  const [line, setLine] = useState("ALL");
  const [duplicate, setDuplicate] = useState(null);
  const [target, setTarget] = useState("");

  useEffect(() => {
    setLoading(true);
    const params = { page, size: 8, q: query || undefined, status: status === "ANY" ? undefined : status, competition: competition || undefined, home: home === "" ? undefined : home === "home" };
    api.get("/api/matches/lineup-overview", { params }).then((response) => {
      setBody(response.data?.data || { rows: [], total: 0, page: 0, size: 8 });
    }).catch((err) => setError(err.message)).finally(() => setLoading(false));
  }, [page, query, status, competition, home, setError]);

  const rows = body?.rows || [];
  const competitions = [...new Set([...(catalog || []).map((row) => row.competition), ...rows.map((row) => row.competition)].filter(Boolean))];
  const pages = Math.max(1, Math.ceil((body?.total || 0) / (body?.size || 8)));

  async function openPlayers(row) {
    try {
      const response = await api.get(`/api/matches/${row.matchId}/participation`);
      setPlayers({ match: row, ...(response.data?.data || {}) });
      setTab("played");
      setLine("ALL");
    } catch (err) {
      setError(err.message);
    }
  }

  async function download(row, kind) {
    const path = kind === "pdf" ? `/api/lineups/${row.matchId}/pdf?lang=${locale}` : `/api/lineups/${row.matchId}/graphic`;
    const response = await api.get(path, { responseType: "blob" });
    const blob = response.data instanceof Blob ? response.data : new Blob([response.data]);
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = kind === "pdf" ? "Viking_FC_Matchsheet.pdf" : "Viking_FC_Matchday.png";
    link.click();
    URL.revokeObjectURL(url);
  }

  const list = (players?.[tab] || []).filter((player) => line === "ALL" || player.lineCode === line);

  return (
    <div className="lineup-panel match-table">
      <div className="lineup-bar">
        <TextInput aria-label={flow.searchMatches} placeholder={flow.searchMatches} value={query} onChange={(event) => { setPage(0); setQuery(event.target.value); }} />
        <SelectInput aria-label={flow.lineupStatus} value={status} onChange={(event) => { setPage(0); setStatus(event.target.value); }}>
          <option value="ANY">{flow.any}</option>
          {["NONE", "DRAFT", "SUBMITTED", "APPROVED", "PUBLISHED", "LOCKED", "FINISHED", "SCHEDULED", "LIVE"].map((item) => <option key={item} value={item}>{copy.statuses?.[item] || flow.noLineup}</option>)}
        </SelectInput>
        <SelectInput aria-label={flow.competition} value={competition} onChange={(event) => { setPage(0); setCompetition(event.target.value); }}>
          <option value="">{flow.competition}</option>
          {competitions.map((item) => <option key={item}>{item}</option>)}
        </SelectInput>
        <SelectInput aria-label={flow.side} value={home} onChange={(event) => { setPage(0); setHome(event.target.value); }}>
          <option value="">{flow.allSides}</option>
          <option value="home">{flow.home}</option>
          <option value="away">{flow.away}</option>
        </SelectInput>
      </div>
      {loading ? <p>{copy.loading}</p> : null}
      {!loading && !rows.length ? <p>{flow.emptyMatches}</p> : null}
      <div className="admin-table-wrap">
        <table className="admin-table">
          <thead>
            <tr>
              {[flow.date, flow.opponent, flow.competition, flow.side, flow.venue, flow.result, flow.lineupStatus, flow.squad, flow.formation, flow.version, flow.actions].map((label) => <th key={label}>{label}</th>)}
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => {
              const mark = resultBadge(row);
              return (
                <tr key={row.matchId}>
                  <td>{when(locale, row.kickoff)}</td>
                  <td className="opp-cell">{row.opponentLogo ? <img src={photo(row.opponentLogo)} alt="" /> : <span className="lineup-crest">{(row.opponentShort || row.opponent || "?").slice(0, 2)}</span>} {row.opponent}</td>
                  <td>{row.competition}</td>
                  <td>{row.home ? flow.home : flow.away}</td>
                  <td>{row.venue}</td>
                  <td>{mark ? <><b>{row.goalsFor} – {row.goalsAgainst}</b> <Badge tone={mark === "W" ? "good" : mark === "L" ? "bad" : "warn"}>{mark}</Badge></> : when(locale, row.kickoff).split(", ").pop()}</td>
                  <td><Badge tone={row.lineupStatus === "PUBLISHED" ? "good" : row.lineupStatus === "NONE" ? "neutral" : "warn"}>{row.lineupStatus === "NONE" ? flow.noLineup : (copy.statuses?.[row.lineupStatus] || row.lineupStatus)}</Badge></td>
                  <td>{row.squadSize || "—"}</td>
                  <td>{row.formation || "—"}</td>
                  <td>{row.revision || "—"}</td>
                  <td className="row-actions">
                    <IconButton label={flow.open} onClick={() => onOpen(row.matchId)}><Eye size={16} /></IconButton>
                    <IconButton label={flow.edit} onClick={() => onOpen(row.matchId, true)}><Pencil size={16} /></IconButton>
                    <IconButton label={flow.players} onClick={() => openPlayers(row)}><Users size={16} /></IconButton>
                    <IconButton label={flow.download} onClick={() => download(row, "png")}><Download size={16} /></IconButton>
                    <IconButton label={flow.duplicate} onClick={() => { setDuplicate(row); setTarget(""); }}><Star size={16} /></IconButton>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      <div className="pager">
        <Button disabled={page <= 0} onClick={() => setPage((value) => value - 1)}>{copy.prev}</Button>
        <span>{page + 1} / {pages}</span>
        <Button disabled={page + 1 >= pages} onClick={() => setPage((value) => value + 1)}>{copy.next}</Button>
      </div>

      {players ? (
        <Modal title={`${players.opponent || players.match?.opponent}`} subtitle={`${players.formation || "—"} · ${players.squadSize || "—"}`} size="lg" onClose={() => setPlayers(null)} closeLabel={copy.cancel}
          footer={<><Button onClick={() => download(players.match, "pdf")}>{copy.sheet}</Button><Button variant="primary" onClick={() => { onOpen(players.matchId || players.match.matchId); setPlayers(null); }}>{flow.open}</Button></>}>
          <Tabs id="players-tab" label={flow.players} value={tab} onChange={setTab} items={[{ id: "played", label: `${flow.played} ${(players.played || []).length}` }, { id: "absent", label: `${flow.absent} ${(players.absent || []).length}` }]} />
          <SelectInput aria-label={flow.filterLine} value={line} onChange={(event) => setLine(event.target.value)}>
            <option value="ALL">{flow.allLines}</option>
            {Object.entries(flow.lines).map(([key, label]) => <option key={key} value={key}>{label}</option>)}
          </SelectInput>
          <div className="player-stack">
            {list.map((player) => (
              <article key={player.playerId} className="player-card">
                <Avatar name={player.knownAs} src={photo(player.photoUrl)} />
                <span>
                  <b>#{player.jersey} {player.knownAs} {player.motm ? "★" : ""}</b>
                  <small>{flow.positions[player.position] || player.positionLabel} · {flow.minutes} {player.minutes} · {flow.goals} {player.goals} · {flow.assists} {player.assists}</small>
                  <small>{player.entered != null ? `${flow.entered} ${player.entered}'` : ""} {player.replaced != null ? `${flow.replaced} ${player.replaced}'` : ""} {player.reason ? `${flow.reason}: ${player.reason}` : ""} {player.planned ? flow.planned : ""} {player.rating != null ? `${flow.rating} ${player.rating}` : ""}</small>
                </span>
                <Badge>{player.yellow}/{player.red}</Badge>
              </article>
            ))}
          </div>
        </Modal>
      ) : null}

      {duplicate ? (
        <Modal title={flow.duplicate} onClose={() => setDuplicate(null)} closeLabel={copy.cancel}
          footer={<><Button onClick={() => setDuplicate(null)}>{copy.cancel}</Button><Button variant="primary" disabled={!target} onClick={async () => { try { await api.post(`/api/lineups/${duplicate.matchId}/duplicate`, { targetMatchId: Number(target) }); setDuplicate(null); onOpen(Number(target)); } catch (err) { setError(err.message); } }}>{flow.duplicate}</Button></>}>
          <SelectInput aria-label={flow.duplicate} value={target} onChange={(event) => setTarget(event.target.value)}>
            <option value="">{flow.opponent}</option>
            {(catalog.length ? catalog : rows).filter((row) => (row.matchId || row.id) !== duplicate.matchId).map((row) => <option key={row.matchId || row.id} value={row.matchId || row.id}>{row.opponent}</option>)}
          </SelectInput>
        </Modal>
      ) : null}
    </div>
  );
}
