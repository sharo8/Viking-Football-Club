"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { getData } from "@/services/api";
import { useLocale } from "@/components/providers/LocaleProvider";
import en from "@/locales/office/en.json";
import fr from "@/locales/office/fr.json";
import rw from "@/locales/office/rw.json";

const COPY = { en, fr, rw };
const ALERT_KEY = {
  RESULT_MISSING: "alertResult",
  LINEUP_NOW: "alertLineupNow",
  NO_LINEUP: "alertNoLineup",
  LINEUP_DRAFT: "alertLineupDraft",
  LINEUP_SUBMITTED: "alertLineupSubmitted",
  LINEUP_APPROVED: "alertLineupApproved",
  LINEUP: "alertLineup",
};

const DISMISS = "vfc_alerts_dismissed";

function alertCode(row) {
  if (row.code && ALERT_KEY[row.code]) return row.code;
  const title = String(row.title || "");
  if (title.startsWith("Result missing for ")) return "RESULT_MISSING";
  if (title.startsWith("Build lineup now")) return "LINEUP_NOW";
  if (title.startsWith("No lineup yet")) return "NO_LINEUP";
  if (title.startsWith("Lineup draft")) return "LINEUP_DRAFT";
  if (title.startsWith("Submitted, waiting")) return "LINEUP_SUBMITTED";
  if (title.startsWith("Approved, ready")) return "LINEUP_APPROVED";
  if (title.startsWith("Lineup vs ") || title.startsWith("Lineup against ")) return "LINEUP";
  return "";
}

function alertName(row) {
  if (row.opponent) return row.opponent;
  const title = String(row.title || "");
  const tail = title.match(/(?: for | vs | against )(.+)$/);
  return tail ? tail[1] : "";
}

function alertText(t, row) {
  const code = alertCode(row);
  const template = ALERT_KEY[code] ? t[ALERT_KEY[code]] : "";
  if (!template) return row.title;
  return template.replace("{name}", alertName(row));
}

export function AttentionBar() {
  const { locale } = useLocale();
  const t = COPY[locale] || en;
  const [rows, setRows] = useState([]);
  const [hidden, setHidden] = useState([]);

  useEffect(() => {
    try {
      setHidden(JSON.parse(sessionStorage.getItem(DISMISS) || "[]"));
    } catch {
      setHidden([]);
    }
    getData("/api/alerts/me").then((list) => setRows(Array.isArray(list) ? list : [])).catch(() => setRows([]));
  }, []);

  const visible = rows.filter((row) => !hidden.includes(row.title));
  if (!visible.length) return null;

  const dismiss = (title) => {
    const next = [...hidden, title];
    setHidden(next);
    sessionStorage.setItem(DISMISS, JSON.stringify(next));
  };

  return (
    <div className="alert-strip" aria-label={t.alertAttention}>
      {visible.slice(0, 3).map((row) => (
        <div key={row.title} className="alert-strip-item">
          <span>{alertText(t, row)}</span>
          <Link href={row.link || "/admin"}>{t.view}</Link>
          <button type="button" onClick={() => dismiss(row.title)} aria-label={t.close}>×</button>
        </div>
      ))}
    </div>
  );
}
