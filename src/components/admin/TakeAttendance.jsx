"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { api } from "@/services/api";
import { Button, Field, Modal, SelectInput, Stepper, TextArea, TextInput } from "@/ui/kit";
import { rwf } from "./attendanceCopy";

async function send(method, path, body) {
  const response = await api[method](path, body);
  if (!response.data?.success) throw new Error(response.data?.message || "Request failed");
  return response.data.data;
}

function arrivalFrom(start, minutes) {
  const [hour, minute] = String(start || "16:00").slice(0, 5).split(":").map(Number);
  const total = (hour * 60) + minute + Number(minutes || 0);
  return `${String(Math.floor(total / 60) % 24).padStart(2, "0")}:${String(total % 60).padStart(2, "0")}`;
}

function fineFor(mark, kind, thresholds) {
  if (!mark?.status || !thresholds || ["E", "I", "N"].includes(mark.status)) return null;
  const late = Number(mark.minutesLate || 0);
  const limit = kind === "MATCH" ? Number(thresholds.matchLateMinutes || thresholds.lateMinutes) : Number(thresholds.lateMinutes);
  if (kind === "MATCH") {
    if (mark.status === "A") return { rule: 8, amount: thresholds.matchAbsenceFine };
    if (mark.status === "L" && late > limit) return { rule: 8, amount: thresholds.matchLateFine };
    return null;
  }
  if (mark.status === "A") return { rule: 7, amount: thresholds.absenceFine };
  if (mark.status === "L" && late > Number(thresholds.lateMinutes)) return { rule: 6, amount: thresholds.lateFine };
  return null;
}

