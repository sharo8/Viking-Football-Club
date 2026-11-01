"use client";

import { motion } from "framer-motion";
import { ArrowDown, ArrowUp } from "lucide-react";
import { Avatar, Badge } from "@/ui/kit";
import { API_URL } from "@/services/api";

function reduced() {
  return typeof window !== "undefined" && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

function photo(url) {
  if (!url) return "";
  return url.startsWith("http") ? url : `${API_URL}${url}`;
}

export function liveXi(starters, events) {
  const map = { ...(starters || {}) };
  const marks = {};
  (events || [])
    .filter((event) => event.type === "SUB_IN")
    .slice()
    .sort((a, b) => a.minute - b.minute)
    .forEach((event) => {
      const slot = Object.keys(map).find((code) => map[code] === event.relatedPlayerId);
      if (!slot) return;
      marks[event.relatedPlayerId] = { way: "out", minute: event.minute };
      marks[event.playerId] = { way: "in", minute: event.minute, slot };
      map[slot] = event.playerId;
    });
  return { map, marks };
}

export function XiViews({
  view, skin, tilt, spots, squad, starters, bench, formation, events, editable, pickedSlot,
  captainId, captains, copy, flow, fits, onPlace, onPick, onProfile, onCaptain, onClear,
}) {
  const placed = liveXi(starters, events);
  const player = (id) => (squad || []).find((item) => item.id === id) || null;
  const rows = groupRows(spots, placed.map, player);
  const benchPlayers = (bench || []).map(player).filter((item) => item && !Object.values(placed.map).includes(item.id));
  const subs = pairSubs(events, player);

  return (
    <div className="xi-views">
      {view === "stadium" ? (
        <Stadium skin={skin} tilt={tilt} spots={spots} placed={placed} player={player} formation={formation} editable={editable} pickedSlot={pickedSlot} captainId={captainId} captains={captains} copy={copy} fits={fits} onPlace={onPlace} onPick={onPick} onProfile={onProfile} />
      ) : null}
      {view === "cards" ? <Cards rows={rows} benchPlayers={benchPlayers} placed={placed} captainId={captainId} captains={captains} copy={copy} flow={flow} editable={editable} onProfile={onProfile} onCaptain={onCaptain} onClear={onClear} /> : null}
      {view === "table" ? <Sheet rows={rows} benchPlayers={benchPlayers} placed={placed} captainId={captainId} copy={copy} flow={flow} editable={editable} onProfile={onProfile} onCaptain={onCaptain} onClear={onClear} /> : null}
      <Replacements subs={subs} copy={copy} flow={flow} />
    </div>
  );
}

function Stadium({ skin, tilt, spots, placed, player, formation, editable, pickedSlot, captainId, captains, copy, fits, onPlace, onPick, onProfile }) {
  return (
    <div className={`pitch-stage${tilt && skin === "photo" ? " is-tilt" : ""}${skin === "kit" ? " is-kit" : ""}`}>
      <div className="pitch-board" role="list">
        <PitchFace kit={skin === "kit"} />
        <span className="formation-pill">{formation}</span>
        {spots.map((spot) => {
          const person = player(placed.map[spot.code]);
          const mark = person ? placed.marks[person.id] : null;
          const warn = person && !fits(spot.code, person.position);
          return (
            <motion.button
              key={spot.code}
              type="button"
              role="listitem"
              className={`pitch-token${warn ? " warn" : ""}${pickedSlot === spot.code ? " is-picked" : ""}${skin === "kit" ? " is-shirt" : ""}`}
              style={{ left: `${spot.x}%`, top: `${spot.y}%` }}
              layout={!reduced()}
              aria-label={person ? `${person.knownAs} ${spot.code}` : spot.code}
              onDragOver={(event) => event.preventDefault()}
              onDrop={(event) => { event.preventDefault(); const id = Number(event.dataTransfer.getData("text/plain")); if (id) onPlace(spot.code, id); }}
              onClick={() => (person ? onProfile(person) : onPick(spot.code))}
              draggable={Boolean(person) && editable}
              onDragStart={(event) => { if (person) event.dataTransfer.setData("text/plain", String(person.id)); }}
            >
              {skin === "kit" ? <Shirt number={person?.jersey} keeper={spot.code === "GK"} /> : person ? <Avatar name={person.knownAs} src={photo(person.photoUrl)} /> : <i className="slot-empty">{spot.code}</i>}
              {person && skin !== "kit" ? <em className="jersey">#{person.jersey}</em> : null}
              {person && (captainId === person.id || captains[person.id]) ? <em className="arm">{captainId === person.id ? "C" : copy.ranks[captains[person.id]]}</em> : null}
              {mark ? <em className={`sub-arrow ${mark.way}`}>{mark.way === "out" ? <ArrowDown size={12} /> : <ArrowUp size={12} />}{mark.minute}'</em> : null}
              <small>{person ? String(person.knownAs || "").slice(0, 12) : ""}</small>
            </motion.button>
          );
        })}
      </div>
    </div>
  );
}

function Cards({ rows, benchPlayers, placed, captainId, captains, copy, flow, editable, onProfile, onCaptain, onClear }) {
  return (
    <div className="xi-cards">
      {rows.map((row) => (
        <section key={row.line}>
          <h3>{flow.lines[row.line] || row.line}</h3>
          <div className="card-grid">
            {row.players.map((person) => (
              <article key={person.id} className="xi-card">
                <Avatar name={person.knownAs} src={photo(person.photoUrl)} />
                <div>
                  <b>#{person.jersey} {person.knownAs}</b>
                  <p>{person.fullName}</p>
                  <small>{person.slot} · {flow.positions[person.position] || person.positionLabel} · {copy.age} {person.age ?? "—"} · {copy.form} {person.form ?? "—"}</small>
                  {captainId === person.id || captains[person.id] ? <Badge>{captainId === person.id ? "C" : copy.ranks[captains[person.id]]}</Badge> : null}
                  {placed.marks[person.id] ? <Badge tone={placed.marks[person.id].way === "in" ? "good" : "bad"}>{placed.marks[person.id].minute}'</Badge> : null}
                </div>
                <div className="card-actions">
                  <button type="button" disabled={!editable} onClick={() => onProfile(person)}>{flow.replace}</button>
                  <button type="button" disabled={!editable} onClick={() => onCaptain(person.id)}>{flow.captain}</button>
                  <button type="button" disabled={!editable} onClick={() => onClear(person.slot)}>{copy.remove}</button>
                </div>
              </article>
            ))}
          </div>
        </section>
      ))}
      <section>
        <h3>{copy.bench}</h3>
        <div className="card-grid bench-cards">
          {benchPlayers.map((person) => (
            <article key={person.id} className="xi-card is-bench">
              <Avatar name={person.knownAs} src={photo(person.photoUrl)} />
              <div><b>#{person.jersey} {person.knownAs}</b><small>{person.positionLabel}</small></div>
            </article>
          ))}
        </div>
      </section>
    </div>
  );
}

function Sheet({ rows, benchPlayers, placed, captainId, copy, flow, editable, onProfile, onCaptain, onClear }) {
  const people = [
    ...rows.flatMap((row) => row.players.map((person) => ({ ...person, role: flow.starter }))),
    ...benchPlayers.map((person) => ({ ...person, role: flow.substitute, slot: "—" })),
  ];
  return (
    <div className="admin-table-wrap">
      <table className="rating-table xi-table">
        <thead>
          <tr>
            <th>#</th><th></th><th>{copy.player}</th><th>{flow.slot}</th><th>{flow.natural}</th><th>{flow.role}</th><th>{copy.age}</th><th>{copy.form}</th><th>{copy.availabilityLabel || copy.availableOnly}</th><th></th>
          </tr>
        </thead>
        <tbody>
          {people.map((person) => (
            <tr key={`${person.role}-${person.id}`}>
              <td>{person.jersey}</td>
              <td><Avatar name={person.knownAs} src={photo(person.photoUrl)} /></td>
              <td>{person.knownAs}{captainId === person.id ? " (C)" : ""}{placed.marks[person.id] ? ` ${placed.marks[person.id].minute}'` : ""}</td>
              <td>{person.slot}</td>
              <td>{flow.positions[person.position] || person.positionLabel}</td>
              <td>{person.role}</td>
              <td>{person.age ?? "—"}</td>
              <td>{person.form ?? "—"}</td>
              <td>{copy.availability[person.availability] || person.availability}</td>
              <td className="row-actions">
                <button type="button" disabled={!editable} aria-label={flow.replace} title={flow.replace} onClick={() => onProfile(person)}>↔</button>
                <button type="button" disabled={!editable} aria-label={flow.captain} title={flow.captain} onClick={() => onCaptain(person.id)}>C</button>
                <button type="button" disabled={!editable || person.role !== flow.starter} aria-label={copy.remove} title={copy.remove} onClick={() => onClear(person.slot)}>×</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function Replacements({ subs, copy, flow }) {
  if (!subs.length) return null;
  return (
    <section className="replacements">
      <h3>{flow.replacements}</h3>
      <ul>
        {subs.map((sub) => (
          <li key={`${sub.minute}-${sub.in}`}>
            <ArrowDown size={14} /> {sub.out} <ArrowUp size={14} /> {sub.in}
            <span>{sub.minute}' · {sub.reason || flow.tactical}</span>
          </li>
        ))}
      </ul>
    </section>
  );
}

function Shirt({ number, keeper }) {
  return (
    <span className={`kit-shirt${keeper ? " is-gk" : ""}`} aria-hidden="true">
      <b>{number ?? ""}</b>
    </span>
  );
}

function PitchFace({ kit }) {
  return (
    <svg className="pitch-svg" viewBox="0 0 68 105" preserveAspectRatio="none" aria-hidden="true">
      <defs>
        <pattern id="grass" width="68" height="10" patternUnits="userSpaceOnUse">
          <rect width="68" height="5" fill={kit ? "#0e3b2e" : "#2f9d57"} />
          <rect y="5" width="68" height="5" fill={kit ? "#0b2e24" : "#27864a"} />
        </pattern>
      </defs>
      <rect width="68" height="105" fill="url(#grass)" />
      <rect x="3" y="3" width="62" height="99" fill="none" stroke="#fff" strokeWidth="0.6" />
      <line x1="3" y1="52" x2="65" y2="52" stroke="#fff" strokeWidth="0.4" />
      <circle cx="34" cy="52" r="8" fill="none" stroke="#fff" strokeWidth="0.4" />
      <rect x="14" y="3" width="40" height="18" fill="none" stroke="#fff" strokeWidth="0.4" />
      <rect x="14" y="84" width="40" height="18" fill="none" stroke="#fff" strokeWidth="0.4" />
      <rect x="22" y="3" width="24" height="8" fill="none" stroke="#fff" strokeWidth="0.4" />
      <rect x="22" y="94" width="24" height="8" fill="none" stroke="#fff" strokeWidth="0.4" />
      <circle cx="34" cy="16" r="0.7" fill="#fff" />
      <circle cx="34" cy="89" r="0.7" fill="#fff" />
    </svg>
  );
}

function groupRows(spots, starters, player) {
  const order = ["GK", "DEF", "MID", "ATT"];
  const buckets = { GK: [], DEF: [], MID: [], ATT: [] };
  (spots || []).forEach((spot) => {
    const person = player(starters[spot.code]);
    if (!person) return;
    const line = spot.line || "MID";
    buckets[line] = buckets[line] || [];
    buckets[line].push({ ...person, slot: spot.code });
  });
  return order.filter((line) => buckets[line]?.length).map((line) => ({ line, players: buckets[line] }));
}

function pairSubs(events, player) {
  return (events || [])
    .filter((event) => event.type === "SUB_IN")
    .map((event) => ({
      minute: event.minute,
      in: player(event.playerId)?.knownAs || event.playerName,
      out: player(event.relatedPlayerId)?.knownAs || event.relatedPlayerName,
      reason: event.note,
    }));
}
