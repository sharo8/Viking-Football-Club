"use client";

import { useState } from "react";
import { api } from "@/services/api";
import { useLocale } from "@/components/providers/LocaleProvider";
import { Field, TextInput, Button } from "@/ui/kit";
import en from "@/locales/office/en.json";
import fr from "@/locales/office/fr.json";
import rw from "@/locales/office/rw.json";

const COPY = { en, fr, rw };

export function localizedName(row, locale) {
  if (!row) return "";
  const source = row.sourceLang || "en";
  const bag = {
    en: row.nameEn || (source === "en" ? row.name : ""),
    fr: row.nameFr || "",
    rw: row.nameRw || "",
  };
  if (row.name && !bag[source]) bag[source] = row.name;
  const direct = String(bag[locale] || "").trim();
  if (direct) return direct;
  const main = String(bag[source] || row.name || row.nameEn || row.nameFr || row.nameRw || "").trim();
  return main;
}

export function TranslatedName({ value, onChange, onError }) {
  const { locale } = useLocale();
  const t = COPY[locale] || en;
  const [busy, setBusy] = useState(false);

  const fill = async () => {
    const text = (value.name || "").trim();
    if (!text) return;
    const to = ["fr", "rw"].filter((lang) => lang !== locale && !String(value[lang === "fr" ? "nameFr" : "nameRw"] || "").trim());
    if (!to.length) return;
    setBusy(true);
    try {
      const response = await api.post("/api/translate", { text, from: locale, to });
      const suggested = response.data?.data || {};
      onChange({
        ...value,
        nameFr: value.nameFr || suggested.fr || "",
        nameRw: value.nameRw || suggested.rw || "",
      });
    } catch {
      onError(t.translateFailed);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="span-2">
      <Field label={t.name} required>
        <TextInput value={value.name || ""} onChange={(event) => onChange({ ...value, name: event.target.value, sourceLang: value.sourceLang || locale })} />
      </Field>
      <details className="name-translations">
        <summary>{t.translationsOptional}</summary>
        <div className="crud-grid">
          <Field label={t.nameFr} hint={t.checkTranslation}>
            <TextInput value={value.nameFr || ""} onChange={(event) => onChange({ ...value, nameFr: event.target.value })} />
          </Field>
          <Field label={t.nameRw} hint={t.checkTranslation}>
            <TextInput value={value.nameRw || ""} onChange={(event) => onChange({ ...value, nameRw: event.target.value })} />
          </Field>
        </div>
        <Button type="button" loading={busy} onClick={fill}>{t.autoTranslate}</Button>
      </details>
    </div>
  );
}
