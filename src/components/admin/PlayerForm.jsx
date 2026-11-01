"use client";

import { useEffect, useMemo, useState } from "react";
import { Check } from "lucide-react";
import { api, getData } from "@/services/api";
import { Avatar, Button, Field, Modal, PhoneInput, SelectInput, TextInput } from "@/ui/kit";
import { BONUS_LABEL, POSITION, STATUS, label, money } from "./playersCopy";
import { BONUS_TYPES, paySummary, profileIssues } from "./playerRules";
import { TranslatedName } from "./TranslatedName";

const EMPTY = {
  fullName: "", knownAs: "", jersey: "", position: "FORWARD", dateOfBirth: "", nationality: "Rwanda",
  foot: "Right", heightCm: "", weightKg: "", status: "ACTIVE", phone: "", email: "", preferredLanguage: "en",
  address: "", emergencyName: "", emergencyRelationship: "", emergencyPhone: "", emergencyEmail: "",
  seasonalPay: "", advance: "0", releaseClause: "", marketValue: "", sfExempt: false, socialFund: "",
  contractStart: "", contractEnd: "", confirmDuplicate: false,
};

function yearsOf(value) {
  if (!value) return null;
  const born = new Date(value);
  if (Number.isNaN(born.getTime())) return null;
  const now = new Date();
  let years = now.getFullYear() - born.getFullYear();
  const month = now.getMonth() - born.getMonth();
  if (month < 0 || (month === 0 && now.getDate() < born.getDate())) years -= 1;
  return years;
}

