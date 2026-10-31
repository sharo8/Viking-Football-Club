"use client";

import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { translations, type Locale, type TranslationKeys } from "@/lib/i18n";

type LocaleContextValue = {
  locale: Locale;
  setLocale: (locale: Locale) => void;
  t: TranslationKeys;
};

const LocaleContext = createContext<LocaleContextValue | null>(null);

const htmlLang: Record<Locale, string> = {
  en: "en",
  fr: "fr",
  rw: "rw",
};

export function LocaleProvider({ children }: { children: ReactNode }) {
  const [locale, setLocaleState] = useState<Locale>("en");

  useEffect(() => {
    const stored = (localStorage.getItem("vfc_lang") || localStorage.getItem("viking-locale")) as Locale | null;
    if (stored && stored in translations) setLocaleState(stored);
  }, []);

  useEffect(() => {
    document.documentElement.lang = htmlLang[locale];
  }, [locale]);

  const setLocale = (next: Locale) => {
    if (!(next in translations)) return;
    setLocaleState(next);
    localStorage.setItem("vfc_lang", next);
    localStorage.setItem("viking-locale", next);
  };

  const value = useMemo(
    () => ({
      locale,
      setLocale,
      t: translations[locale] as TranslationKeys,
    }),
    [locale],
  );

  return (
    <LocaleContext.Provider value={value}>{children}</LocaleContext.Provider>
  );
}

export function useLocale() {
  const ctx = useContext(LocaleContext);
  if (!ctx) throw new Error("useLocale must be used within LocaleProvider");
  return ctx;
}
