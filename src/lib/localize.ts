import type { Locale } from "@/lib/i18n";

export type LocalizedString = {
  en: string;
  fr: string;
  rw: string;
};

export function L(
  en: string,
  fr: string,
  rw: string,
): LocalizedString {
  return { en, fr, rw };
}

export function localize(
  value: LocalizedString | string,
  locale: Locale,
): string {
  if (typeof value === "string") return value;
  return value[locale] || value.en;
}
