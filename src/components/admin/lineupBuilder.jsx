"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { motion } from "framer-motion";
import { Check, ChevronLeft, ChevronRight, Download, Minus, Plus, RotateCcw, Users } from "lucide-react";
import { api, API_URL } from "@/services/api";
import { Avatar, Badge, Button, Field, Modal, SelectInput, Stepper, TextArea, TextInput } from "@/ui/kit";
import { XiViews } from "./lineupPitch";

const LINES = ["GK", "DEF", "MID", "ATT"];
const FITS = {
  GK: ["GOALKEEPER"],
  LB: ["LEFT_BACK"],
  RB: ["RIGHT_BACK"],
  LWB: ["LEFT_BACK", "WINGER"],
  RWB: ["RIGHT_BACK", "WINGER"],
  CB: ["CENTER_BACK"],
  LCB: ["CENTER_BACK"],
  RCB: ["CENTER_BACK"],
  CDM: ["DEFENSIVE_MIDFIELDER", "CENTRAL_MIDFIELDER"],
  DM: ["DEFENSIVE_MIDFIELDER", "CENTRAL_MIDFIELDER"],
  LDM: ["DEFENSIVE_MIDFIELDER", "CENTRAL_MIDFIELDER"],
  RDM: ["DEFENSIVE_MIDFIELDER", "CENTRAL_MIDFIELDER"],
  CM: ["CENTRAL_MIDFIELDER", "DEFENSIVE_MIDFIELDER", "OFFENSIVE_MIDFIELDER"],
  LCM: ["CENTRAL_MIDFIELDER", "DEFENSIVE_MIDFIELDER", "OFFENSIVE_MIDFIELDER"],
  RCM: ["CENTRAL_MIDFIELDER", "DEFENSIVE_MIDFIELDER", "OFFENSIVE_MIDFIELDER"],
  CAM: ["OFFENSIVE_MIDFIELDER", "FORWARD"],
  AM: ["OFFENSIVE_MIDFIELDER", "FORWARD"],
  SS: ["OFFENSIVE_MIDFIELDER", "FORWARD"],
  LM: ["WINGER", "OFFENSIVE_MIDFIELDER", "FORWARD"],
  RM: ["WINGER", "OFFENSIVE_MIDFIELDER", "FORWARD"],
  LW: ["WINGER", "OFFENSIVE_MIDFIELDER", "FORWARD"],
  RW: ["WINGER", "OFFENSIVE_MIDFIELDER", "FORWARD"],
  ST: ["FORWARD", "WINGER"],
  CF: ["FORWARD", "WINGER"],
  LS: ["FORWARD", "WINGER"],
  RS: ["FORWARD", "WINGER"],
};