export function PlayerForm({ copy, locale, executive, player, initialStep = 0, onClose, onSaved }) {
  const [step, setStep] = useState(initialStep);
  const [form, setForm] = useState({ ...EMPTY, ...(player || {}), dateOfBirth: String(player?.dateOfBirth || "").slice(0, 10) });
  const [errors, setErrors] = useState({});
  const [taken, setTaken] = useState(null);
  const [used, setUsed] = useState({});
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState("");
  const [openJersey, setOpenJersey] = useState(false);
  const [done, setDone] = useState(false);
  const [photoFile, setPhotoFile] = useState(null);
  const [photoPreview, setPhotoPreview] = useState(player?.photoUrl || "");
  const [bonuses, setBonuses] = useState([]);
  const [baseline] = useState(() => JSON.stringify({ ...EMPTY, ...(player || {}) }));
  const creating = !player?.id;
  const steps = executive ? [copy.step1, copy.step2, copy.step3] : [copy.step1, copy.step2];
  const problems = profileIssues(form, { creating, executive, hasPhoto: Boolean(photoFile || photoPreview), locale });
  const blocking = problems.filter((item) => creating || item.code !== "REQUIRED");
  const missing = profileIssues(form, { creating: true, executive, hasPhoto: Boolean(photoFile || photoPreview), locale }).filter((item) => item.code === "REQUIRED");
  const byStep = [0, 1, 2].map((index) => missing.filter((item) => item.step === index).length);
  const pay = paySummary(form);
  const age = useMemo(() => yearsOf(form.dateOfBirth), [form.dateOfBirth]);

  useEffect(() => {
    if (!executive || player?.id) return;
    getData("/api/settings").then((view) => {
      if (view?.playerSocialFund != null) set("socialFund", String(view.playerSocialFund));
    }).catch(() => {});
  }, [executive, player?.id]);

  useEffect(() => {
    getData("/api/players/directory", { size: 100 }).then((page) => {
      const map = {};
      for (const row of page.content || []) {
        if (row.jersey != null) map[row.jersey] = row.fullName;
      }
      setUsed(map);
    }).catch(() => {});
  }, []);

  useEffect(() => {
    if (!form.jersey) return undefined;
    const timer = window.setTimeout(() => {
      getData("/api/players/jersey-availability", { number: form.jersey, excludeId: player?.id }).then(setTaken).catch(() => setTaken(null));
    }, 250);
    return () => window.clearTimeout(timer);
  }, [form.jersey, player?.id]);

  function set(key, value) {
    setForm((current) => ({ ...current, [key]: value }));
    setErrors((current) => ({ ...current, [key]: undefined }));
  }

  function localErrors() {
    const next = {};
    for (const item of (creating ? problems : blocking)) next[item.field] = item.message;
    if (taken?.taken) next.jersey = `${form.jersey}: ${taken.owner}`;
    return next;
  }

  function onPhoto(file) {
    if (!file) return;
    const image = new Image();
    image.onload = () => {
      const canvas = document.createElement("canvas");
      canvas.width = 800;
      canvas.height = 800;
      const size = Math.min(image.width, image.height);
      canvas.getContext("2d").drawImage(image, (image.width - size) / 2, (image.height - size) / 2, size, size, 0, 0, 800, 800);
      canvas.toBlob((blob) => {
        setPhotoFile(new File([blob], "photo.jpg", { type: "image/jpeg" }));
        setPhotoPreview(canvas.toDataURL("image/jpeg"));
      }, "image/jpeg", 0.9);
    };
    image.src = URL.createObjectURL(file);
  }

  function requestClose() {
    if (JSON.stringify(form) !== baseline && !window.confirm(copy.unsaved)) return;
    onClose();
  }

  async function save() {
    const next = localErrors();
    setErrors(next);
    if (Object.keys(next).length) {
      setStep(next.dateOfBirth || next.fullName || next.jersey || next.position ? 0 : next.email ? 1 : step);
      return;
    }
    setBusy(true);
    setNotice("");
    try {
      const payload = { ...form, id: player?.id, version: player?.version, jersey: Number(form.jersey), sfExempt: Boolean(form.sfExempt) };
      const body = new FormData();
      body.append("player", new Blob([JSON.stringify(payload)], { type: "application/json" }));
      if (photoFile) body.append("photo", photoFile);
      const saved = await api.post("/api/players/form", body);
      const savedId = saved.data?.data?.id || player?.id;
      const ready = bonuses.filter((bonus) => Number(bonus.amount) > 0);
      if (executive && savedId && ready.length) {
        await api.put(`/api/players/${savedId}/bonus-rules`, ready);
      }
      setDone(true);
      window.setTimeout(onSaved, 700);
    } catch (err) {
      const fieldErrors = err.data?.errors || [];
      if (fieldErrors.length) {
        const mapped = {};
        for (const item of fieldErrors) mapped[item.field] = item.message;
        setErrors(mapped);
        const field = fieldErrors[0]?.field || "";
        if (["phone", "email", "address", "emergencyName"].includes(field)) setStep(1);
        else if (["seasonalPay", "releaseClause"].includes(field)) setStep(2);
        else setStep(0);
      }
      if (err.code === "JERSEY_TAKEN") setErrors((current) => ({ ...current, jersey: err.message }));
      if (err.code === "PLAYER_EXISTS") setNotice(err.message);
      else if (!fieldErrors.length && err.code !== "JERSEY_TAKEN") setNotice(err.message);
    } finally {
      setBusy(false);
    }
  }

  const lines = ["Goalkeepers", "Defenders", "Midfielders", "Attackers"];

  return (
    <Modal size="lg" title={player ? copy.edit : copy.add} subtitle={copy.sub} onClose={requestClose} closeLabel={copy.close}
      footer={done ? null : (
        <>
          <Button variant="ghost" onClick={requestClose}>{copy.cancel}</Button>
          <div className="vfc-foot-right">
            {step > 0 ? <Button variant="secondary" onClick={() => setStep((value) => value - 1)}>{copy.back}</Button> : null}
            {step < steps.length - 1 ? <Button variant="primary" onClick={() => setStep((value) => value + 1)}>{copy.nextStep}</Button> : (
              <>
                {creating && missing.length ? <span className="vfc-error">{copy.missingLine}: {missing.map((item) => item.field).join(", ")}</span> : null}
                <Button variant="primary" loading={busy} {...(creating && (blocking.length > 0 || taken?.taken) ? { disabled: true } : {})} onClick={() => { if (creating && (blocking.length || taken?.taken)) { setErrors(localErrors()); setStep((blocking[0] || { step: 0 }).step); return; } save(); }}>{copy.savePlayer}</Button>
              </>
            )}
          </div>
        </>
      )}>
      {done ? (
        <div className="vfc-check" role="status"><Check size={36} /></div>
      ) : (
        <div className="vfc-split">
          <aside className="vfc-rail">
            <div className="vfc-preview">
              <span className="vfc-watermark">{form.jersey || "–"}</span>
              <label className="vfc-avatar vfc-avatar--96" title="Photo">
                {photoPreview ? <img src={photoPreview} alt="" /> : (form.fullName || "").trim().slice(0, 2).toUpperCase()}
                <input type="file" accept="image/jpeg,image/png,image/webp" hidden onChange={(event) => onPhoto(event.target.files?.[0])} />
              </label>
              <strong>{form.fullName || copy.name}</strong>
              <span>{form.knownAs}</span>
              <span className="vfc-chip">{form.position ? label(POSITION, form.position, locale) : copy.position}</span>
              {age != null ? <span className="vfc-chip">{copy.age} {age}</span> : null}
            </div>
            <div className="vfc-steps">
              {steps.map((name, index) => (
                <button key={name} type="button" className={step === index ? "is-on" : ""} onClick={() => setStep(index)}>
                  <b>{byStep[index] ? byStep[index] : "✓"}</b> {name}
                  {byStep[index] ? <i className="dot" /> : null}
                </button>
              ))}
            </div>
          </aside>
          <div className="vfc-grid">
            {step === 0 ? (
              <>
                <Field label={copy.name} required error={errors.fullName}><TextInput value={form.fullName} onChange={(event) => set("fullName", event.target.value)} /></Field>
                <Field label={copy.knownAs}><TextInput value={form.knownAs || ""} onChange={(event) => set("knownAs", event.target.value)} /></Field>
                <Field label={copy.dob} required error={errors.dateOfBirth} hint={age != null ? `${copy.age} ${age}` : ""}>
                  <TextInput type="date" value={form.dateOfBirth || ""} onChange={(event) => set("dateOfBirth", event.target.value)} />
                </Field>
                <Field label={copy.position} required error={errors.position}>
                  <SelectInput value={form.position} onChange={(event) => set("position", event.target.value)}>
                    {lines.map((line) => (
                      <optgroup key={line} label={line}>
                        {Object.entries(POSITION).filter(([, value]) => value.line === line).map(([key]) => <option key={key} value={key}>{label(POSITION, key, locale)}</option>)}
                      </optgroup>
                    ))}
                  </SelectInput>
                </Field>
                <Field label={copy.jersey} required error={errors.jersey} hint={taken?.taken ? taken.suggestions?.slice(0, 3).map((number) => `#${number}`).join("  ") : ""}>
                  <div className="vfc-pop">
                    <Button onClick={() => setOpenJersey((value) => !value)}>{form.jersey ? `#${form.jersey}` : copy.pickNumber}</Button>
                    {openJersey ? (
                      <div className="vfc-popover">
                        <div className="vfc-jersey">
                          {Array.from({ length: 99 }, (_, index) => index + 1).map((number) => {
                            const owner = used[number];
                            const mine = player && String(player.jersey) === String(number);
                            return (
                              <button key={number} type="button" title={owner && !mine ? owner : String(number)} disabled={Boolean(owner) && !mine}
                                className={String(form.jersey) === String(number) ? "is-on" : ""}
                                onClick={() => { set("jersey", number); setOpenJersey(false); }}>{number}</button>
                            );
                          })}
                        </div>
                        <Button size="sm" onClick={() => set("jersey", (taken?.suggestions || [1])[0])}>{copy.nextFree}</Button>
                      </div>
                    ) : null}
                  </div>
                </Field>
                <Field label={copy.nationality}><TextInput value={form.nationality || ""} onChange={(event) => set("nationality", event.target.value)} /></Field>
                <Field label={copy.foot}>
                  <div className="vfc-seg">
                    {[["Left", copy.left], ["Right", copy.right], ["Both", copy.both]].map(([value, text]) => (
                      <button key={value} type="button" className={form.foot === value ? "is-on" : ""} onClick={() => set("foot", value)}>{text}</button>
                    ))}
                  </div>
                </Field>
                <Field label={copy.height}><TextInput inputMode="numeric" value={form.heightCm || ""} onChange={(event) => set("heightCm", event.target.value)} /></Field>
                <Field label={copy.weight}><TextInput inputMode="numeric" value={form.weightKg || ""} onChange={(event) => set("weightKg", event.target.value)} /></Field>
                <Field label={copy.status}>
                  <SelectInput value={form.status || "ACTIVE"} onChange={(event) => set("status", event.target.value)}>
                    {Object.keys(STATUS).map((key) => <option key={key} value={key}>{label(STATUS, key, locale)}</option>)}
                  </SelectInput>
                </Field>
              </>
            ) : null}
            {step === 1 ? (
              <>
                <Field label={copy.phone} error={errors.phone}><PhoneInput value={form.phone || ""} aria-label={copy.phone} onChange={(value) => set("phone", value)} /></Field>
                <Field label={copy.email} error={errors.email}><TextInput value={form.email || ""} onChange={(event) => set("email", event.target.value)} /></Field>
                <Field label={copy.language}>
                  <div className="vfc-seg">
                    {["en", "fr", "rw"].map((value) => (
                      <button key={value} type="button" className={(form.preferredLanguage || "en") === value ? "is-on" : ""} onClick={() => set("preferredLanguage", value)}>{value.toUpperCase()}</button>
                    ))}
                  </div>
                </Field>
                <Field label={copy.address}><TextInput value={form.address || ""} onChange={(event) => set("address", event.target.value)} /></Field>
                <p className="vfc-section">{copy.emergency}</p>
                <Field label={copy.name}><TextInput value={form.emergencyName || ""} onChange={(event) => set("emergencyName", event.target.value)} /></Field>
                <Field label={copy.relationship}><TextInput value={form.emergencyRelationship || ""} onChange={(event) => set("emergencyRelationship", event.target.value)} /></Field>
                <Field label={copy.phone} error={errors.emergencyPhone}><PhoneInput value={form.emergencyPhone || ""} aria-label={copy.phone} onChange={(value) => set("emergencyPhone", value)} /></Field>
                <Field label={copy.email}><TextInput value={form.emergencyEmail || ""} onChange={(event) => set("emergencyEmail", event.target.value)} /></Field>
              </>
            ) : null}
            {step === 2 ? (
              executive ? (
                <>
                  <Field label={copy.pay}><TextInput inputMode="numeric" value={form.seasonalPay || ""} onChange={(event) => set("seasonalPay", event.target.value)} /></Field>
                  <Field label="Advance"><TextInput inputMode="numeric" value={form.advance || ""} onChange={(event) => set("advance", event.target.value)} /></Field>
                  <Field label="Release clause"><TextInput inputMode="numeric" value={form.releaseClause || ""} onChange={(event) => set("releaseClause", event.target.value)} /></Field>
                  <Field label="Market value"><TextInput inputMode="numeric" value={form.marketValue || ""} onChange={(event) => set("marketValue", event.target.value)} /></Field>
                  <Field label={copy.start} error={errors.contractStart}><TextInput type="date" value={form.contractStart || ""} onChange={(event) => set("contractStart", event.target.value)} /></Field>
                  <Field label={copy.end} error={errors.contractEnd}><TextInput type="date" value={form.contractEnd || ""} onChange={(event) => set("contractEnd", event.target.value)} /></Field>
                  <Field label={copy.socialFund} error={errors.socialFund}><TextInput inputMode="numeric" disabled={Boolean(form.sfExempt)} value={form.sfExempt ? "0" : form.socialFund || ""} onChange={(event) => set("socialFund", event.target.value)} /></Field>
                  <label className="vfc-field"><span className="vfc-label"><input type="checkbox" checked={Boolean(form.sfExempt)} onChange={(event) => set("sfExempt", event.target.checked)} /> {copy.exempt}</span></label>
                  <article className="vfc-tile span-2">
                    <small>{copy.monthly}</small>
                    <strong>{money(pay.monthly)}</strong>
                    <p>{copy.monthly} {money(pay.monthly)} · 3 {money(pay.three)} · {copy.socialFund} {money(pay.social1)} · {money(pay.net3)} · {money(pay.envelope)}</p>
                    {pay.shortfall ? <em className="vfc-error">{copy.socialShort.replace("{monthly}", money(pay.monthly)).replace("{three}", money(pay.three)).replace("{fund}", money(pay.social1))}</em> : null}
                    {!pay.shortfall && form.seasonalPay !== "" && pay.owed > 0 ? <em className="vfc-error">{copy.advanceHigh} {money(pay.owed)}</em> : null}
                  </article>
                  <div className="span-2">
                    <p className="vfc-section">{copy.bonuses}</p>
                    {bonuses.map((bonus, index) => (
                      <div key={index} className="vfc-grid">
                        <SelectInput value={bonus.type} onChange={(event) => setBonuses((rows) => rows.map((item, itemIndex) => itemIndex === index ? { ...item, type: event.target.value } : item))}>
                          {BONUS_TYPES.map((type) => <option key={type} value={type}>{label(BONUS_LABEL, type, locale)}</option>)}
                        </SelectInput>
                        {bonus.type === "CUSTOM" ? (
                          <TranslatedName
                            value={{ name: bonus.customName || "", nameFr: bonus.nameFr || "", nameRw: bonus.nameRw || "", sourceLang: bonus.sourceLang || locale }}
                            onChange={(next) => setBonuses((rows) => rows.map((item, itemIndex) => itemIndex === index ? { ...item, customName: next.name, nameFr: next.nameFr, nameRw: next.nameRw, sourceLang: next.sourceLang || locale } : item))}
                            onError={setNotice}
                          />
                        ) : null}
                        <Field label={copy.amountRwf}><TextInput inputMode="numeric" value={bonus.amount} onChange={(event) => setBonuses((rows) => rows.map((item, itemIndex) => itemIndex === index ? { ...item, amount: event.target.value } : item))} /></Field>
                        <Button size="sm" variant="danger" onClick={() => setBonuses((rows) => rows.filter((_, itemIndex) => itemIndex !== index))}>{copy.remove}</Button>
                      </div>
                    ))}
                    <Button {...(bonuses.length >= 3 ? { disabled: true } : {})} onClick={() => setBonuses((rows) => [...rows, { type: "GOAL", amount: "", active: true }])}>{bonuses.length >= 3 ? copy.maxBonus : copy.addBonus}</Button>
                  </div>
                </>
              ) : <p>{copy.locked}</p>
            ) : null}
            {notice ? (
              <p className="span-2 vfc-error">{notice}
                <label><input type="checkbox" checked={Boolean(form.confirmDuplicate)} onChange={(event) => set("confirmDuplicate", event.target.checked)} /> {copy.confirm}</label>
              </p>
            ) : null}
          </div>
        </div>
      )}
    </Modal>
  );
}
