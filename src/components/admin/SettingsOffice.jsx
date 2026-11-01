"use client";

import { useEffect, useState } from "react";
import { api, getData } from "@/services/api";
import { Button, Field, TextInput } from "@/ui/kit";
import { EmailHealth } from "./EmailHealth";
import { useAdminToast } from "./toast";

const COPY = {
  en: {
    title: "Settings", subtitle: "Club rules used by attendance, fines and payroll.",
    club: "Club", discipline: "Attendance and fines", payroll: "Payroll", email: "Email",
    competition: "Competition", months: "Contract months", phase: "First phase months", fund: "Social fund per phase",
    lateMin: "Late after (minutes)", lateFine: "Late training fine", absenceFine: "Unexcused absence fine",
    matchFine: "Match late or absence fine", onTrack: "On track from", watch: "Watch from",
    save: "Save", saved: "Settings saved", rule6: "Rule 6", rule7: "Rule 7", rule8: "Rule 8",
  },
  fr: {
    title: "Réglages", subtitle: "Les règles du club pour les présences, les amendes et les salaires.",
    club: "Club", discipline: "Présences et amendes", payroll: "Salaires", email: "E-mail",
    competition: "Compétition", months: "Mois de contrat", phase: "Mois de la première phase", fund: "Fonds social par phase",
    lateMin: "Retard après (minutes)", lateFine: "Amende de retard à l'entraînement", absenceFine: "Amende d'absence non excusée",
    matchFine: "Amende de retard ou d'absence au match", onTrack: "Dans les temps à partir de", watch: "À surveiller à partir de",
    save: "Enregistrer", saved: "Réglages enregistrés", rule6: "Règle 6", rule7: "Règle 7", rule8: "Règle 8",
  },
  rw: {
    title: "Igenamiterere", subtitle: "Amategeko y'ikipe ku kwitabira, ibihano n'umushahara.",
    club: "Ikipe", discipline: "Kwitabira n'ibihano", payroll: "Umushahara", email: "Imeri",
    competition: "Irushanwa", months: "Amezi y'amasezerano", phase: "Amezi ya mbere", fund: "Ikigega buri cyiciro",
    lateMin: "Gutinda nyuma y'iminota", lateFine: "Igihano cyo gutinda mu myitozo", absenceFine: "Igihano cyo kutaza",
    matchFine: "Igihano cyo gutinda cyangwa kutaza mu mukino", onTrack: "Ku nzira uva", watch: "Kureba uva",
    save: "Bika", saved: "Byabitswe", rule6: "Itegeko 6", rule7: "Itegeko 7", rule8: "Itegeko 8",
  },
};

function pick(form, key, fallback) {
  const raw = form.get(key);
  if (raw == null || raw === "") return fallback;
  const value = Number(raw);
  return Number.isFinite(value) ? value : fallback;
}

export function SettingsOffice({ locale = "en" }) {
  const t = COPY[locale] || COPY.en;
  const toast = useAdminToast();
  const [view, setView] = useState(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [section, setSection] = useState("discipline");

  useEffect(() => {
    getData("/api/settings").then(setView).catch((err) => setError(err.message));
  }, []);

  async function save(event) {
    event.preventDefault();
    if (!view) return;
    const form = new FormData(event.currentTarget);
    setBusy(true);
    const body = {
      competitionName: String(form.get("competitionName") || view.competitionName),
      playerContractMonths: pick(form, "playerContractMonths", view.playerContractMonths),
      playerFirstPhaseMonths: pick(form, "playerFirstPhaseMonths", view.playerFirstPhaseMonths),
      playerSocialFund: pick(form, "playerSocialFund", view.playerSocialFund),
      staffContractMonths: view.staffContractMonths,
      staffFirstPhaseMonths: view.staffFirstPhaseMonths,
      staffSocialFund: view.staffSocialFund,
      attendanceOnTrackPct: pick(form, "attendanceOnTrackPct", view.attendanceOnTrackPct),
      attendanceWatchPct: pick(form, "attendanceWatchPct", view.attendanceWatchPct),
      trainingLateFine: pick(form, "trainingLateFine", view.trainingLateFine),
      trainingLateMinutes: pick(form, "trainingLateMinutes", view.trainingLateMinutes),
      trainingAbsenceFine: pick(form, "trainingAbsenceFine", view.trainingAbsenceFine),
      matchLateFine: pick(form, "matchLateFine", view.matchLateFine),
      matchLateMinutes: view.matchLateMinutes,
      matchAbsenceFine: pick(form, "matchAbsenceFine", view.matchAbsenceFine),
      rule6Text: view.rule6Text,
      rule7Text: view.rule7Text,
      maxSubstitutes: view.maxSubstitutes,
      matchLengthMinutes: view.matchLengthMinutes,
    };
    try {
      const response = await api.put("/api/settings", body);
      setView(response.data?.data || view);
      toast(t.saved);
      setError("");
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="crud-page">
      <header className="crud-head">
        <div>
          <h1>{t.title}</h1>
          <p>{t.subtitle}</p>
        </div>
      </header>
      <div className="settings-nav">
        {["discipline", "payroll", "club", "email"].map((key) => (
          <Button key={key} variant={section === key ? "primary" : "secondary"} onClick={() => setSection(key)}>{t[key]}</Button>
        ))}
      </div>
      {error ? <p className="crud-error">{error}</p> : null}
      {view && section !== "email" ? (
        <form className="settings-card" onSubmit={save}>
          {section === "club" ? (
            <Field label={t.competition}><TextInput name="competitionName" defaultValue={view.competitionName} /></Field>
          ) : null}
          {section === "discipline" ? (
            <>
              <Field label={`${t.rule6} · ${t.lateMin}`}><TextInput name="trainingLateMinutes" type="number" min="1" defaultValue={view.trainingLateMinutes} /></Field>
              <Field label={`${t.rule6} · ${t.lateFine}`}><TextInput name="trainingLateFine" type="number" min="0" defaultValue={view.trainingLateFine} /></Field>
              <Field label={`${t.rule7} · ${t.absenceFine}`}><TextInput name="trainingAbsenceFine" type="number" min="0" defaultValue={view.trainingAbsenceFine} /></Field>
              <Field label={`${t.rule8} · ${t.matchFine}`}><TextInput name="matchLateFine" type="number" min="0" defaultValue={view.matchLateFine} /></Field>
              <input type="hidden" name="matchAbsenceFine" value={view.matchAbsenceFine} />
              <Field label={t.onTrack}><TextInput name="attendanceOnTrackPct" type="number" min="1" max="100" defaultValue={view.attendanceOnTrackPct} /></Field>
              <Field label={t.watch}><TextInput name="attendanceWatchPct" type="number" min="1" max="100" defaultValue={view.attendanceWatchPct} /></Field>
            </>
          ) : null}
          {section === "payroll" ? (
            <>
              <Field label={t.months}><TextInput name="playerContractMonths" type="number" min="1" defaultValue={view.playerContractMonths} /></Field>
              <Field label={t.phase}><TextInput name="playerFirstPhaseMonths" type="number" min="1" defaultValue={view.playerFirstPhaseMonths} /></Field>
              <Field label={t.fund}><TextInput name="playerSocialFund" type="number" min="0" defaultValue={view.playerSocialFund} /></Field>
            </>
          ) : null}
          <Button variant="primary" loading={busy} type="submit">{t.save}</Button>
        </form>
      ) : null}
      {section === "email" ? <EmailHealth /> : null}
    </section>
  );
}