function reduced() {
  return typeof window !== "undefined" && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

function photo(url) {
  if (!url) return "";
  return url.startsWith("http") ? url : `${API_URL}${url}`;
}

export function recommended(size) {
  const gk = size >= 12 ? 2 : 1;
  const rest = Math.max(0, size - gk);
  let def = Math.round((rest * 6) / 16);
  let mid = Math.round((rest * 6) / 16);
  let att = rest - def - mid;
  if (att < 1 && rest >= 3) {
    if (def >= mid && def > 1) def -= 1;
    else if (mid > 1) mid -= 1;
    att += 1;
  }
  return { GK: gk, DEF: Math.max(def, 0), MID: Math.max(mid, 0), ATT: Math.max(att, 0) };
}

function lineOf(player) {
  return player?.lineCode || (player?.line === "Goalkeepers" ? "GK" : player?.line === "Defenders" ? "DEF" : player?.line === "Midfielders" ? "MID" : player?.line === "Attackers" ? "ATT" : "MID");
}

function fits(code, position) {
  return (FITS[code] || []).includes(position);
}

function spotsOf(sheet, formation) {
  return (sheet?.formations || []).find((item) => item.code === formation)?.spots || (sheet?.formations || [])[0]?.spots || [];
}

function playerById(squad, id) {
  return (squad || []).find((player) => player.id === id) || null;
}

async function send(method, path, body) {
  const response = await api({ method, url: path, data: body });
  if (!response.data?.success) throw new Error(response.data?.message || "Request failed");
  return response.data.data;
}

function tone(state) {
  if (state === "AVAILABLE") return "good";
  if (state === "DOUBTFUL") return "warn";
  if (state === "INJURED" || state === "SUSPENDED" || state === "UNAVAILABLE") return "bad";
  return "neutral";
}

export function LineupBuilder({
  sheet, draft, patch, flow, copy, locale, permissions, executive, captains, matchId, onRefresh, user, setNotice, setError,
}) {
  const squad = sheet?.squad || [];
  const lineup = sheet?.lineup;
  const [step, setStep] = useState(0);
  const [query, setQuery] = useState("");
  const [availableOnly, setAvailableOnly] = useState(false);
  const [pickedSlot, setPickedSlot] = useState(null);
  const [profile, setProfile] = useState(null);
  const [showOpponent, setShowOpponent] = useState(false);
  const [custom, setCustom] = useState(draft?.squadSize || 18);
  const [saving, setSaving] = useState(false);
  const [savedAt, setSavedAt] = useState("");
  const [preview, setPreview] = useState(null);
  const [menu, setMenu] = useState(false);
  const [success, setSuccess] = useState(false);
  const [comment, setComment] = useState(lineup?.reviewComment || "");
  const [meeting, setMeeting] = useState(lineup?.meetingPlace || "");
  const [override, setOverride] = useState("");
  const [mobileList, setMobileList] = useState(false);
  const [asDefault, setAsDefault] = useState(false);
  const [sizeReason, setSizeReason] = useState("");
  const [xiView, setXiView] = useState("stadium");
  const [skin, setSkin] = useState("photo");
  const [tilt, setTilt] = useState(false);
  const [artPick, setArtPick] = useState({ template: "photo", ratio: "portrait" });
  const [subForm, setSubForm] = useState(null);
  const history = useRef([]);
  const future = useRef([]);
  const drag = useRef(null);
  const skip = useRef(true);

  const plan = draft?.plan || recommended(draft?.squadSize || 18);
  const called = draft?.called || [];
  const editable = Boolean(permissions.edit) && (!lineup?.status || lineup.status === "DRAFT" || lineup.status === "CHANGES_REQUESTED");
  const rule = sheet?.settings?.competitionRule;

  useEffect(() => {
    setStep(0);
    setComment(lineup?.reviewComment || "");
    setMeeting(lineup?.meetingPlace || "");
    skip.current = true;
  }, [matchId]);

  useEffect(() => {
    if (!draft || !editable) return undefined;
    if (skip.current) {
      skip.current = false;
      return undefined;
    }
    const timer = setTimeout(() => { persist(); }, 900);
    return () => clearTimeout(timer);
  }, [draft, editable]);

  const counts = useMemo(() => {
    const map = { GK: 0, DEF: 0, MID: 0, ATT: 0 };
    called.forEach((id) => {
      const player = playerById(squad, id);
      if (player) map[lineOf(player)] += 1;
    });
    return map;
  }, [called, squad]);

  const availableByLine = useMemo(() => {
    const map = { GK: 0, DEF: 0, MID: 0, ATT: 0 };
    squad.forEach((player) => {
      if (player.availability === "AVAILABLE" || player.availability === "DOUBTFUL") map[lineOf(player)] += 1;
    });
    return map;
  }, [squad]);

  const issues = useMemo(() => validate(step, draft, plan, counts, squad, sheet, flow), [step, draft, plan, counts, squad, sheet, flow]);
  const spots = spotsOf(sheet, draft?.formation || "4-3-3");

  function remember() {
    history.current = [...history.current.slice(-19), { starters: draft.starters, bench: draft.bench, formation: draft.formation }];
    future.current = [];
  }

  function update(next) {
    skip.current = false;
    patch(next);
  }

  async function persist() {
    if (!draft || !editable) return;
    const quota = LINES.reduce((total, line) => total + (plan[line] || 0), 0);
    if (quota !== draft.squadSize) return;
    setSaving(true);
    try {
      let version = sheet.lineup?.version ?? null;
      let data = await send("put", `/api/lineups/${matchId}/squad-size`, { squadSize: draft.squadSize, plan, version, useAsDefault: asDefault, overrideReason: sizeReason || null });
      version = data.lineup?.version ?? version;
      const players = squad.map((player) => ({
        playerId: player.id,
        line: lineOf(player),
        called: called.includes(player.id),
        benchOrder: draft.bench.indexOf(player.id),
        reasonNotCalled: called.includes(player.id) ? null : (draft.reasons?.[player.id] || (player.availability === "INJURED" ? "Injured" : player.availability === "SUSPENDED" ? "Suspended" : "Tactical")),
        overrideReason: draft.overrides?.[player.id] || null,
      }));
      data = await send("put", `/api/lineups/${matchId}/squad`, { players, version });
      version = data.lineup?.version ?? version;
      const slots = spots.map((spot) => {
        const playerId = draft.starters?.[spot.code];
        if (!playerId || !called.includes(playerId)) return null;
        return { playerId, positionSlot: spot.code, x: spot.x, y: spot.y, captain: playerId === draft.captainId, overrideReason: draft.overrides?.[playerId] || null };
      }).filter(Boolean);
      data = await send("put", `/api/lineups/${matchId}/xi`, { formation: draft.formation, slots, version, opponentFormation: showOpponent ? draft.opponentFormation : null });
      skip.current = true;
      await onRefresh(data);
      setSavedAt(new Intl.DateTimeFormat(locale === "fr" ? "fr-FR" : "en-GB", { timeZone: "Africa/Kigali", hour: "2-digit", minute: "2-digit", hourCycle: "h23" }).format(new Date()));
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  function chooseSize(size) {
    const nextPlan = recommended(size);
    update({ squadSize: size, plan: nextPlan });
  }

  function nudge(line, delta) {
    const next = { ...plan, [line]: Math.max(0, (plan[line] || 0) + delta) };
    update({ plan: next });
  }

  function toggle(player) {
    if (!editable) return;
    const blocked = player.availability === "INJURED" || player.availability === "SUSPENDED" || player.availability === "UNAVAILABLE";
    if (blocked && !executive) return;
    if (blocked && executive && !draft.overrides?.[player.id]) {
      const reason = window.prompt(flow.override);
      if (!reason) return;
      update({ overrides: { ...draft.overrides, [player.id]: reason } });
    }
    if (called.includes(player.id)) {
      update({
        called: called.filter((id) => id !== player.id),
        starters: Object.fromEntries(Object.entries(draft.starters).filter(([, id]) => id !== player.id)),
        bench: draft.bench.filter((id) => id !== player.id),
      });
      return;
    }
    if (called.length >= draft.squadSize) {
      setError(flow.full);
      return;
    }
    if (counts[lineOf(player)] >= (plan[lineOf(player)] || 0)) {
      setError(flow.needMore(flow.lines[lineOf(player)]));
      return;
    }
    update({ called: [...called, player.id], bench: draft.bench.includes(player.id) ? draft.bench : [...draft.bench, player.id] });
  }

  function place(code, player) {
    if (!editable || !player || !called.includes(player.id)) return;
    const blocked = player.availability === "INJURED" || player.availability === "SUSPENDED" || player.availability === "UNAVAILABLE";
    if (blocked && !(executive && draft.overrides?.[player.id])) return;
    remember();
    const starters = { ...draft.starters };
    Object.keys(starters).forEach((slot) => { if (starters[slot] === player.id) delete starters[slot]; });
    const previous = starters[code];
    starters[code] = player.id;
    let bench = draft.bench.filter((id) => id !== player.id);
    if (previous && previous !== player.id) bench = [previous, ...bench.filter((id) => id !== previous)];
    called.filter((id) => id !== player.id && !Object.values(starters).includes(id) && !bench.includes(id)).forEach((id) => bench.push(id));
    update({ starters, bench });
    setPickedSlot(null);
  }

  function clearSlot(code) {
    if (!editable || !draft.starters[code]) return;
    remember();
    const id = draft.starters[code];
    const starters = { ...draft.starters };
    delete starters[code];
    update({ starters, bench: draft.bench.includes(id) ? draft.bench : [...draft.bench, id] });
  }

  function changeFormation(code) {
    const previous = spotsOf(sheet, draft.formation);
    const nextSpots = spotsOf(sheet, code);
    const used = new Set();
    const starters = {};
    Object.entries(draft.starters).forEach(([slot, playerId]) => {
      const old = previous.find((spot) => spot.code === slot);
      const pool = nextSpots.filter((spot) => !used.has(spot.code));
      pool.sort((a, b) => {
        const aScore = (a.line === old?.line ? 0 : 1) * 1000 + Math.hypot(a.x - (old?.x || 50), a.y - (old?.y || 50));
        const bScore = (b.line === old?.line ? 0 : 1) * 1000 + Math.hypot(b.x - (old?.x || 50), b.y - (old?.y || 50));
        return aScore - bScore;
      });
      if (pool[0]) {
        starters[pool[0].code] = playerId;
        used.add(pool[0].code);
      }
    });
    remember();
    update({ formation: code, starters });
  }

  async function confirm() {
    setError("");
    try {
      await persist();
      const data = await send("post", `/api/lineups/${matchId}/confirm`, { comment, meetingPlace: meeting, overrideReason: override || null, version: sheet.lineup?.version ?? null });
      skip.current = true;
      await onRefresh(data);
      setSuccess(true);
      setNotice(flow.success);
    } catch (err) {
      setError(err.message);
    }
  }

  async function download(kind) {
    const path = kind === "pdf" ? `/api/lineups/${matchId}/pdf?lang=${locale}` : `/api/lineups/${matchId}/graphic?template=${artPick.template}&ratio=${artPick.ratio}`;
    const response = await api.get(path, { responseType: "blob" });
    const blob = response.data instanceof Blob ? response.data : new Blob([response.data]);
    const url = URL.createObjectURL(blob);
    setPreview({ url, kind, name: kind === "pdf" ? "Viking_FC_Matchsheet.pdf" : "Viking_FC_Matchday.png" });
    setMenu(false);
  }

  const filtered = squad.filter((player) => {
    if (availableOnly && player.availability !== "AVAILABLE" && player.availability !== "DOUBTFUL") return false;
    const blob = `${player.knownAs} ${player.fullName} ${player.jersey || ""}`.toLowerCase();
    return blob.includes(query.trim().toLowerCase());
  }).sort((a, b) => (a.jersey || 999) - (b.jersey || 999));

  const calledPlayers = called.map((id) => playerById(squad, id)).filter(Boolean);
  const benchPlayers = (draft.bench || []).map((id) => playerById(squad, id)).filter((player) => player && !Object.values(draft.starters).includes(player.id));
  const primary = step < 3 ? flow.next : executive ? flow.approvePublish : permissions.submit ? flow.submitAdmin : flow.confirm;
  const blocked = issues.length > 0;

  return (
    <div className="builder-card">
      <div className="builder-body">
        <Stepper steps={flow.steps} current={step} onSelect={setStep} label={flow.steps[step]} />
        <p className="save-line">{saving ? copy.saving : savedAt ? `${copy.saved} ${savedAt}` : ""}{lineup?.editingBy && lineup.editingBy !== user?.email ? ` · ${copy.editing}: ${lineup.editingBy}` : ""}</p>
        {!editable ? <p className="banner">{flow.readOnly}</p> : null}
        {issues.length ? <p className="banner" role="status">{issues.join(" · ")}</p> : null}

        {step === 0 ? (
          <div className="size-stack">
            <section className="size-panel">
              <header>
                <h2>{flow.sizeTitle}</h2>
                <p>{flow.thisMatch}</p>
              </header>
              {rule?.label ? <p className="lineup-meta">{flow.competitionRule}: {rule.label}</p> : null}
              <div className="size-grid">
                {[15, 16, 18, 20].map((size) => (
                  <motion.button key={size} type="button" className={`size-card${draft.squadSize === size ? " on" : ""}`} disabled={!editable} onClick={() => chooseSize(size)} whileTap={reduced() ? undefined : { scale: 0.98 }}>
                    {draft.squadSize === size ? <Check size={16} /> : <Users size={16} />}
                    <strong>{size}</strong>
                    <span>{flow.split(size - 11)}</span>
                    {size === (rule?.squadSize || 18) ? <Badge tone="bad">{flow.recommended}</Badge> : null}
                  </motion.button>
                ))}
                <div className={`size-card${! [15, 16, 18, 20].includes(draft.squadSize) ? " on" : ""}`}>
                  <strong>{flow.custom}</strong>
                  <div className="num-step">
                    <Button icon={<Minus size={16} />} aria-label={flow.custom} disabled={!editable} onClick={() => { const next = Math.max(sheet?.settings?.minSquad || 12, Number(custom || draft.squadSize) - 1); setCustom(next); chooseSize(next); }} />
                    <input aria-label={flow.custom} type="number" min={sheet?.settings?.minSquad || 12} max={sheet?.settings?.maxSquad || 23} value={custom} disabled={!editable} onChange={(event) => { setCustom(event.target.value); const next = Number(event.target.value); if (next) chooseSize(next); }} />
                    <Button icon={<Plus size={16} />} aria-label={flow.custom} disabled={!editable} onClick={() => { const next = Math.min(sheet?.settings?.maxSquad || 23, Number(custom || draft.squadSize) + 1); setCustom(next); chooseSize(next); }} />
                  </div>
                  <span>{flow.thisMatch}</span>
                </div>
              </div>
              {executive ? <label className="tool-switch"><input type="checkbox" checked={asDefault} onChange={(event) => setAsDefault(event.target.checked)} /> {flow.defaultFor(rule?.competition || matchLabel(sheet))}</label> : null}
              {draft.squadSize > (rule?.squadSize || 18) ? <Field label={flow.override}><TextInput value={sizeReason} onChange={(event) => setSizeReason(event.target.value)} /></Field> : null}
            </section>
            {called.length > draft.squadSize ? (
              <p className="banner">{flow.willRemove(called.length - draft.squadSize)} <Button onClick={() => {
                const extra = called.length - draft.squadSize;
                const drop = new Set(called.slice(-extra));
                update({
                  called: called.filter((id) => !drop.has(id)),
                  starters: Object.fromEntries(Object.entries(draft.starters).filter(([, id]) => !drop.has(id))),
                  bench: draft.bench.filter((id) => !drop.has(id)),
                });
              }}>{flow.applySize}</Button></p>
            ) : null}
            <section className="size-panel">
              <header className="panel-head">
                <div>
                  <h2>{flow.distribution}</h2>
                  <Badge tone={LINES.reduce((sum, line) => sum + (plan[line] || 0), 0) === draft.squadSize ? "good" : "bad"}>{LINES.reduce((sum, line) => sum + (plan[line] || 0), 0)} / {draft.squadSize}</Badge>
                </div>
                <Button icon={<RotateCcw size={16} />} disabled={!editable} onClick={() => update({ plan: recommended(draft.squadSize) })}>{flow.reset}</Button>
              </header>
              <div className="quota-edit">
                {LINES.map((line) => (
                  <div key={line}>
                    <b>{flow.lines[line]}</b>
                    <span>{flow.available(availableByLine[line])}</span>
                    <div className="num-step">
                      <Button icon={<Minus size={16} />} aria-label={line} disabled={!editable} onClick={() => nudge(line, -1)} />
                      <strong>{plan[line] || 0}</strong>
                      <Button icon={<Plus size={16} />} aria-label={line} disabled={!editable} onClick={() => nudge(line, 1)} />
                    </div>
                  </div>
                ))}
              </div>
            </section>
          </div>
        ) : null}

        {step === 1 ? (
          <div>
            <div className="lineup-bar">
              <TextInput aria-label={copy.search} placeholder={copy.search} value={query} onChange={(event) => setQuery(event.target.value)} />
              <label><input type="checkbox" checked={availableOnly} onChange={(event) => setAvailableOnly(event.target.checked)} /> {copy.availableOnly}</label>
              {editable ? <Button onClick={async () => { try { skip.current = true; await onRefresh(await send("post", `/api/lineups/${matchId}/suggest-squad`)); } catch (err) { setError(err.message); } }}>{flow.suggest}</Button> : null}
            </div>
            <div className="line-columns">
              {LINES.map((line) => {
                const rows = filtered.filter((player) => lineOf(player) === line);
                const exact = counts[line] === (plan[line] || 0);
                const over = counts[line] > (plan[line] || 0);
                return (
                  <section key={line}>
                    <header>
                      <b>{flow.lines[line]}</b>
                      <Badge tone={exact ? "good" : over ? "bad" : "warn"}>{counts[line]}/{plan[line] || 0}</Badge>
                    </header>
                    <div className="player-stack">
                      {rows.map((player) => {
                        const on = called.includes(player.id);
                        const blocked = player.availability === "INJURED" || player.availability === "SUSPENDED" || player.availability === "UNAVAILABLE";
                        return (
                          <button key={player.id} type="button" className={`player-card${on ? " on" : ""}`} disabled={!editable || (blocked && !executive)} title={blocked ? copy.availability[player.availability] : ""} onClick={() => toggle(player)}>
                            <Avatar name={player.knownAs} src={photo(player.photoUrl)} />
                            <span>
                              <b>#{player.jersey} {player.knownAs}</b>
                              <small>{flow.positions[player.position] || player.positionLabel} · {copy.age} {player.age ?? "—"} · {copy.form} {player.form ?? "—"}</small>
                            </span>
                            <Badge tone={tone(player.availability)}>{copy.availability[player.availability] || flow.doubtful}</Badge>
                            {on ? <Check size={16} /> : null}
                            {captains[player.id] ? <Badge>{copy.ranks[captains[player.id]]}</Badge> : null}
                          </button>
                        );
                      })}
                    </div>
                  </section>
                );
              })}
            </div>
            <div className="selected-strip">
              <b>{called.length}/{draft.squadSize}</b>
              <Donut counts={counts} plan={plan} label={flow.distribution} />
              {calledPlayers.map((player) => (
                <button key={player.id} type="button" className="mini-chip" onClick={() => toggle(player)} disabled={!editable}>
                  <Avatar name={player.knownAs} src={photo(player.photoUrl)} />
                  {player.knownAs}
                  <span aria-hidden="true">×</span>
                </button>
              ))}
            </div>
          </div>
        ) : null}

        {step === 2 ? (
          <div className="xi-layout">
            <div>
              <div className="xi-tools" role="toolbar" aria-label={copy.formation}>
                {(sheet.formations || []).map((item) => (
                  <button key={item.code} type="button" className={`form-chip${draft.formation === item.code ? " on" : ""}`} disabled={!editable} title={item.code} onClick={() => changeFormation(item.code)}>
                    <MiniForm spots={item.spots || []} />
                    <span>{item.code}</span>
                  </button>
                ))}
                <label className="tool-switch"><input type="checkbox" role="switch" checked={showOpponent} onChange={(event) => setShowOpponent(event.target.checked)} /> {copy.showOpponent}</label>
                <Button disabled={!editable} title={flow.clearXi} onClick={() => { remember(); update({ starters: {} }); }}>{flow.clearXi}</Button>
                <Button disabled={!editable} title={flow.autoXi} onClick={async () => { try { skip.current = true; await onRefresh(await send("post", `/api/lineups/${matchId}/auto-xi`)); } catch (err) { setError(err.message); } }}>{flow.autoXi}</Button>
                <Button disabled={!history.current.length} title={flow.undo} onClick={() => { const prev = history.current.pop(); if (!prev) return; future.current.push({ starters: draft.starters, bench: draft.bench, formation: draft.formation }); update(prev); }}>{flow.undo}</Button>
                <Button disabled={!future.current.length} title={flow.redo} onClick={() => { const next = future.current.pop(); if (!next) return; history.current.push({ starters: draft.starters, bench: draft.bench, formation: draft.formation }); update(next); }}>{flow.redo}</Button>
                <Button disabled={!editable} title={flow.copyPrev} onClick={async () => { try { skip.current = true; await onRefresh(await send("post", `/api/lineups/${matchId}/copy-previous`)); } catch (err) { setError(err.message); } }}>{flow.copyPrev}</Button>
              </div>
              <div className="view-switch" role="tablist" aria-label={flow.stadium}>
                {[["stadium", flow.stadium], ["cards", flow.cards], ["table", flow.table]].map(([id, label]) => (
                  <button key={id} type="button" className={xiView === id ? "on" : ""} onClick={() => { setXiView(id); localStorage.setItem("viking-xi-view", id); }}>{label}</button>
                ))}
              </div>
              {xiView === "stadium" ? (
                <div className="view-switch">
                  <button type="button" className={skin === "photo" ? "on" : ""} onClick={() => setSkin("photo")}>{flow.photoSkin}</button>
                  <button type="button" className={skin === "kit" ? "on" : ""} onClick={() => setSkin("kit")}>{flow.kitSkin}</button>
                  <button type="button" className={tilt ? "on" : ""} onClick={() => setTilt((value) => !value)}>{flow.tilt}</button>
                </div>
              ) : null}
              <XiViews
                view={xiView}
                skin={skin}
                tilt={tilt}
                spots={showOpponent ? spotsOf(sheet, draft.opponentFormation || "4-4-2").map((spot) => ({ ...spot, x: 100 - spot.x, y: 100 - spot.y })) : spots}
                squad={squad}
                starters={showOpponent ? {} : draft.starters}
                bench={draft.bench}
                formation={draft.formation}
                events={sheet.events}
                editable={editable && !showOpponent}
                pickedSlot={pickedSlot}
                captainId={draft.captainId}
                captains={captains}
                copy={copy}
                flow={flow}
                fits={fits}
                onPlace={(code, id) => { const person = playerById(squad, id || drag.current?.playerId); if (person) place(code, person); }}
                onPick={setPickedSlot}
                onProfile={setProfile}
                onCaptain={(id) => update({ captainId: id })}
                onClear={clearSlot}
              />
              {permissions.subs ? <Button onClick={() => setSubForm({ outId: "", inId: "", minute: "60", reason: "tactical" })}>{flow.makeSub}</Button> : null}
              {xiView === "stadium" ? <h3>{copy.bench} {benchPlayers.length}/{Math.max(0, draft.squadSize - 11)}</h3> : null}
              {xiView === "stadium" ? (
                <div className="bench-row">
                  {benchPlayers.map((player) => (
                    <button key={player.id} type="button" className="player-card bench-card" draggable={editable} onDragStart={(event) => { drag.current = { playerId: player.id }; event.dataTransfer.setData("text/plain", String(player.id)); }} onClick={() => pickedSlot ? place(pickedSlot, player) : setProfile(player)}>
                      <Avatar name={player.knownAs} src={photo(player.photoUrl)} />
                      <span><b>#{player.jersey} {player.knownAs}</b><small>{player.positionLabel}</small></span>
                    </button>
                  ))}
                </div>
              ) : null}
            </div>
            <aside className={`squad-side${mobileList ? " open" : ""}`}>
              <h3>{pickedSlot ? flow.forSlot : flow.other}</h3>
              <div className="player-stack">
                {calledPlayers
                  .slice()
                  .sort((a, b) => {
                    if (!pickedSlot) return (a.jersey || 999) - (b.jersey || 999);
                    return Number(fits(pickedSlot, b.position)) - Number(fits(pickedSlot, a.position));
                  })
                  .map((player) => {
                    const used = Object.values(draft.starters).includes(player.id);
                    return (
                      <button key={player.id} type="button" className="player-card" draggable={editable} onDragStart={(event) => { drag.current = { playerId: player.id }; event.dataTransfer.setData("text/plain", String(player.id)); }} onClick={() => (pickedSlot ? place(pickedSlot, player) : setProfile(player))}>
                        <Avatar name={player.knownAs} src={photo(player.photoUrl)} />
                        <span>
                          <b>#{player.jersey} {player.knownAs}</b>
                          <small>{flow.positions[player.position] || player.positionLabel} · {copy.form} {player.form ?? "—"}</small>
                        </span>
                        {used ? <Badge tone="bad">XI</Badge> : <Badge tone={tone(player.availability)}>{copy.availability[player.availability]}</Badge>}
                        {pickedSlot && !fits(pickedSlot, player.position) ? <Badge tone="warn">{copy.outOfPosition}</Badge> : null}
                      </button>
                    );
                  })}
              </div>
            </aside>
            <Button className="drawer-toggle" onClick={() => setMobileList((open) => !open)}>{copy.squad}</Button>
          </div>
        ) : null}

        {step === 3 ? (
          <div className="review-grid">
            <div className="pitch-stage mini">
              <div className="pitch-board" aria-hidden="true">
                <PitchFace />
                {spots.map((spot) => {
                  const player = playerById(squad, draft.starters[spot.code]);
                  return <span key={spot.code} className="pitch-token" style={{ left: `${spot.x}%`, top: `${spot.y}%` }}>{player ? <Avatar name={player.knownAs} src={photo(player.photoUrl)} /> : <i className="slot-empty">{spot.code}</i>}<small>{player?.knownAs}</small></span>;
                })}
              </div>
            </div>
            <div>
              <p><b>{draft.formation}</b> · {copy.bench} {benchPlayers.length}</p>
              {LINES.map((line) => (
                <section key={line}>
                  <h3>{flow.lines[line]}</h3>
                  <div className="bench-row">
                    {calledPlayers.filter((player) => lineOf(player) === line).map((player) => (
                      <span key={player.id} className="mini-chip"><Avatar name={player.knownAs} src={photo(player.photoUrl)} />{player.knownAs}</span>
                    ))}
                  </div>
                </section>
              ))}
              <h3>{flow.excluded}</h3>
              <div className="bench-row">
                {squad.filter((player) => !called.includes(player.id)).map((player) => (
                  <span key={player.id} className="mini-chip">{player.knownAs} · {copy.availability[player.availability] || draft.reasons?.[player.id] || "Tactical"}</span>
                ))}
              </div>
              <Field label={flow.captain}>
                <SelectInput aria-label={flow.captain} value={draft.captainId || ""} disabled={!editable} onChange={(event) => update({ captainId: Number(event.target.value) })}>
                  <option value="">{flow.captain}</option>
                  {calledPlayers.map((player) => <option key={player.id} value={player.id}>{player.knownAs}</option>)}
                </SelectInput>
              </Field>
              <Field label={flow.place}><TextInput value={meeting} disabled={!editable && !permissions.calls} onChange={(event) => setMeeting(event.target.value)} /></Field>
              <Field label={flow.comment}><TextArea rows={3} value={comment} disabled={!editable} onChange={(event) => setComment(event.target.value)} /></Field>
              {executive ? <Field label={flow.override}><TextInput value={override} onChange={(event) => setOverride(event.target.value)} /></Field> : null}
            </div>
          </div>
        ) : null}
      </div>

      <div className="builder-actions">
        <span title={step === 0 ? "" : ""}>
          <Button icon={<ChevronLeft size={16} />} disabled={step === 0} onClick={() => setStep((value) => Math.max(0, value - 1))}>{flow.back}</Button>
        </span>
        {editable ? <Button onClick={() => persist()} disabled={saving}>{copy.save}</Button> : null}
        {!editable && permissions.revise ? <Button variant="primary" onClick={async () => { try { skip.current = true; await onRefresh(await send("post", `/api/lineups/${matchId}/edit`)); setNotice(flow.edit); } catch (err) { setError(err.message); } }}>{flow.edit}</Button> : null}
        <span title={blocked ? issues.join(", ") : ""}>
          <Button variant="primary" icon={<ChevronRight size={16} />} disabled={blocked || (!editable && step === 3)} onClick={() => (step < 3 ? setStep((value) => value + 1) : confirm())}>{primary}</Button>
        </span>
        <div className="download-menu">
          <Button icon={<Download size={16} />} onClick={() => setMenu((open) => !open)}>{flow.download}</Button>
          {menu ? (
            <div role="menu">
              {[["photo", flow.templatePhoto], ["kit", flow.templateKit], ["classic", flow.templateClassic]].map(([id, label]) => (
                <button key={id} type="button" onClick={() => setArtPick((pick) => ({ ...pick, template: id }))}>{label}</button>
              ))}
              {[["portrait", flow.portrait], ["story", flow.story], ["square", flow.square]].map(([id, label]) => (
                <button key={id} type="button" onClick={() => setArtPick((pick) => ({ ...pick, ratio: id }))}>{label}</button>
              ))}
              <button type="button" onClick={() => download("png")}>PNG · {artPick.template} · {artPick.ratio}</button>
              <button type="button" onClick={() => download("pdf")}>PDF</button>
            </div>
          ) : null}
        </div>
      </div>

      {profile ? (
        <Modal title={profile.knownAs} subtitle={flow.positions[profile.position] || profile.positionLabel} onClose={() => setProfile(null)} closeLabel={copy.cancel}
          footer={<><Button onClick={() => setProfile(null)}>{copy.cancel}</Button>{editable ? <Button variant="danger" onClick={() => { const code = Object.keys(draft.starters).find((slot) => draft.starters[slot] === profile.id); if (code) clearSlot(code); setProfile(null); }}>{copy.remove}</Button> : null}</>}>
          <div className="lineup-club"><Avatar name={profile.knownAs} src={photo(profile.photoUrl)} /><div><p>{copy.age}: {profile.age ?? "—"}</p><p>{copy.form}: {profile.form ?? "—"}</p><p>{copy.availability[profile.availability]}</p></div></div>
          {editable ? <Button onClick={() => update({ captainId: profile.id })}>{flow.captain}</Button> : null}
        </Modal>
      ) : null}
      {preview ? (
        <Modal title={flow.preview} size="lg" onClose={() => { URL.revokeObjectURL(preview.url); setPreview(null); }} closeLabel={copy.cancel}
          footer={<><Button onClick={() => { URL.revokeObjectURL(preview.url); setPreview(null); }}>{copy.cancel}</Button><Button variant="primary" onClick={() => { const link = document.createElement("a"); link.href = preview.url; link.download = preview.name; link.click(); }}>{flow.download}</Button>{preview.kind === "png" ? <Button onClick={async () => { try { await navigator.clipboard.write([new ClipboardItem({ "image/png": await (await fetch(preview.url)).blob() })]); } catch (err) { setError(err.message); } }}>{flow.share}</Button> : null}</>}>
          {preview.kind === "png" ? <img src={preview.url} alt="" className="preview-art" /> : <iframe title={flow.preview} src={preview.url} className="preview-art" />}
        </Modal>
      ) : null}
      {subForm ? (
        <Modal title={flow.makeSub} onClose={() => setSubForm(null)} closeLabel={copy.cancel}
          footer={<><Button onClick={() => setSubForm(null)}>{copy.cancel}</Button><Button variant="primary" onClick={async () => {
            try {
              const reason = subForm.reason === "gk-override" ? `override:${sizeReason || "goalkeeper"}` : subForm.reason;
              skip.current = true;
              await onRefresh(await send("post", `/api/lineups/${matchId}/substitutions`, { outId: Number(subForm.outId), inId: Number(subForm.inId), minute: Number(subForm.minute), reason }));
              setSubForm(null);
            } catch (err) { setError(err.message); }
          }}>{flow.makeSub}</Button></>}>
          <Field label={flow.out}>
            <SelectInput aria-label={flow.out} value={subForm.outId} onChange={(event) => setSubForm({ ...subForm, outId: event.target.value })}>
              <option value="">{flow.out}</option>
              {Object.values(draft.starters).map((id) => playerById(squad, id)).filter(Boolean).map((player) => <option key={player.id} value={player.id}>{player.knownAs}</option>)}
            </SelectInput>
          </Field>
          <Field label={flow.in}>
            <SelectInput aria-label={flow.in} value={subForm.inId} onChange={(event) => setSubForm({ ...subForm, inId: event.target.value })}>
              <option value="">{flow.in}</option>
              {benchPlayers.map((player) => <option key={player.id} value={player.id}>{player.knownAs}</option>)}
            </SelectInput>
          </Field>
          <Field label={flow.minutes}><TextInput type="number" min="1" max="120" value={subForm.minute} onChange={(event) => setSubForm({ ...subForm, minute: event.target.value })} /></Field>
          <Field label={flow.reason}>
            <SelectInput aria-label={flow.reason} value={subForm.reason} onChange={(event) => setSubForm({ ...subForm, reason: event.target.value })}>
              <option value="tactical">{flow.tactical}</option>
              <option value="injury">{flow.injury}</option>
              <option value="fatigue">{flow.fatigue}</option>
              <option value="red card">{flow.redCover}</option>
              <option value="gk-override">{flow.override}</option>
            </SelectInput>
          </Field>
        </Modal>
      ) : null}
      {success ? (
        <Modal title={flow.success} onClose={() => setSuccess(false)} closeLabel={copy.cancel} footer={<Button variant="primary" onClick={() => setSuccess(false)}>{copy.cancel}</Button>}>
          <motion.div className="success-mark" initial={reduced() ? false : { scale: 0.6, opacity: 0 }} animate={{ scale: 1, opacity: 1 }}><Check size={36} /></motion.div>
        </Modal>
      ) : null}
    </div>
  );
}

function validate(step, draft, plan, counts, squad, sheet, flow) {
  if (!draft) return [];
  const list = [];
  const sum = LINES.reduce((total, line) => total + (plan[line] || 0), 0);
  if (sum !== draft.squadSize) list.push(flow.reset);
  if ((draft.called || []).length > draft.squadSize) list.push(flow.removeHint(draft.called.length - draft.squadSize));
  if (step === 0) return list;
  if ((draft.called || []).length !== draft.squadSize) list.push(`${draft.called.length}/${draft.squadSize}`);
  LINES.forEach((line) => {
    if (counts[line] !== (plan[line] || 0)) list.push(`${flow.lines[line]} ${counts[line]}/${plan[line] || 0}`);
  });
  if ((counts.GK || 0) < 2) list.push(flow.needMore(flow.lines.GK));
  if (step < 2) return list;
  const ids = Object.values(draft.starters || {});
  if (ids.length !== 11) list.push(flow.needMore("XI"));
  const keeper = playerById(squad, draft.starters?.GK);
  if (!keeper || keeper.position !== "GOALKEEPER") list.push(flow.lines.GK);
  if (ids.some((id) => !(draft.called || []).includes(id))) list.push(flow.selected);
  if (new Set(ids).size !== ids.length) list.push("XI");
  return list;
}

function matchLabel(sheet) {
  return sheet?.match?.competition || sheet?.settings?.competitionRule?.competition || "";
}

function Donut({ counts, plan, label }) {
  const total = Math.max(1, LINES.reduce((sum, line) => sum + counts[line], 0));
  let cursor = 0;
  const colors = { GK: "#a3283a", DEF: "#1f6b3a", MID: "#2457a6", ATT: "#c4841d" };
  const stops = LINES.map((line) => {
    const start = cursor;
    cursor += (counts[line] / total) * 100;
    return `${colors[line]} ${start}% ${cursor}%`;
  }).join(", ");
  return <span className="donut" style={{ background: `conic-gradient(${stops})` }} title={label} aria-label={label}><i>{LINES.map((line) => `${line} ${counts[line]}/${plan[line] || 0}`).join(" ")}</i></span>;
}

function MiniForm({ spots }) {
  return (
    <svg className="mini-form" viewBox="0 0 100 120" aria-hidden="true">
      <rect x="8" y="6" width="84" height="108" rx="6" fill="none" stroke="currentColor" strokeWidth="4" />
      <line x1="8" y1="60" x2="92" y2="60" stroke="currentColor" strokeWidth="3" />
      {spots.map((spot) => <circle key={spot.code} cx={spot.x} cy={spot.y * 1.12} r="5" fill="currentColor" />)}
    </svg>
  );
}

function PitchFace() {
  return (
    <svg className="pitch-svg" viewBox="0 0 68 105" preserveAspectRatio="none" aria-hidden="true">
      <defs>
        <pattern id="grass" width="68" height="10" patternUnits="userSpaceOnUse">
          <rect width="68" height="5" fill="#2f9d57" />
          <rect y="5" width="68" height="5" fill="#27864a" />
        </pattern>
      </defs>
      <rect width="68" height="105" fill="url(#grass)" rx="4" />
      <rect x="3" y="3" width="62" height="99" fill="none" stroke="#fff" strokeWidth="0.6" />
      <line x1="3" y1="52" x2="65" y2="52" stroke="#fff" strokeWidth="0.4" />
      <circle cx="34" cy="52" r="8" fill="none" stroke="#fff" strokeWidth="0.4" />
      <rect x="14" y="78" width="40" height="24" fill="none" stroke="#fff" strokeWidth="0.4" />
      <rect x="14" y="3" width="40" height="20" fill="none" stroke="#fff" strokeWidth="0.4" />
      <rect x="24" y="100" width="20" height="3" fill="none" stroke="#fff" strokeWidth="0.6" />
    </svg>
  );
}
