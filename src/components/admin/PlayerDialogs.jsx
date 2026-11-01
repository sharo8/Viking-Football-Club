"use client";

import { useEffect, useState } from "react";
import { api, getData } from "@/services/api";
import { Avatar, Button, Field, Modal, SelectInput, TextArea, TextInput } from "@/ui/kit";
import { POSITION, RANKS, label, money } from "./playersCopy";

const TEMPLATES = [
  ["notice", "Official notice", "A note from the club office."],
  ["training", "Training convocation", "You are called to training."],
  ["match", "Match convocation", "You are called for the match."],
  ["discipline", "Disciplinary", "A disciplinary notice."],
  ["contract", "Contract", "A note about your contract."],
  ["cheers", "Congratulations", "Well played."],
  ["free", "Free message", ""],
];

export function ProfileModal({ row, copy, locale, executive, onClose, onEdit, onMail, onCaptain, onArchive }) {
  const [tab, setTab] = useState("overview");
  const [stats, setStats] = useState(null);
  const [messages, setMessages] = useState([]);
  useEffect(() => {
    getData(`/api/players/${row.id}/stats`).then(setStats).catch(() => setStats(null));
    getData(`/api/players/${row.id}/messages`).then(setMessages).catch(() => setMessages([]));
  }, [row.id]);
  const missing = copy.notProvided;
  const tabs = [["overview", copy.overview], ["emergency", copy.emergencyTab], ["stats", copy.stats], ["medical", copy.medical], ["messages", copy.messages]];
  if (executive) tabs.splice(2, 0, ["contract", copy.contractTab]);

  return (
    <Modal size="md" title={row.fullName} subtitle={row.knownAs || ""} onClose={onClose} closeLabel={copy.close}
      footer={(
        <div className="vfc-foot-right">
          <Button onClick={() => onEdit(row)}>{copy.edit}</Button>
          <Button disabled={!row.email} onClick={() => onMail(row)}>{copy.message}</Button>
          <Button onClick={() => onCaptain(row)}>{copy.captain}</Button>
          <Button variant="danger" onClick={() => onArchive(row)}>{copy.archive}</Button>
        </div>
      )}>
      <div className="vfc-cover">
        <span className="vfc-watermark">{row.jersey ?? ""}</span>
        <Avatar name={row.fullName} src={row.photoUrl} size="96" />
        <h2 style={{ margin: "8px 0 0" }}>{row.fullName}</h2>
        <p>#{row.jersey} · {label(POSITION, row.position, locale)} · {row.status || ""} {row.captain || ""}</p>
      </div>
      <div className="vfc-tabs">
        {tabs.map(([id, text]) => <button key={id} type="button" className={tab === id ? "is-on" : ""} onClick={() => setTab(id)}>{text}</button>)}
      </div>
      {tab === "overview" ? (
        <div className="vfc-tiles">
          <Tile label={copy.age} value={row.age == null ? missing : `${row.age}${row.dateOfBirth ? ` · ${row.dateOfBirth}` : ""}`} />
          <Tile label={copy.nationality} value={row.nationality || missing} />
          <Tile label={copy.foot} value={row.foot || missing} />
          <Tile label={copy.height} value={row.heightCm || missing} />
          <Tile label={copy.phone} value={row.phone || missing} />
          <Tile label={copy.email} value={row.email || missing} />
        </div>
      ) : null}
      {tab === "emergency" ? <Tile label={copy.emergency} value={row.emergencyName ? `${row.emergencyName} · ${row.emergencyPhone || ""}` : missing} /> : null}
      {tab === "contract" && executive ? (
        <div className="vfc-tiles">
          <Tile label={copy.contract} value={row.contractEnd || missing} />
          <Tile label={copy.pay} value={money(row.seasonalPay)} />
        </div>
      ) : null}
      {tab === "stats" ? (
        <div className="vfc-tiles">
          <Tile label="Matches" value={stats?.matchesPlayed ?? "–"} />
          <Tile label="Goals" value={stats?.goals ?? "–"} />
          <Tile label="Assists" value={stats?.assists ?? "–"} />
          <Tile label="Minutes" value={stats?.minutes ?? "–"} />
        </div>
      ) : null}
      {tab === "medical" ? <Tile label={copy.status} value={row.status || missing} /> : null}
      {tab === "messages" ? (
        <ul className="vfc-depth">
          {messages.length === 0 ? <li className="vfc-muted">{missing}</li> : messages.map((item) => (
            <li key={item.id}><span>{item.subject}</span><b>{item.status}</b>{item.error ? <small>{item.error}</small> : null}</li>
          ))}
        </ul>
      ) : null}
    </Modal>
  );
}

