"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import { motion } from "framer-motion";
import { api, API_URL, getData } from "@/services/api";
import { useAuth } from "@/components/providers/AuthProvider";
import { useLocale } from "@/components/providers/LocaleProvider";
import { isExecutive } from "@/lib/admin/access";
import { Badge, Button, Field, Modal, SelectInput, Tabs, TextArea, TextInput } from "@/ui/kit";
import { ChartCard } from "./dashboard/parts";
import { lineupCopy } from "./lineupCopy";
import { lineupFlowCopy } from "./lineupFlowCopy";
import { LineupBuilder } from "./lineupBuilder";
import { LineupMatches } from "./lineupMatches";
import "./lineup.css";

const STEP_KEYS = ["DRAFT", "SUBMITTED", "APPROVED", "PUBLISHED", "LOCKED", "FINISHED"];
const LINE_OF = { GK: "Goalkeepers", DEF: "Defenders", MID: "Midfielders", ATT: "Attackers" };
const REASONS = ["Injured", "Suspended", "Tactical", "Rested", "Discipline", "Not fit", "Other"];

function copyFor(locale) {
  return lineupCopy[locale] || lineupCopy.en;
}

function flowFor(locale) {
  return lineupFlowCopy[locale] || lineupFlowCopy.en;
}

function reduced() {
  return typeof window !== "undefined" && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

function photo(url) {
  if (!url) return "";
  return url.startsWith("http") ? url : `${API_URL}${url}`;
}

function face(person, className = "face") {
  const name = person?.knownAs || person?.name || person?.fullName || "";
  if (person?.photoUrl) return <img src={photo(person.photoUrl)} alt="" />;
  const initials = name.split(" ").map((part) => part[0]).join("").slice(0, 2).toUpperCase();
  return <span className={className}>{initials || "?"}</span>;
}

function when(locale, iso) {
  if (!iso) return "";
  const tag = locale === "fr" ? "fr-FR" : locale === "rw" ? "rw-RW" : "en-GB";
  return new Intl.DateTimeFormat(tag, { timeZone: "Africa/Kigali", dateStyle: "medium", timeStyle: "short" }).format(new Date(iso));
}

function clock(iso) {
  if (!iso) return "";
  return new Intl.DateTimeFormat("en-GB", { timeZone: "Africa/Kigali", hour: "2-digit", minute: "2-digit", hourCycle: "h23" }).format(new Date(iso));
}

function kigaliInput(iso) {
  if (!iso) return "";
  const parts = new Intl.DateTimeFormat("en-GB", {
    timeZone: "Africa/Kigali", year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit", hourCycle: "h23",
  }).formatToParts(new Date(iso));
  const get = (type) => parts.find((part) => part.type === type)?.value;
  return `${get("year")}-${get("month")}-${get("day")}T${get("hour")}:${get("minute")}`;
}

function kigaliIso(value) {
  if (!value) return null;
  return value.length === 16 ? `${value}:00+02:00` : value;
}

function spotsOf(sheet, formation) {
  return (sheet?.formations || []).find((item) => item.code === formation)?.spots || (sheet?.formations || [])[0]?.spots || [];
}

function playerById(squad, id) {
  return (squad || []).find((player) => player.id === id) || null;
}

function compatible(spot, player) {
  if (!spot || !player) return true;
  if (spot.line === "GK" || spot.code === "GK") return player.line === "Goalkeepers" || player.position === "GOALKEEPER";
  return player.line === LINE_OF[spot.line];
}

function marks(events, playerId) {
  const list = events || [];
  const mine = list.filter((event) => event.playerId === playerId);
  return {
    goals: mine.filter((event) => event.type === "GOAL").length,
    assists: list.filter((event) => event.type === "GOAL" && event.relatedPlayerId === playerId).length,
    yellow: mine.some((event) => event.type === "YELLOW_CARD"),
    red: mine.some((event) => event.type === "RED_CARD"),
    injury: mine.some((event) => event.type === "INJURY"),
    out: mine.find((event) => event.type === "SUB_OUT"),
    inn: mine.find((event) => event.type === "SUB_IN"),
  };
}

function rateTone(value) {
  if (value == null || Number.isNaN(Number(value))) return "";
  if (Number(value) >= 7) return "good";
  if (Number(value) >= 6) return "mid";
  return "low";
}

function hydrate(sheet) {
  const formation = sheet?.lineup?.formation || "4-3-3";
  const starters = {};
  const bench = [];
  const overrides = {};
  let captainId = null;
  (sheet?.slots || []).forEach((slot) => {
    if (slot.selection === "STARTING" && slot.positionSlot) starters[slot.positionSlot] = slot.playerId;
    if (slot.selection === "SUBSTITUTE") bench.push(slot.playerId);
    if (slot.captain) captainId = slot.playerId;
    if (slot.overrideReason) overrides[slot.playerId] = slot.overrideReason;
  });
  const members = sheet?.squadMembers || [];
  const reasons = {};
  members.forEach((row) => {
    if (!row.called && row.reasonNotCalled) reasons[row.playerId] = row.reasonNotCalled;
    if (row.overrideReason) overrides[row.playerId] = row.overrideReason;
  });
  const calledFromMembers = members.filter((row) => row.called).map((row) => row.playerId);
  const called = calledFromMembers.length ? calledFromMembers : [...new Set([...Object.values(starters), ...bench])];
  return {
    formation,
    starters,
    bench,
    captainId,
    overrides,
    reasons,
    called,
    squadSize: sheet?.lineup?.squadSize || sheet?.match?.squadSizeOverride || sheet?.settings?.competitionRule?.squadSize || sheet?.settings?.defaultSquadSize || 18,
    plan: sheet?.lineup?.squadPlan || null,
    opponentFormation: sheet?.lineup?.opponentFormation || "4-4-2",
    opponentNames: sheet?.lineup?.opponentNames || "",
  };
}

function buildSlots(draft, sheet) {
  const spots = spotsOf(sheet, draft.formation);
  const slots = [];
  spots.forEach((spot, index) => {
    const playerId = draft.starters[spot.code];
    if (!playerId) return;
    slots.push({
      playerId, selection: "STARTING", positionSlot: spot.code, sortOrder: index,
      captain: playerId === draft.captainId, overrideReason: draft.overrides[playerId] || null, x: spot.x, y: spot.y,
    });
  });
  draft.bench.forEach((playerId, index) => {
    slots.push({
      playerId, selection: "SUBSTITUTE", positionSlot: "BENCH", sortOrder: 20 + index,
      captain: false, overrideReason: draft.overrides[playerId] || null, x: null, y: null,
    });
  });
  return slots;
}

async function send(method, path, body) {
  const response = await api({ method, url: path, data: body });
  if (!response.data?.success) throw new Error(response.data?.message || "Request failed");
  return response.data.data;
}

async function download(path, filename) {
  const response = await api.get(path, { responseType: "blob" });
  const blob = response.data instanceof Blob ? response.data : new Blob([response.data]);
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}

export function LineupOffice() {
  const { locale } = useLocale();
  const { user } = useAuth();
  const copy = copyFor(locale);
  const flow = flowFor(locale);
  const [view, setView] = useState("builder");
  const executive = isExecutive(user?.role || "");
  const [matches, setMatches] = useState([]);
  const [matchId, setMatchId] = useState("");
  const [tab, setTab] = useState("lineup");
  const [sheet, setSheet] = useState(null);
  const [draft, setDraft] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [saving, setSaving] = useState(false);
  const [savedAt, setSavedAt] = useState("");
  const [dirty, setDirty] = useState(false);
  const [line, setLine] = useState("All");
  const [query, setQuery] = useState("");
  const [availableOnly, setAvailableOnly] = useState(false);
  const [squadOpen, setSquadOpen] = useState(false);
  const [picked, setPicked] = useState(null);
  const [profile, setProfile] = useState(null);
  const [showOpponent, setShowOpponent] = useState(false);
  const [captains, setCaptains] = useState({});
  const [note, setNote] = useState(null);
  const [comment, setComment] = useState("");
  const [calls, setCalls] = useState({});
  const [logistics, setLogistics] = useState({ meetingAt: "", meetingPlace: "", kitNote: "", travelNote: "", message: "" });
  const [leaderboard, setLeaderboard] = useState([]);
  const [historyPage, setHistoryPage] = useState(0);
  const [historySize, setHistorySize] = useState(8);
  const [historyFilter, setHistoryFilter] = useState("Any");
  const [sub, setSub] = useState({ outId: "", inId: "", minute: "60", reason: "tactical" });
  const [eventForm, setEventForm] = useState({ minute: "1", type: "GOAL", playerId: "", relatedPlayerId: "", note: "" });
  const [rateMode, setRateMode] = useState(false);
  const drag = useRef(null);
  const skipSave = useRef(true);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    if (params.get("matchId")) setMatchId(params.get("matchId"));
    if (params.get("tab")) setTab(params.get("tab"));
    getData("/api/matches").then((rows) => {
      const list = rows || [];
      setMatches(list);
      if (!params.get("matchId") && list.length) {
        const upcoming = list.filter((row) => row.status !== "FINISHED").sort((a, b) => new Date(a.kickoff) - new Date(b.kickoff));
        const next = upcoming.find((row) => new Date(row.kickoff) >= Date.now()) || upcoming[0] || list[0];
        setMatchId(String(next.id));
      }
    }).catch((err) => setError(err.message)).finally(() => setLoading(false));
    getData("/api/captains").then((rows) => {
      const map = {};
      (rows || []).filter((row) => !row.endedAt).forEach((row) => { map[row.playerId] = row.rank; });
      setCaptains(map);
    }).catch(() => setCaptains({}));
  }, []);

  useEffect(() => {
    if (tab !== "ratings") return;
    getData("/api/ratings/leaderboard").then(setLeaderboard).catch(() => setLeaderboard([]));
  }, [tab]);

  useEffect(() => {
    if (!matchId) return;
    const url = `/admin/lineup?matchId=${matchId}&tab=${tab}`;
    if (window.location.pathname.startsWith("/admin/lineup")) window.history.replaceState(null, "", url);
  }, [matchId, tab]);

  useEffect(() => {
    if (!matchId) return;
    setLoading(true);
    setError("");
    skipSave.current = true;
    getData(`/api/lineups/${matchId}`).then((data) => {
      setSheet(data);
      const next = hydrate(data);
      setDraft(next);
      const map = {};
      (data.convocations || []).forEach((row) => { map[row.playerId] = row; });
      setCalls(map);
      setLogistics({
        meetingAt: kigaliInput(data.lineup?.meetingAt),
        meetingPlace: data.lineup?.meetingPlace || "",
        kitNote: data.lineup?.kitNote || "",
        travelNote: data.lineup?.travelNote || "",
        message: data.lineup?.message || "",
      });
      setDirty(false);
    }).catch((err) => setError(err.message)).finally(() => setLoading(false));
  }, [matchId]);

  useEffect(() => {
    if (view === "builder" && tab === "lineup") return undefined;
    if (!dirty || !draft || !sheet?.permissions?.edit) return undefined;
    if (skipSave.current) {
      skipSave.current = false;
      return undefined;
    }
    const timer = setTimeout(() => {
      setSaving(true);
      send("put", `/api/lineups/${matchId}`, {
        formation: draft.formation,
        slots: buildSlots(draft, sheet),
        version: sheet.lineup?.version ?? null,
        opponentFormation: draft.opponentFormation,
        opponentNames: draft.opponentNames,
      }).then((data) => {
        setSheet(data);
        setSavedAt(clock(new Date().toISOString()));
        setDirty(false);
      }).catch((err) => {
        setError(err.message);
        if (err.status === 409) getData(`/api/lineups/${matchId}`).then(setSheet);
      }).finally(() => setSaving(false));
    }, 2500);
    return () => clearTimeout(timer);
  }, [dirty, draft, matchId, sheet, view, tab]);

  const squad = sheet?.squad || [];
  const spots = spotsOf(sheet, draft?.formation || "4-3-3");
  const permissions = sheet?.permissions || {};
  const match = sheet?.match;
  const lineup = sheet?.lineup;

  const filtered = useMemo(() => squad.filter((player) => {
    if (line !== "All" && player.line !== line) return false;
    if (availableOnly && player.availability !== "AVAILABLE") return false;
    const blob = `${player.knownAs} ${player.fullName} ${player.jersey || ""}`.toLowerCase();
    return blob.includes(query.trim().toLowerCase());
  }), [squad, line, availableOnly, query]);

  function patch(next) {
    skipSave.current = false;
    setDraft((current) => ({ ...current, ...next }));
    setDirty(true);
  }

  function take(playerId) {
    const starters = { ...draft.starters };
    Object.keys(starters).forEach((code) => { if (starters[code] === playerId) delete starters[code]; });
    return { starters, bench: draft.bench.filter((id) => id !== playerId) };
  }

  function place(code, player) {
    if (!permissions.edit || !player) return;
    if (player.availability !== "AVAILABLE" && !draft.overrides[player.id] && !executive) {
      setError(`${player.knownAs} · ${copy.availability[player.availability] || player.availability}`);
      return;
    }
    if (player.availability !== "AVAILABLE" && !draft.overrides[player.id] && executive) {
      setNote({ kind: "override", player, code });
      setComment("");
      return;
    }
    const cleared = take(player.id);
    patch({ starters: { ...cleared.starters, [code]: player.id }, bench: cleared.bench });
    setPicked(null);
  }

  function toBench(playerId) {
    if (!permissions.edit) return;
    const cleared = take(playerId);
    const max = sheet?.settings?.maxBench || 9;
    if (cleared.bench.length >= max) {
      setError(`${copy.needBench} ${max}`);
      return;
    }
    patch({ starters: cleared.starters, bench: [...cleared.bench, playerId] });
  }

  function removePlayer(playerId) {
    const cleared = take(playerId);
    patch({ starters: cleared.starters, bench: cleared.bench, captainId: draft.captainId === playerId ? null : draft.captainId });
    setProfile(null);
  }

  function changeFormation(code) {
    const previous = spotsOf(sheet, draft.formation);
    const nextSpots = spotsOf(sheet, code);
    const used = new Set();
    const starters = {};
    Object.entries(draft.starters).forEach(([slot, playerId]) => {
      const same = nextSpots.find((spot) => spot.code === slot && !used.has(spot.code));
      const old = previous.find((spot) => spot.code === slot);
      const fallback = nextSpots.find((spot) => spot.line === old?.line && !used.has(spot.code)) || nextSpots.find((spot) => !used.has(spot.code));
      const chosen = same || fallback;
      if (chosen) {
        starters[chosen.code] = playerId;
        used.add(chosen.code);
      }
    });
    patch({ formation: code, starters });
  }

  async function refresh(data) {
    setSheet(data);
    setDraft(hydrate(data));
    setDirty(false);
    skipSave.current = true;
    const map = {};
    (data.convocations || []).forEach((row) => { map[row.playerId] = row; });
    setCalls(map);
  }

  async function act(action, text) {
    setError("");
    try {
      if (dirty) {
        await send("put", `/api/lineups/${matchId}`, {
          formation: draft.formation, slots: buildSlots(draft, sheet), version: sheet.lineup?.version ?? null,
          opponentFormation: draft.opponentFormation, opponentNames: draft.opponentNames,
        });
      }
      const data = await send("post", `/api/lineups/${matchId}/${action}`, text ? { comment: text } : {});
      await refresh(data);
      setNotice(copy.statuses[data.lineup?.status] || "");
    } catch (err) {
      setError(err.message);
    }
  }

  const issues = useMemo(() => {
    if (!draft) return [];
    const ids = Object.values(draft.starters);
    const list = [];
    if (ids.length !== 11) list.push(copy.needXi);
    const gk = playerById(squad, draft.starters.GK);
    if (!gk || gk.line !== "Goalkeepers") list.push(copy.needGk);
    if (new Set([...ids, ...draft.bench]).size !== ids.length + draft.bench.length) list.push(copy.both);
    if (draft.bench.length > (sheet?.settings?.maxBench || 9)) list.push(copy.needBench);
    return list;
  }, [draft, squad, sheet, copy]);

  const counts = useMemo(() => {
    const called = draft?.called?.length || Object.values(calls).filter((row) => row.called).length;
    const unavailable = squad.filter((player) => player.availability === "INJURED" || player.availability === "SUSPENDED").length;
    const rated = (sheet?.ratings || []).map((row) => Number(row.final)).filter((value) => !Number.isNaN(value));
    const average = rated.length ? (rated.reduce((sum, value) => sum + value, 0) / rated.length).toFixed(1) : null;
    const size = draft?.squadSize || sheet?.settings?.defaultSquadSize || 18;
    const xi = Object.keys(draft?.starters || {}).length;
    return { called, size, xi, bench: Math.max(0, called - xi), notCalled: Math.max(0, squad.length - called), unavailable, average };
  }, [calls, squad, draft, sheet]);

  const deadline = lineup?.deadlineAt || (match ? new Date(new Date(match.kickoff).getTime() - (sheet?.settings?.deadlineHours || 48) * 3600000).toISOString() : null);
  const [now, setNow] = useState(Date.now());
  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(id);
  }, []);
  const remaining = deadline ? new Date(deadline).getTime() - now : 0;
  const countdown = remaining <= 0
    ? copy.overdue
    : `${copy.deadline} ${Math.floor(remaining / 86400000)}d ${String(Math.floor(remaining / 3600000) % 24).padStart(2, "0")}h ${String(Math.floor(remaining / 60000) % 60).padStart(2, "0")}m`;
  const chip = remaining <= 0 ? "bad" : remaining < 6 * 3600000 ? "warn" : "";

  const upcoming = matches.filter((row) => row.status !== "FINISHED");
  const finished = matches.filter((row) => row.status === "FINISHED");

  if (!loading && !matches.length) {
    return (
      <section className="lineup-page">
        <h1>{copy.title}</h1>
        <p>{copy.empty}</p>
        <Link href="/admin/matches"><Button variant="primary">{copy.schedule}</Button></Link>
      </section>
    );
  }

  return (
    <section className="lineup-page">
      <div className="view-switch" role="tablist" aria-label={flow.viewBuilder}>
        <button type="button" className={view === "builder" ? "on" : ""} onClick={() => setView("builder")}>{flow.viewBuilder}</button>
        <button type="button" className={view === "matches" ? "on" : ""} onClick={() => setView("matches")}>{flow.viewMatches}</button>
      </div>
      {view === "matches" ? <LineupMatches flow={flow} copy={copy} locale={locale} catalog={matches} setError={setError} onOpen={(id) => { setMatchId(String(id)); setView("builder"); setTab("lineup"); }} /> : null}
      {view === "builder" ? <div className="lineup-bar">
        <button type="button" className="vfc-btn vfc-btn--secondary" onClick={() => shift(-1)} aria-label={copy.prev}>{copy.prev}</button>
        <SelectInput aria-label={copy.title} value={matchId} onChange={(event) => setMatchId(event.target.value)}>
          <optgroup label={copy.upcoming}>{upcoming.map((row) => <option key={row.id} value={row.id}>{row.opponent} · {when(locale, row.kickoff)}</option>)}</optgroup>
          <optgroup label={copy.finished}>{finished.map((row) => <option key={row.id} value={row.id}>{row.opponent} · {when(locale, row.kickoff)}</option>)}</optgroup>
        </SelectInput>
        <button type="button" className="vfc-btn vfc-btn--secondary" onClick={() => shift(1)} aria-label={copy.next}>{copy.next}</button>
        <Link href="/admin/matches">{copy.schedule}</Link>
        {sheet?.published === false ? <span>{copy.unpublished}</span> : null}
      </div> : null}

      {view === "builder" && (loading || !sheet || !draft) ? <div className="lineup-panel" style={{ padding: 16 }}>{[1, 2, 3, 4].map((item) => <div key={item} className="skel" style={{ marginBottom: 8 }} />)}<p>{copy.loading}</p></div> : null}
      {error ? <p className="banner" role="alert">{error} <button type="button" onClick={() => setMatchId(matchId)}>{copy.retry}</button></p> : null}
      {notice ? <p className="banner">{notice}</p> : null}

      {view === "builder" && match && draft ? (
        <>
          <header className="lineup-head">
            <div className="lineup-score">
              <div className="lineup-club">
                <img src="/logo/viking-fc.png" alt="Viking FC" className="lineup-crest" />
                <div><strong>Viking FC</strong><p>{match.home ? copy.home : copy.away}</p></div>
              </div>
              <div>
                <strong>{match.status === "FINISHED" || match.status === "LIVE" ? `${match.goalsFor ?? 0} - ${match.goalsAgainst ?? 0}` : clock(match.kickoff)}</strong>
                <p>{match.competition}</p>
              </div>
              <div className="lineup-club away">
                {match.opponentLogo ? <img src={photo(match.opponentLogo)} alt="" /> : <span className="lineup-crest">{(match.opponentShort || match.opponent || "?").slice(0, 2)}</span>}
                <div><strong>{match.opponent}</strong><p>{match.venue}</p></div>
              </div>
            </div>
            <p className="lineup-meta">{when(locale, match.kickoff)}{match.referee ? ` · ${copy.referee}: ${match.referee}` : ""}</p>
            <div className="lineup-steps" aria-label={copy.title}>
              {STEP_KEYS.map((key, index) => (
                <span key={key} className={STEP_KEYS.indexOf(lineup?.status === "CHANGES_REQUESTED" ? "DRAFT" : lineup?.status) >= index ? "on" : ""}>{copy.steps[index]}</span>
              ))}
            </div>
            {lineup?.status === "CHANGES_REQUESTED" ? <p>{copy.statuses.CHANGES_REQUESTED}{lineup.reviewComment ? `: ${lineup.reviewComment}` : ""}</p> : null}
            {deadline ? <span className={`lineup-chip ${chip}`}>{countdown}</span> : null}
            {lineup?.editingBy && lineup.editingBy !== user?.email ? <p>{copy.editing}: {lineup.editingBy}</p> : null}
            {lineup?.gkSuggestionId ? <p>{copy.suggest}: {playerById(squad, lineup.gkSuggestionId)?.knownAs}</p> : null}
          </header>

          <div className="lineup-summary">
            <Kpi value={counts.called} max={counts.size} label={copy.called} />
            <Kpi value={counts.xi} max={11} label={copy.xi} />
            <Kpi value={counts.bench} max={Math.max(0, counts.size - 11)} label={copy.bench} />
            <Kpi value={counts.notCalled} label={copy.notCalled} />
            <Kpi value={counts.unavailable} label={copy.unavailable} />
          </div>

          <Tabs label={copy.title} value={tab} onChange={setTab} items={Object.entries(copy.tabs).map(([id, label]) => ({ id, label }))} />

          {tab === "lineup" ? (
            <LineupBuilder
              sheet={sheet}
              draft={draft}
              patch={patch}
              flow={flow}
              copy={copy}
              locale={locale}
              permissions={permissions}
              executive={executive}
              captains={captains}
              matchId={matchId}
              onRefresh={refresh}
              user={user}
              setNotice={setNotice}
              setError={setError}
            />
          ) : null}

          {tab === "callups" ? <Callups copy={copy} locale={locale} squad={squad} calls={calls} setCalls={setCalls} logistics={logistics} setLogistics={setLogistics} permissions={permissions} matchId={matchId} onRefresh={refresh} setError={setError} setNotice={setNotice} settings={sheet.settings} /> : null}
          {tab === "bench" ? <BenchTab copy={copy} squad={squad} draft={draft} events={sheet.events} permissions={permissions} sub={sub} setSub={setSub} matchId={matchId} onRefresh={refresh} setError={setError} /> : null}
          {tab === "ratings" ? <Ratings copy={copy} sheet={sheet} squad={squad} permissions={permissions} executive={executive} matchId={matchId} onRefresh={refresh} setError={setError} leaderboard={leaderboard} rateMode={rateMode} setRateMode={setRateMode} /> : null}
          {tab === "timeline" ? <Timeline copy={copy} locale={locale} events={sheet.events} squad={squad} permissions={permissions} form={eventForm} setForm={setEventForm} matchId={matchId} onRefresh={refresh} setError={setError} /> : null}
          {tab === "history" ? <History copy={copy} locale={locale} rows={sheet.history || []} page={historyPage} setPage={setHistoryPage} size={historySize} setSize={setHistorySize} filter={historyFilter} setFilter={setHistoryFilter} canRestore={permissions.edit} onRestore={async (revision) => { try { await refresh(await send("post", `/api/lineups/${matchId}/restore/${revision}`)); } catch (err) { setError(err.message); } }} /> : null}

          {tab !== "lineup" ? <div className="lineup-actions">
            <Button className="drawer-toggle" onClick={() => setSquadOpen((open) => !open)}>{copy.squad}</Button>
            {permissions.edit ? <Button onClick={() => saveDraft()} disabled={saving}>{copy.save}</Button> : null}
            {permissions.submit ? <Button variant="primary" disabled={issues.length > 0} title={issues.join(", ")} onClick={() => act("submit")}>{copy.submit}</Button> : null}
            {permissions.approve ? <Button variant="primary" onClick={() => act("approve")}>{copy.approve}</Button> : null}
            {permissions.approve ? <Button onClick={() => { setNote({ kind: "changes" }); setComment(""); }}>{copy.changes}</Button> : null}
            {permissions.approve ? <Button variant="danger" onClick={() => { setNote({ kind: "reject" }); setComment(""); }}>{copy.reject}</Button> : null}
            {permissions.publish ? <Button variant="primary" onClick={() => { if (lineup?.status !== "APPROVED" && executive) { setNote({ kind: "publish" }); setComment(""); } else act("publish"); }}>{copy.publish}</Button> : null}
            {permissions.unlock ? <Button onClick={() => { setNote({ kind: "unlock" }); setComment(""); }}>{copy.unlock}</Button> : null}
            {permissions.graphic ? <Button onClick={() => download(`/api/lineups/${matchId}/graphic`, "Viking_FC_Matchday.png")}>{copy.graphic}</Button> : null}
            {permissions.graphic || permissions.calls ? <Button onClick={() => download(`/api/lineups/${matchId}/pdf?lang=${locale}`, "Viking_FC_Matchsheet.pdf")}>{copy.sheet}</Button> : null}
          </div> : null}
        </>
      ) : null}

      {profile ? (
        <Modal title={profile.knownAs} subtitle={`#${profile.jersey || ""} · ${profile.positionLabel || ""}`} onClose={() => setProfile(null)} closeLabel={copy.cancel}
          footer={<><Button onClick={() => setProfile(null)}>{copy.cancel}</Button>{permissions.edit ? <Button variant="danger" onClick={() => removePlayer(profile.id)}>{copy.remove}</Button> : null}</>}>
          <div className="lineup-club">{face(profile)}<div><p>{copy.age}: {profile.age ?? "—"}</p><p>{copy.form}: {profile.form ?? "—"}</p><p>{copy.availability[profile.availability]}</p></div></div>
          {permissions.edit ? <Button onClick={() => { setPicked(profile.id); setProfile(null); }}>{copy.replace}</Button> : null}
          {permissions.edit ? <Button onClick={() => patch({ captainId: profile.id })}>{copy.captain}</Button> : null}
          {permissions.suggestGk && profile.line === "Goalkeepers" ? <Button onClick={async () => { try { await refresh(await send("post", `/api/lineups/${matchId}/suggest-gk`, { playerId: profile.id })); setProfile(null); } catch (err) { setError(err.message); } }}>{copy.suggest}</Button> : null}
          {permissions.medical ? <Button onClick={() => { setNote({ kind: "flag", player: profile }); setComment(""); }}>{copy.flag}</Button> : null}
        </Modal>
      ) : null}

      {note ? (
        <Modal title={copy.reason} onClose={() => setNote(null)} closeLabel={copy.cancel}
          footer={<><Button onClick={() => setNote(null)}>{copy.cancel}</Button><Button variant="primary" onClick={() => confirmNote()}>{copy.apply}</Button></>}>
          <Field label={copy.comment}><TextArea value={comment} onChange={(event) => setComment(event.target.value)} /></Field>
        </Modal>
      ) : null}
    </section>
  );

  async function saveDraft() {
    setSaving(true);
    try {
      const data = await send("put", `/api/lineups/${matchId}`, {
        formation: draft.formation, slots: buildSlots(draft, sheet), version: sheet.lineup?.version ?? null,
        opponentFormation: draft.opponentFormation, opponentNames: draft.opponentNames,
      });
      setSheet(data);
      setSavedAt(clock(new Date().toISOString()));
      setDirty(false);
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  function shift(step) {
    const index = matches.findIndex((row) => String(row.id) === String(matchId));
    const next = matches[index + step];
    if (next) setMatchId(String(next.id));
  }

  async function confirmNote() {
    if (!comment.trim() && note.kind !== "flag") return;
    if (note.kind === "override") {
      const cleared = take(note.player.id);
      patch({ starters: { ...cleared.starters, [note.code]: note.player.id }, bench: cleared.bench, overrides: { ...draft.overrides, [note.player.id]: comment.trim() } });
      setNote(null);
      return;
    }
    if (note.kind === "flag") {
      try { await refresh(await send("post", `/api/lineups/${matchId}/flags`, { playerId: note.player.id, note: comment.trim() })); } catch (err) { setError(err.message); }
      setNote(null);
      return;
    }
    const action = note.kind === "changes" ? "request-changes" : note.kind;
    setNote(null);
    act(action, comment.trim());
  }
}

function Pitch({ spots, draft, squad, events, ratings, captains, copy, mirror, names, drag, onPlace, onBench, onOpen, picked }) {
  const lines = (names || "").split("\n").map((line) => line.trim()).filter(Boolean);
  return (
    <div className="pitch-wrap" role="list">
      <div className="pitch-lines" aria-hidden="true"><i /><b /><em /></div>
      {spots.map((spot) => {
        const player = mirror ? null : playerById(squad, draft.starters[spot.code]);
        const opponent = mirror ? lines[spots.indexOf(spot)] : "";
        const left = mirror ? 100 - spot.x : spot.x;
        const top = mirror ? 100 - spot.y : spot.y;
        const rating = (ratings || []).find((row) => row.playerId === player?.id);
        const mark = player ? marks(events, player.id) : null;
        const rank = player ? captains[player.id] : null;
        const open = picked && !mirror;
        return (
          <motion.button
            key={spot.code}
            type="button"
            className={`token${player && !compatible(spot, player) ? " warn" : ""}${!player && open ? " slot-open" : ""}`}
            style={{ left: `${left}%`, top: `${top}%` }}
            layout={!reduced()}
            aria-label={player ? `${player.knownAs} ${spot.code}` : spot.code}
            onDragOver={(event) => event.preventDefault()}
            onDrop={(event) => { event.preventDefault(); if (!mirror && drag.current?.playerId) onPlace(spot.code, playerById(squad, drag.current.playerId)); }}
            onClick={() => {
              if (mirror) return;
              if (picked) onPlace(spot.code, playerById(squad, picked));
              else if (player) onOpen(player);
            }}
            draggable={Boolean(player)}
            onDragStart={() => { if (player) drag.current = { playerId: player.id, from: spot.code }; }}
          >
            {player ? face(player) : <span className="face">{opponent ? opponent.split(" ")[0] : spot.code}</span>}
            {player ? <span className="jersey">{player.jersey}</span> : null}
            {player && (rank || draft.captainId === player.id) ? <span className="arm">{copy.ranks[rank] || (draft.captainId === player.id ? "C" : "")}</span> : null}
            {rating?.final != null ? <span className={`rate ${rateTone(rating.final)}`}>{Number(rating.final).toFixed(1)}{rating.auto ? "*" : ""}</span> : null}
            <small>{player ? player.knownAs : opponent || spot.code}</small>
            {mark?.goals ? <small>● {mark.goals}</small> : null}
            {mark?.out ? <small>↑ {mark.out.minute}</small> : null}
            {mark?.inn ? <small>↓ {mark.inn.minute}</small> : null}
          </motion.button>
        );
      })}
    </div>
  );
}

function SquadPanel({ copy, open, players, line, setLine, query, setQuery, availableOnly, setAvailableOnly, draft, calls, captains, drag, onPick, picked }) {
  return (
    <aside className={`lineup-panel squad-dock${open ? " open" : ""}`} style={{ padding: 12 }}>
      <h2>{copy.squad}</h2>
      <TextInput aria-label={copy.search} placeholder={copy.search} value={query} onChange={(event) => setQuery(event.target.value)} />
      <div className="chips">
        {Object.entries(copy.lines).map(([key, label]) => <button key={key} type="button" className={line === key ? "on" : ""} onClick={() => setLine(key)}>{label}</button>)}
      </div>
      <label><input type="checkbox" checked={availableOnly} onChange={(event) => setAvailableOnly(event.target.checked)} /> {copy.availableOnly}</label>
      <div className="squad-list">
        {players.map((player) => {
          const used = Object.values(draft.starters).includes(player.id) ? "STARTING" : draft.bench.includes(player.id) ? "SUBSTITUTE" : calls[player.id]?.called ? "CALLED" : "";
          return (
            <button key={player.id} type="button" className="squad-card" draggable onDragStart={() => { drag.current = { playerId: player.id }; }} onClick={() => onPick(player)} aria-pressed={picked === player.id}>
              {face(player)}
              <span>
                <b>#{player.jersey} {player.knownAs}</b>
                <small>{player.positionLabel} · {copy.age} {player.age ?? "—"} · {copy.form} {player.form ?? "—"}</small>
                <small>{copy.availability[player.availability]} {captains[player.id] ? copy.ranks[captains[player.id]] : ""} {used ? copy.used[used] : ""}</small>
              </span>
            </button>
          );
        })}
      </div>
    </aside>
  );
}

function Groups({ copy, squad, draft, calls }) {
  const used = new Set([...Object.values(draft.starters), ...draft.bench]);
  const called = squad.filter((player) => calls[player.id]?.called && !used.has(player.id));
  const omitted = squad.filter((player) => calls[player.id] && !calls[player.id].called);
  return (
    <>
      <h2>{copy.calledUpNot}</h2>
      <div className="group-row">{called.map((player) => <span key={player.id} className="bench-card">{face(player)}{player.knownAs}</span>)}</div>
      <h2>{copy.notCalledGroup}</h2>
      <div className="group-row">{omitted.map((player) => <span key={player.id} className="bench-card">{face(player)}{player.knownAs} · {copy.reasons[calls[player.id].reason] || "—"}</span>)}</div>
    </>
  );
}

function Callups({ copy, locale, squad, calls, setCalls, logistics, setLogistics, permissions, matchId, onRefresh, setError, setNotice, settings }) {
  const rows = squad.map((player) => calls[player.id] || { playerId: player.id, called: false, response: "PENDING", reason: "" });
  const called = rows.filter((row) => row.called).length;
  function update(playerId, patch) {
    setCalls((current) => ({ ...current, [playerId]: { ...(current[playerId] || { playerId, called: false, response: "PENDING" }), ...patch } }));
  }
  async function save(extra) {
    try {
      const data = await send("put", `/api/convocations/${matchId}`, {
        players: squad.map((player) => {
          const row = calls[player.id] || { called: false };
          return { playerId: player.id, called: Boolean(row.called), reason: row.reason || null, reasonNote: row.reasonNote || null };
        }),
        meetingAt: kigaliIso(logistics.meetingAt),
        meetingPlace: logistics.meetingPlace,
        kitNote: logistics.kitNote,
        travelNote: logistics.travelNote,
        message: logistics.message,
        ...extra,
      });
      await onRefresh(data);
    } catch (err) { setError(err.message); }
  }
  async function post(path) {
    try {
      const data = await send("post", path);
      await onRefresh(data);
      setNotice(`${copy.sent}: ${data.sent || 0}${data.missingEmail ? ` · ${copy.missing}: ${data.missingEmail}` : ""}`);
    } catch (err) { setError(err.message); }
  }
  return (
    <div className="lineup-panel" style={{ padding: 16 }}>
      <p>{copy.called} {called}/{settings?.maxSquad || 23} · {settings?.minSquad || 16}</p>
      <div className="lineup-bar">
        {permissions.calls ? <Button onClick={() => { const next = { ...calls }; squad.forEach((player) => { if (player.availability === "AVAILABLE") next[player.id] = { ...(next[player.id] || {}), playerId: player.id, called: true, response: "PENDING" }; }); setCalls(next); }}>{copy.callAll}</Button> : null}
        {permissions.calls ? <Button onClick={() => { const next = {}; squad.forEach((player) => { next[player.id] = { playerId: player.id, called: false, response: "PENDING" }; }); setCalls(next); }}>{copy.clear}</Button> : null}
        {permissions.calls ? <Button variant="primary" onClick={() => save()}>{copy.save}</Button> : null}
        {permissions.calls ? <Button onClick={() => save({ notifyStaff: true })}>Save and notify</Button> : null}
        {permissions.calls ? <Button variant="primary" onClick={() => post(`/api/convocations/${matchId}/confirm-and-send`)}>Confirm and send</Button> : null}
        {permissions.calls ? <Button onClick={() => post(`/api/convocations/${matchId}/send`)}>{copy.send}</Button> : null}
        {permissions.calls ? <Button onClick={() => post(`/api/convocations/${matchId}/remind`)}>{copy.remind}</Button> : null}
        {permissions.calls ? <Button onClick={() => post(`/api/convocations/${matchId}/not-selected`)}>{copy.omitted}</Button> : null}
      </div>
      <div className="admin-table-wrap">
        <table className="rating-table">
          <tbody>
            {squad.map((player) => {
              const row = calls[player.id] || {};
              return (
                <tr key={player.id}>
                  <td>{face(player)}</td>
                  <td>#{player.jersey} {player.knownAs}<br />{player.positionLabel} · {copy.availability[player.availability]}</td>
                  <td><label><input type="checkbox" checked={Boolean(row.called)} disabled={!permissions.calls} onChange={(event) => update(player.id, { called: event.target.checked })} /> {row.called ? copy.used.CALLED : copy.notCalled}</label></td>
                  <td>{row.called ? (copy.responses[row.response] || copy.responses.PENDING) : (
                    <SelectInput value={row.reason || ""} disabled={!permissions.calls} onChange={(event) => update(player.id, { reason: event.target.value })}>
                      <option value="">{copy.reason}</option>
                      {REASONS.map((reason) => <option key={reason} value={reason}>{copy.reasons[reason]}</option>)}
                    </SelectInput>
                  )}</td>
                  <td>{row.notifiedAt ? "✓" : ""}{row.readAt ? "✓" : ""}{row.confirmedAt ? "✓" : ""}{player.email ? "" : ` ${copy.addEmail}`}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      <div className="lineup-layout">
        <Field label={copy.meeting}><TextInput type="datetime-local" value={logistics.meetingAt} onChange={(event) => setLogistics({ ...logistics, meetingAt: event.target.value })} /></Field>
        <Field label={copy.place}><TextInput value={logistics.meetingPlace} onChange={(event) => setLogistics({ ...logistics, meetingPlace: event.target.value })} /></Field>
        <Field label={copy.kit}><TextInput value={logistics.kitNote} onChange={(event) => setLogistics({ ...logistics, kitNote: event.target.value })} /></Field>
        <Field label={copy.travel}><TextInput value={logistics.travelNote} onChange={(event) => setLogistics({ ...logistics, travelNote: event.target.value })} /></Field>
      </div>
      <Field label={copy.message}>
        <TextArea rows={4} value={logistics.message} onChange={(event) => setLogistics({ ...logistics, message: event.target.value })} />
      </Field>
      <Button onClick={() => setLogistics({ ...logistics, message: copy.templateBody })}>{copy.template} ({locale.toUpperCase()})</Button>
    </div>
  );
}

function BenchTab({ copy, squad, draft, events, permissions, sub, setSub, matchId, onRefresh, setError }) {
  const subs = (events || []).filter((event) => event.type === "SUB_OUT" || event.type === "SUB_IN");
  const xi = Object.values(draft.starters).map((id) => playerById(squad, id)).filter(Boolean);
  const bench = draft.bench.map((id) => playerById(squad, id)).filter(Boolean);
  return (
    <div className="lineup-panel" style={{ padding: 16 }}>
      <div className="bench-row">{bench.map((player) => <span key={player.id} className="bench-card">{face(player)}#{player.jersey} {player.knownAs} · {player.positionLabel}</span>)}</div>
      {permissions.subs ? (
        <div className="lineup-bar">
          <SelectInput aria-label={copy.out} value={sub.outId} onChange={(event) => setSub({ ...sub, outId: event.target.value })}>
            <option value="">{copy.out}</option>
            {xi.map((player) => <option key={player.id} value={player.id}>{player.knownAs}</option>)}
          </SelectInput>
          <SelectInput aria-label={copy.inn} value={sub.inId} onChange={(event) => setSub({ ...sub, inId: event.target.value })}>
            <option value="">{copy.inn}</option>
            {bench.map((player) => <option key={player.id} value={player.id}>{player.knownAs}</option>)}
          </SelectInput>
          <TextInput aria-label={copy.minute} type="number" min="1" max="120" value={sub.minute} onChange={(event) => setSub({ ...sub, minute: event.target.value })} />
          <SelectInput aria-label={copy.subReason} value={sub.reason} onChange={(event) => setSub({ ...sub, reason: event.target.value })}>
            <option value="tactical">{copy.tactical}</option>
            <option value="injury">{copy.injury}</option>
          </SelectInput>
          <Button variant="primary" onClick={async () => {
            try { await onRefresh(await send("post", `/api/lineups/${matchId}/substitutions`, { outId: Number(sub.outId), inId: Number(sub.inId), minute: Number(sub.minute), reason: sub.reason })); }
            catch (err) { setError(err.message); }
          }}>{copy.record}</Button>
        </div>
      ) : null}
      <h2>{copy.entrants}</h2>
      {!subs.length ? <p>{copy.noEvents}</p> : subs.map((event) => (
        <p key={event.id}>{event.minute}&apos; {event.type === "SUB_OUT" ? "↑" : "↓"} {event.playerName}</p>
      ))}
    </div>
  );
}

function Ratings({ copy, sheet, squad, permissions, executive, matchId, onRefresh, setError, leaderboard, rateMode, setRateMode }) {
  const [focus, setFocus] = useState(0);
  const rows = (sheet.slots || []).map((slot) => {
    const rating = (sheet.ratings || []).find((row) => row.playerId === slot.playerId) || {};
    const player = playerById(squad, slot.playerId);
    return { ...slot, ...rating, photoUrl: slot.photoUrl, knownAs: slot.name, form: player?.form };
  });
  const numbers = rows.map((row) => Number(row.final)).filter((value) => !Number.isNaN(value));
  const best = rows.filter((row) => row.final != null).sort((a, b) => Number(b.final) - Number(a.final))[0];
  const worst = rows.filter((row) => row.final != null).sort((a, b) => Number(a.final) - Number(b.final))[0];
  async function rate(row, value, motm) {
    try {
      await onRefresh(await send("put", `/api/matches/${matchId}/ratings`, { playerId: row.playerId, rating: value, comment: row.comment || "", motm: Boolean(motm) }));
    } catch (err) { setError(err.message); }
  }
  return (
    <div className="lineup-panel" style={{ padding: 16 }}>
      {!rows.length ? <p>{copy.noRatings}</p> : null}
      <div className="lineup-bar">
        {permissions.rate ? <Button onClick={() => setRateMode((on) => !on)}>{copy.rateAll}</Button> : null}
        {executive && sheet.lineup ? <Button onClick={async () => { try { await onRefresh(await send("post", `/api/matches/${matchId}/ratings/close`)); } catch (err) { setError(err.message); } }}>{copy.closeRatings}</Button> : null}
      </div>
      <div className="admin-table-wrap">
        <table className="rating-table">
          <thead><tr><th>{copy.player}</th><th>{copy.minutesPlayed}</th><th>{copy.system}</th><th>{copy.rate}</th><th>{copy.final}</th><th>{copy.motm}</th></tr></thead>
          <tbody>
            {rows.map((row, index) => (
              <tr key={row.playerId} className={rateMode && focus === index ? "current" : ""}>
                <td>{face(row)} {row.knownAs}<br />{row.position}</td>
                <td>{row.minutes ?? "—"} · {row.goals || 0}/{row.assists || 0}</td>
                <td>{row.system ?? "—"}</td>
                <td>{permissions.rate ? <input type="range" min="1" max="10" step="0.5" value={row.coach || 6} aria-label={copy.rate} onChange={(event) => rate(row, Number(event.target.value), row.motm)} onKeyDown={(event) => { if (event.key === "ArrowDown") setFocus(Math.min(rows.length - 1, index + 1)); if (event.key === "ArrowUp") setFocus(Math.max(0, index - 1)); }} /> : row.coach ?? "—"}</td>
                <td><span className={`rate ${rateTone(row.final)}`}>{row.final ?? "—"}{row.auto ? ` ${copy.auto}` : ""}</span>{spark(row.last)}</td>
                <td>{permissions.rate ? <input type="checkbox" checked={Boolean(row.motm)} aria-label={copy.motm} onChange={(event) => rate(row, row.coach || 6, event.target.checked)} /> : row.motm ? copy.motm : ""}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <p>{best ? `${copy.best}: ${best.knownAs} ${best.final}` : ""} {worst && worst !== best ? `${copy.worst}: ${worst.knownAs} ${worst.final}` : ""}</p>
      <ChartCard title={copy.chart} status="ready" empty={!numbers.length ? copy.noRatings : ""}>
        <div className="bench-row">{rows.filter((row) => row.final != null).map((row) => <span key={row.playerId}>{row.knownAs} {row.final}</span>)}</div>
      </ChartCard>
      <h2>{copy.leaderboard}</h2>
      {!leaderboard.length ? <p>{copy.noRatings}</p> : leaderboard.map((row) => <p key={row.playerId}>{row.name} · {row.average} · {row.matches}</p>)}
    </div>
  );
}

function spark(values) {
  if (!values || values.length < 2) return null;
  const nums = values.map(Number);
  const min = Math.min(...nums);
  const max = Math.max(...nums);
  const points = nums.map((value, index) => `${(index / (nums.length - 1)) * 72},${24 - ((value - min) / ((max - min) || 1)) * 20}`).join(" ");
  return <svg className="spark" viewBox="0 0 72 24" aria-hidden="true"><polyline fill="none" stroke="#a3283a" strokeWidth="2" points={points} /></svg>;
}

function Timeline({ copy, locale, events, squad, permissions, form, setForm, matchId, onRefresh, setError }) {
  async function add() {
    try {
      await send("post", `/api/matches/${matchId}/events`, {
        minute: Number(form.minute), type: form.type, playerId: Number(form.playerId), relatedPlayerId: form.relatedPlayerId ? Number(form.relatedPlayerId) : null, note: form.note,
      });
      await onRefresh(await getData(`/api/lineups/${matchId}`));
    } catch (err) { setError(err.message); }
  }
  return (
    <div className="lineup-panel" style={{ padding: 16 }}>
      {!(events || []).length ? <p>{copy.noEvents}</p> : (events || []).map((event) => (
        <p key={event.id}>{event.minute}&apos; {event.type} · {event.playerName}{event.relatedPlayerName ? ` · ${event.relatedPlayerName}` : ""}{event.note ? ` · ${event.note}` : ""}</p>
      ))}
      {permissions.edit ? (
        <div className="lineup-bar">
          <TextInput aria-label={copy.minute} type="number" min="0" max="120" value={form.minute} onChange={(event) => setForm({ ...form, minute: event.target.value })} />
          <SelectInput aria-label={copy.eventType} value={form.type} onChange={(event) => setForm({ ...form, type: event.target.value })}>
            {["GOAL", "YELLOW_CARD", "RED_CARD", "INJURY"].map((type) => <option key={type}>{type}</option>)}
          </SelectInput>
          <SelectInput aria-label={copy.player} value={form.playerId} onChange={(event) => setForm({ ...form, playerId: event.target.value })}>
            <option value="">{copy.player}</option>
            {squad.map((player) => <option key={player.id} value={player.id}>{player.knownAs}</option>)}
          </SelectInput>
          <SelectInput aria-label={copy.assist} value={form.relatedPlayerId} onChange={(event) => setForm({ ...form, relatedPlayerId: event.target.value })}>
            <option value="">{copy.assist}</option>
            {squad.map((player) => <option key={player.id} value={player.id}>{player.knownAs}</option>)}
          </SelectInput>
          <Button variant="primary" onClick={add}>{copy.addEvent}</Button>
        </div>
      ) : null}
    </div>
  );
}

function Kpi({ value, max, label }) {
  const shown = useCount(value);
  const tone = max == null ? "neutral" : value === max ? "complete" : value > max ? "over" : "short";
  const width = max ? Math.min(100, Math.round((Number(value) / max) * 100)) : 0;
  return (
    <article className={`stat-card ${tone}`}>
      <b>{max == null ? shown : `${shown}/${max}`}</b>
      <span>{label}</span>
      {max != null ? <i style={{ width: `${width}%` }} /> : null}
    </article>
  );
}

function useCount(value) {
  const [n, setN] = useState(0);
  useEffect(() => {
    const target = Number(value) || 0;
    if (reduced()) {
      setN(target);
      return undefined;
    }
    const start = performance.now();
    let frame = 0;
    const tick = (now) => {
      const t = Math.min(1, (now - start) / 480);
      setN(Math.round(target * t));
      if (t < 1) frame = requestAnimationFrame(tick);
    };
    frame = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(frame);
  }, [value]);
  return n;
}

function History({ copy, locale, rows, page, setPage, size, setSize, filter, setFilter, canRestore, onRestore }) {
  const [mode, setMode] = useState("versions");
  const [compare, setCompare] = useState(null);
  const [restore, setRestore] = useState(null);
  const versions = rows.filter((row) => row.kind === "version" || row.players);
  const activity = rows.filter((row) => row.kind === "activity" || (row.action && !row.players));
  const source = (mode === "versions" ? versions : mode === "activity" ? activity : rows)
    .filter((row) => filter === "Any" || row.status === filter || row.action === filter);
  const pages = Math.max(1, Math.ceil(source.length / size));
  const safe = Math.min(page, pages - 1);
  const slice = source.slice(safe * size, safe * size + size);
  const actionStatus = { submit: "SUBMITTED", approve: "APPROVED", publish: "PUBLISHED", unlock: "LOCKED", reject: "CHANGES_REQUESTED", changes: "CHANGES_REQUESTED" };
  return (
    <div className="lineup-panel history-panel">
      <header className="panel-head">
        <div className="view-switch" role="tablist">
          {[["versions", copy.versions || "Versions"], ["activity", copy.activity || "Activity"], ["all", copy.any]].map(([id, label]) => (
            <button key={id} type="button" className={mode === id ? "on" : ""} onClick={() => { setMode(id); setPage(0); }}>{label}</button>
          ))}
        </div>
        <SelectInput aria-label={copy.filter} value={filter} onChange={(event) => { setFilter(event.target.value); setPage(0); }}>
          <option value="Any">{copy.any}</option>
          {STEP_KEYS.map((key) => <option key={key} value={key}>{copy.statuses[key]}</option>)}
        </SelectInput>
      </header>
      {!slice.length ? <p>{copy.historyEmpty}</p> : null}
      <ol className="history-list">
        {slice.map((row) => {
          const diff = row.diff || {};
          return (
            <li key={`${row.kind}-${row.revision}-${row.action || row.status}-${row.at}`} className={row.current ? "is-current" : ""}>
              <div>
                {row.revision ? <Badge tone={row.current ? "bad" : "neutral"}>v{row.revision}</Badge> : null}
                <Badge>{copy.statuses[row.status] || copy.statuses[actionStatus[row.action]] || row.action}</Badge>
                {row.current ? <Badge tone="bad">{copy.current || "Current"}</Badge> : null}
                <b>{row.by || "—"}</b>
                <time>{when(locale, row.at)}</time>
                <p>{row.comment || "—"}</p>
                {row.formationFrom ? <span className="diff-chip">{row.formationFrom} → {row.formation}</span> : null}
                {row.squadFrom ? <span className="diff-chip">{row.squadFrom} → {row.squadSize}</span> : null}
                {(diff.added || []).slice(0, 4).map((player) => <span key={player.playerId} className="diff-chip good">+ {player.name}</span>)}
                {(diff.removed || []).slice(0, 4).map((player) => <span key={player.playerId} className="diff-chip bad">− {player.name}</span>)}
                {(diff.moved || []).slice(0, 4).map((player) => <span key={player.playerId} className="diff-chip warn">{player.from} → {player.to}</span>)}
              </div>
              <div className="row-actions">
                {row.players ? <Button onClick={() => setCompare(row)}>{copy.compare || "Compare"}</Button> : null}
                {canRestore && row.revision && row.players ? <Button onClick={() => setRestore(row)}>{copy.restore}</Button> : null}
              </div>
            </li>
          );
        })}
      </ol>
      <div className="pager">
        <Button disabled={safe === 0} onClick={() => setPage(safe - 1)}>{copy.prev}</Button>
        <span>{copy.showing} {source.length ? safe * size + 1 : 0}-{Math.min(source.length, safe * size + size)} {copy.of} {source.length}</span>
        <SelectInput aria-label={copy.showing} value={size} onChange={(event) => { setSize(Number(event.target.value)); setPage(0); }}>
          {[8, 12, 20].map((count) => <option key={count} value={count}>{count}</option>)}
        </SelectInput>
        <Button disabled={safe >= pages - 1} onClick={() => setPage(safe + 1)}>{copy.next}</Button>
      </div>
      {compare ? (
        <Modal title={`v${compare.revision}`} onClose={() => setCompare(null)} closeLabel={copy.cancel} size="lg">
          <div className="compare-grid">
            {(compare.players || []).map((player) => <span key={player.playerId} className="mini-chip">{player.slot} · {player.name}</span>)}
          </div>
        </Modal>
      ) : null}
      {restore ? (
        <Modal title={copy.restore} onClose={() => setRestore(null)} closeLabel={copy.cancel}
          footer={<><Button onClick={() => setRestore(null)}>{copy.cancel}</Button><Button variant="primary" onClick={() => { onRestore(restore.revision); setRestore(null); }}>{copy.restore}</Button></>}>
          <p>v{restore.revision} · {when(locale, restore.at)}</p>
        </Modal>
      ) : null}
    </div>
  );
}

function versionDiff(current, previous) {
  const before = new Map((previous.players || []).map((player) => [player.playerId, player]));
  const after = new Map((current.players || []).map((player) => [player.playerId, player]));
  const added = [];
  const removed = [];
  const moved = [];
  after.forEach((player, id) => {
    if (!before.has(id)) added.push(player.name);
    else if (before.get(id).slot !== player.slot) moved.push(player.name);
  });
  before.forEach((player, id) => { if (!after.has(id)) removed.push(player.name); });
  return { added, removed, moved };
}