export function TakeAttendance({ t, audience, sessionId, onClose, onDone }) {
  const [step, setStep] = useState(sessionId ? 1 : 0);
  const [id, setId] = useState(sessionId || null);
  const [form, setForm] = useState({ kind: "TRAINING", date: new Date().toISOString().slice(0, 10), startTime: "16:00", place: "", matchId: "", notes: "" });
  const [matches, setMatches] = useState([]);
  const [detail, setDetail] = useState(null);
  const [marks, setMarks] = useState({});
  const [query, setQuery] = useState("");
  const [position, setPosition] = useState("");
  const [focus, setFocus] = useState(0);
  const [reason, setReason] = useState("");
  const [saved, setSaved] = useState("");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const [dirty, setDirty] = useState(false);
  const marksRef = useRef(marks);
  const idRef = useRef(id);
  const savingRef = useRef(false);
  marksRef.current = marks;
  idRef.current = id;

  useEffect(() => {
    api.get("/api/attendance/matches").then((response) => setMatches(response.data?.data || [])).catch(() => setMatches([]));
  }, []);

  useEffect(() => {
    if (!sessionId) return;
    send("get", `/api/attendance/sessions/${sessionId}`).then((data) => applyDetail(data)).catch((err) => setError(err.message));
  }, [sessionId]);

  useEffect(() => {
    const onLeave = (event) => {
      if (!dirty) return;
      event.preventDefault();
      event.returnValue = "";
    };
    window.addEventListener("beforeunload", onLeave);
    return () => window.removeEventListener("beforeunload", onLeave);
  }, [dirty]);

  useEffect(() => {
    const timer = window.setInterval(() => {
      if (idRef.current && dirty) save(reason, true).catch(() => {});
    }, 8000);
    return () => window.clearInterval(timer);
  }, [dirty, reason]);

  function applyDetail(data) {
    setDetail(data);
    setId(data.id);
    setForm({
      kind: data.kind || "TRAINING",
      date: String(data.date).slice(0, 10),
      startTime: String(data.startTime || "16:00").slice(0, 5),
      place: data.place || "",
      matchId: data.matchId || "",
      notes: data.notes || "",
    });
    const next = {};
    (data.roster || []).forEach((row) => {
      next[row.personId] = {
        status: row.injured ? "I" : (row.status || ""),
        minutesLate: row.minutesLate || 0,
        note: row.note || "",
        arrival: row.arrivalTime ? String(row.arrivalTime).slice(0, 5) : "",
        injured: row.injured,
      };
    });
    setMarks(next);
  }

  function patch(personId, change) {
    setMarks((current) => ({ ...current, [personId]: { ...current[personId], ...change } }));
    setDirty(true);
    setSaved("");
  }

  function setStatus(person, status) {
    if (person.injured) return;
    if (status === "E" && !detail?.access?.approve && marks[person.personId]?.status !== "E") return;
    const minutes = status === "L" ? Math.max(Number(marks[person.personId]?.minutesLate || 0), Number(detail?.thresholds?.lateMinutes || 15) + 1) : 0;
    patch(person.personId, {
      status,
      minutesLate: status === "L" ? minutes : 0,
      arrival: status === "L" ? arrivalFrom(form.startTime, minutes) : "",
    });
  }

  const roster = detail?.roster || [];
  const visible = useMemo(() => roster.filter((row) => {
    const blob = `${row.name} ${row.role || ""}`.toLowerCase();
    if (query && !blob.includes(query.toLowerCase())) return false;
    if (position && row.position !== position && row.line !== position) return false;
    return true;
  }), [roster, query, position]);

  const counts = roster.reduce((bag, row) => {
    const status = marks[row.personId]?.status || "";
    bag[status || "U"] = (bag[status || "U"] || 0) + 1;
    return bag;
  }, {});

  async function openSession() {
    setSaving(true);
    setError("");
    try {
      const body = {
        kind: form.kind,
        audience: audience === "staff" ? "STAFF" : "PLAYERS",
        date: form.date,
        startTime: form.startTime || "16:00",
        place: form.place,
        matchId: form.matchId ? Number(form.matchId) : null,
        notes: form.notes,
      };
      const data = id
        ? await send("put", `/api/attendance/sessions/${id}`, body)
        : await send("post", "/api/attendance/sessions", body);
      applyDetail(data);
      setStep(1);
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  async function save(excuse, silent) {
    if (!idRef.current || savingRef.current) return null;
    savingRef.current = true;
    const body = {
      reason: excuse || null,
      marks: Object.entries(marksRef.current).filter(([, mark]) => mark.status).map(([personId, mark]) => ({
        personId: Number(personId),
        status: mark.status,
        minutesLate: Number(mark.minutesLate || 0),
        arrivalTime: mark.arrival || null,
        note: mark.note || null,
      })),
    };
    try {
      const data = await send("put", `/api/attendance/sessions/${idRef.current}/records`, body);
      applyDetail(data);
      setDirty(false);
      setSaved(t.saved);
      return data;
    } catch (err) {
      const message = String(err.message || "");
      if (!silent) setError(message.toLowerCase().includes("excuse") ? t.excuseGap : message);
      throw err;
    } finally {
      savingRef.current = false;
    }
  }

  async function closeSession() {
    setSaving(true);
    setError("");
    try {
      if (detail?.status === "CLOSED" && !reason.trim()) {
        setError(t.reason);
        return;
      }
      await save(reason);
      await send("post", `/api/attendance/sessions/${id}/close`);
      onDone();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  function onKey(event) {
    if (!visible.length || step !== 1) return;
    const key = event.key.toLowerCase();
    if (["p", "l", "a", "e"].includes(key)) {
      event.preventDefault();
      setStatus(visible[Math.min(focus, visible.length - 1)], key.toUpperCase());
    } else if (event.key === "ArrowDown" || event.key === "ArrowUp") {
      event.preventDefault();
      setFocus((index) => Math.max(0, Math.min(visible.length - 1, index + (event.key === "ArrowDown" ? 1 : -1))));
    }
  }

  const fines = roster.map((row) => ({ row, fine: fineFor(marks[row.personId], form.kind, detail?.thresholds) })).filter((item) => item.fine);
  const fineTotal = fines.reduce((sum, item) => sum + Number(item.fine.amount || 0), 0);
  const excuses = roster.filter((row) => marks[row.personId]?.status === "E");
  const excuseGaps = excuses.filter((row) => !(marks[row.personId]?.note || "").trim());
  const sheet = [
    ["P", t.present, counts.P || 0],
    ["L", t.late, counts.L || 0],
    ["A", t.absent, counts.A || 0],
    ["E", t.excused, counts.E || 0],
    ["U", t.unmarked, counts.U || 0],
  ];

  return (
    <Modal
      title={t.take}
      subtitle={audience === "staff" ? t.staff : t.players}
      size="lg"
      onClose={onClose}
      closeLabel={t.close}
      footer={(
        <>
          <span className="att-saved">{saved}</span>
          <div className="vfc-foot-right">
            {step > 0 ? <Button onClick={() => setStep((value) => value - 1)}>{t.back}</Button> : null}
            {step === 0 ? <Button variant="primary" loading={saving} onClick={openSession}>{t.next}</Button> : null}
            {step === 1 ? <Button onClick={() => save(reason).catch(() => {})}>{t.saveDraft}</Button> : null}
            {step === 1 ? <Button variant="primary" onClick={() => { if (excuseGaps.length) { setError(""); setStep(2); return; } save(reason).then(() => setStep(2)).catch(() => {}); }}>{t.review}</Button> : null}
            {step === 2 && excuseGaps.length ? <Button variant="primary" disabled title={t.excuseGap}>{t.closeSession}</Button> : null}
            {step === 2 && !excuseGaps.length ? <Button variant="primary" loading={saving} onClick={closeSession}>{t.closeSession}</Button> : null}
          </div>
        </>
      )}
    >
      <div className={error ? "att-shake" : ""} onKeyDown={onKey}>
        <Stepper steps={[t.stepSession, t.stepMarks, t.stepReview]} current={step} onSelect={(index) => { if (index === 0 || id) setStep(index); }} label={t.take} />
        {error ? <p className="crud-error">{error}</p> : null}
        {step === 0 ? (
          <div className="vfc-grid">
            <Field label={t.kind}>
              <div className="vfc-seg">
                <button type="button" className={form.kind === "TRAINING" ? "is-on" : ""} onClick={() => setForm({ ...form, kind: "TRAINING" })}>{t.training}</button>
                <button type="button" className={form.kind === "MATCH" ? "is-on" : ""} onClick={() => setForm({ ...form, kind: "MATCH" })}>{t.match}</button>
              </div>
            </Field>
            <Field label={t.start}><TextInput type="time" value={form.startTime} onChange={(event) => setForm({ ...form, startTime: event.target.value })} /></Field>
            <Field label={t.day} required><TextInput type="date" value={form.date} onChange={(event) => setForm({ ...form, date: event.target.value })} /></Field>
            <Field label={t.place}><TextInput value={form.place} onChange={(event) => setForm({ ...form, place: event.target.value })} /></Field>
            {form.kind === "MATCH" ? (
              <Field label={t.linkMatch}>
                <SelectInput value={form.matchId} onChange={(event) => setForm({ ...form, matchId: event.target.value })}>
                  <option value="">{t.none}</option>
                  {matches.map((match) => <option key={match.id} value={match.id}>{match.label}</option>)}
                </SelectInput>
              </Field>
            ) : null}
            <Field label={t.notes}><TextArea value={form.notes} onChange={(event) => setForm({ ...form, notes: event.target.value })} /></Field>
          </div>
        ) : null}
        {step === 1 ? (
          <div className="att-marking">
            <div className="att-counters">
              <span>{t.present} {counts.P || 0}</span>
              <span>{t.late} {counts.L || 0}</span>
              <span>{t.absent} {counts.A || 0}</span>
              <span>{t.excused} {counts.E || 0}</span>
              <span>{t.unmarked} {counts.U || 0}</span>
            </div>
            <div className="att-filters">
              <TextInput aria-label={t.searchList} placeholder={t.searchList} value={query} onChange={(event) => setQuery(event.target.value)} />
              <SelectInput aria-label={t.position} value={position} onChange={(event) => setPosition(event.target.value)}>
                <option value="">{t.any}</option>
                {(detail?.roster || []).map((row) => row.position).filter((value, index, list) => value && list.indexOf(value) === index).map((value) => <option key={value}>{value}</option>)}
              </SelectInput>
              <Button onClick={() => roster.forEach((row) => { if (!row.injured) patch(row.personId, { status: "P", minutesLate: 0, arrival: "" }); })}>{t.markAll}</Button>
              <Button onClick={() => roster.forEach((row) => { if (!row.injured && !marks[row.personId]?.status) patch(row.personId, { status: "A", minutesLate: 0 }); })}>{t.markRest}</Button>
            </div>
            <div className="att-people">
              {visible.map((row, index) => {
                const mark = marks[row.personId] || {};
                const fine = fineFor(mark, form.kind, detail?.thresholds);
                return (
                  <article key={row.personId} className={index === focus ? "att-person is-on" : "att-person"} onClick={() => setFocus(index)}>
                    <div>
                      <strong>{row.name}</strong>
                      <small>{row.jersey ? `#${row.jersey} · ` : ""}{row.role}</small>
                    </div>
                    <div className="att-marks">
                      {["P", "L", "A", "E"].map((code) => (
                        <button key={code} type="button" className={`att-mark ${code}${mark.status === code ? " on" : ""}`} disabled={row.injured || (code === "E" && !detail?.access?.approve)} title={code === "E" && !detail?.access?.approve ? t.approveHint : t[code === "P" ? "present" : code === "L" ? "late" : code === "A" ? "absent" : "excused"]} onClick={() => setStatus(row, code)}>{code}</button>
                      ))}
                      {form.kind === "MATCH" ? <button type="button" className={`att-mark N${mark.status === "N" ? " on" : ""}`} disabled={row.injured} onClick={() => setStatus(row, "N")}>N</button> : null}
                      {row.injured || mark.status === "I" ? <span className="mark-chip I">I</span> : null}
                    </div>
                    {mark.status === "L" ? (
                      <div className="att-late">
                        {[5, 10, 15, 20].map((minutes) => (
                          <button key={minutes} type="button" className="vfc-chip" onClick={() => patch(row.personId, { minutesLate: minutes, arrival: arrivalFrom(form.startTime, minutes) })}>+{minutes}</button>
                        ))}
                        <TextInput aria-label={t.minutes} type="number" min="0" value={mark.minutesLate || 0} onChange={(event) => patch(row.personId, { minutesLate: Number(event.target.value), arrival: arrivalFrom(form.startTime, event.target.value) })} />
                      </div>
                    ) : null}
                    {mark.status === "E" || mark.status === "A" || mark.status === "L" ? (
                      <TextInput aria-label={t.note} placeholder={t.note} value={mark.note || ""} onChange={(event) => patch(row.personId, { note: event.target.value })} />
                    ) : null}
                    {fine ? <span className="fine-chip OUTSTANDING">{t.rule} {fine.rule} · {rwf(fine.amount)}</span> : null}
                  </article>
                );
              })}
            </div>
          </div>
        ) : null}
        {step === 2 ? (
          <div className="att-review">
            <header className="att-review-head">
              <div>
                <p className="att-review-kicker">{form.kind === "MATCH" ? t.match : t.training} · {form.date} · {form.startTime}</p>
                <h3>{t.reviewLead}</h3>
                {form.place ? <p>{form.place}</p> : null}
              </div>
              <Button onClick={() => setStep(1)}>{t.backToMarks}</Button>
            </header>
            <div className="att-review-sheet">
              {sheet.map(([code, label, count]) => (
                <article key={code} className={`att-review-stat ${code}`}>
                  <strong>{count}</strong>
                  <span>{label}</span>
                </article>
              ))}
            </div>
            {counts.U ? (
              <div className="att-callout warn">
                <strong>{counts.U} {t.unmarked}</strong>
                <p>{t.warningUnmarked}</p>
              </div>
            ) : null}
            {excuseGaps.length ? (
              <div className="att-callout bad">
                <strong>{t.excuseGap}</strong>
                <div className="att-excuses">
                  {excuseGaps.map((row) => (
                    <label key={row.personId}>
                      <span>{row.name}</span>
                      <TextInput aria-label={t.reason} placeholder={t.reason} value={marks[row.personId]?.note || ""} onChange={(event) => patch(row.personId, { note: event.target.value })} />
                    </label>
                  ))}
                </div>
              </div>
            ) : null}
            <section className="att-ledger">
              <header>
                <h3>{t.finesPreview}</h3>
                <span>{fines.length}</span>
              </header>
              {!fines.length ? <p className="att-ledger-empty">{counts.U ? t.noFines : t.clearSheet}</p> : fines.map(({ row, fine }) => (
                <div key={row.personId} className="att-ledger-row">
                  <div>
                    <strong>{row.name}</strong>
                    <small>{row.jersey ? `#${row.jersey} · ` : ""}{row.role}</small>
                  </div>
                  <span className={`mark-chip ${marks[row.personId]?.status || ""}`}>{marks[row.personId]?.status}</span>
                  <div className="att-ledger-money">
                    <b>{rwf(fine.amount)}</b>
                    <small>{t.rule} {fine.rule}</small>
                  </div>
                </div>
              ))}
              <footer>
                <span>{t.totals}</span>
                <b>{rwf(fineTotal)}</b>
              </footer>
            </section>
            {detail?.status === "CLOSED" ? (
              <Field label={t.reason} required><TextArea value={reason} onChange={(event) => setReason(event.target.value)} /></Field>
            ) : null}
          </div>
        ) : null}
      </div>
    </Modal>
  );
}