function Tile({ label, value }) {
  return <article className="vfc-tile"><small>{label}</small><strong>{value}</strong></article>;
}

export function MessageModal({ player, copy, locale, userEmail, onClose, onSent }) {
  const [template, setTemplate] = useState("notice");
  const [subject, setSubject] = useState("Official notice");
  const [body, setBody] = useState("A note from the club office.");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [sent, setSent] = useState(false);
  const first = (player.knownAs || player.fullName || "").split(" ")[0];
  const preview = body.replaceAll("{{firstName}}", first).replaceAll("{{jersey}}", String(player.jersey ?? ""));

  async function send(test) {
    setBusy(true);
    setError("");
    try {
      const response = test
        ? await api.post("/api/email/outbox/test", { to: userEmail, subject, body: preview })
        : await api.post(`/api/players/${player.id}/messages`, { subject, body: preview, language: locale });
      const note = response.data?.data?.note;
      if (note && note !== "Queued") setError(note);
      setSent(true);
      if (!test) window.setTimeout(onSent, 900);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal size="lg" title={copy.message} subtitle={player.email || copy.noEmail} onClose={onClose} closeLabel={copy.close}
      footer={sent ? null : (
        <>
          <Button variant="ghost" onClick={onClose}>{copy.cancel}</Button>
          <div className="vfc-foot-right">
            <Button disabled={!player.email} onClick={() => send(true)}>{copy.sendTest}</Button>
            <Button variant="primary" loading={busy} disabled={!player.email} onClick={() => send(false)}>{copy.send}</Button>
          </div>
        </>
      )}>
      {!player.email ? <p>{copy.noEmail}. <button type="button" className="add-link" onClick={onClose}>{copy.addEmail}</button></p> : null}
      {sent ? <div className="vfc-check" role="status">{copy.sent}</div> : (
        <div className="vfc-mail">
          <div className="vfc-grid">
            <div className="span-2 vfc-templates">
              {TEMPLATES.map(([id, title, text]) => (
                <button key={id} type="button" className={`vfc-chip${template === id ? " is-on" : ""}`} onClick={() => { setTemplate(id); setSubject(title); setBody(text); }}>{title}</button>
              ))}
            </div>
            <Field label={copy.subject}><TextInput value={subject} onChange={(event) => setSubject(event.target.value)} /></Field>
            <Field label={copy.language}><TextInput value={locale.toUpperCase()} readOnly /></Field>
            <div className="span-2"><Field label={copy.message}><TextArea value={body} onChange={(event) => setBody(event.target.value)} /></Field></div>
            {error ? <p className="span-2 vfc-error">{error}</p> : null}
          </div>
          <aside className="vfc-phone" aria-label={copy.preview}>
            <header>VIKING FC</header>
            <div>
              <strong>{subject}</strong>
              <p>{preview}</p>
              <small>{player.fullName} · #{player.jersey}</small>
            </div>
          </aside>
        </div>
      )}
    </Modal>
  );
}

export function ConfirmModal({ row, copy, onClose, onArchive }) {
  const [error, setError] = useState("");
  const [exitReason, setExitReason] = useState("RELEASED");
  const [exitDate, setExitDate] = useState(new Date().toISOString().slice(0, 10));
  const [note, setNote] = useState("");
  async function run() {
    try {
      await onArchive(row, { exitReason, exitDate, note });
    } catch (err) {
      setError(err?.code === "LINEUP_PUBLISHED" ? copy.lineupBlocked : (err.message || copy.retry));
    }
  }
  return (
    <Modal size="sm" title={`${copy.moveFormer} — ${row.fullName}`} onClose={onClose} closeLabel={copy.close}
      footer={(
        <div className="vfc-foot-right pair-actions">
          <Button onClick={onClose}>{copy.cancel}</Button>
          <Button variant="danger" disabled={!exitReason} onClick={run}>{copy.moveFormer}</Button>
        </div>
      )}>
      <Field label={copy.exitReason} required>
        <SelectInput value={exitReason} onChange={(event) => setExitReason(event.target.value)}>
          <option value="TRANSFERRED">{copy.exitTransferred}</option>
          <option value="RELEASED">{copy.exitReleased}</option>
          <option value="RETIRED">{copy.exitRetired}</option>
          <option value="CONTRACT_ENDED">{copy.exitContract}</option>
          <option value="LOAN_ENDED">{copy.exitLoan}</option>
          <option value="OTHER">{copy.exitOther}</option>
        </SelectInput>
      </Field>
      <Field label={copy.exitDate}><TextInput type="date" value={exitDate} onChange={(event) => setExitDate(event.target.value)} /></Field>
      <Field label={copy.exitNote}><TextArea value={note} onChange={(event) => setNote(event.target.value)} /></Field>
      {error ? <p className="vfc-error">{error}</p> : null}
    </Modal>
  );
}

export function CaptainModal({ picker, captains, squad, copy, onClose, onAssign, onRemove }) {
  const choosingPlayer = typeof picker === "string";
  const player = choosingPlayer ? null : squad.find((row) => row.id === picker.playerId);
  const [query, setQuery] = useState("");
  const [rank, setRank] = useState(choosingPlayer ? picker : (player?.captain || "CAPTAIN"));
  const holder = captains.find((item) => item.rank === rank);
  const filtered = squad.filter((row) => `${row.fullName} ${row.jersey}`.toLowerCase().includes(query.toLowerCase()));

  return (
    <Modal size="md" title={copy.captain} subtitle={player ? `${player.fullName} · #${player.jersey}` : copy.assign} onClose={onClose} closeLabel={copy.close}
      footer={(
        <>
          <Button variant="ghost" onClick={onClose}>{copy.cancel}</Button>
          <div className="vfc-foot-right">
            {!choosingPlayer && player?.captain ? <Button variant="danger" onClick={() => onRemove(player.captain)}>{copy.removeRank}</Button> : null}
            <Button variant="primary" disabled={choosingPlayer || !player} onClick={() => onAssign(rank, player.id)}>
              {copy.assignAs} {RANKS.find(([key]) => key === rank)?.[1] || rank}
            </Button>
          </div>
        </>
      )}>
      {player ? (
        <div className="name-cell" style={{ marginBottom: 12 }}>
          <Avatar name={player.fullName} src={player.photoUrl} />
          <span className="name-block"><strong>{player.fullName}</strong><small>#{player.jersey} · {player.position}</small></span>
        </div>
      ) : (
        <Field label={copy.search}><TextInput value={query} onChange={(event) => setQuery(event.target.value)} /></Field>
      )}
      <div className="vfc-slots">
        {RANKS.map(([key, badge]) => {
          const current = captains.find((item) => item.rank === key);
          return (
            <button key={key} type="button" className={`vfc-slot${rank === key ? " is-on" : ""}`} onClick={() => setRank(key)}>
              <Avatar name={current?.fullName || badge} src={current?.photoUrl} />
              <span><b>{badge}</b><br />{current ? current.fullName : copy.free}</span>
            </button>
          );
        })}
      </div>
      {holder && holder.playerId !== player?.id ? <p className="vfc-error">{copy.replaceWarn} {holder.fullName}</p> : null}
      {choosingPlayer ? (
        <div className="vfc-steps" style={{ marginTop: 12 }}>
          {filtered.slice(0, 8).map((row) => (
            <button key={row.id} type="button" onClick={() => onAssign(rank, row.id)}><Avatar name={row.fullName} src={row.photoUrl} /> #{row.jersey} {row.fullName}</button>
          ))}
        </div>
      ) : null}
    </Modal>
  );
}

export function ColumnsMenu({ copy, extra, executive, onToggle }) {
  const [open, setOpen] = useState(false);
  const items = [["nationality", copy.nationality], ["phone", copy.phone], ["email", copy.email], ["foot", copy.foot], ["pay", copy.pay]];
  return (
    <div className="vfc-pop">
      <Button onClick={() => setOpen((value) => !value)}>{copy.columns}</Button>
      {open ? (
        <div className="vfc-popover" style={{ minWidth: 180 }}>
          {items.filter(([key]) => key !== "pay" || executive).map(([key, text]) => (
            <label key={key} className="vfc-field" style={{ padding: 6 }}>
              <span><input type="checkbox" checked={Boolean(extra[key])} onChange={() => onToggle(key)} /> {text}</span>
            </label>
          ))}
        </div>
      ) : null}
    </div>
  );
}
