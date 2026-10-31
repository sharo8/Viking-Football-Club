"use client";

import Link from "next/link";
import Image from "next/image";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import {
  Menu,
  X,
  Search,
  Sun,
  Moon,
  User,
  ChevronDown,
} from "lucide-react";
import { AnimatePresence, motion } from "framer-motion";
import { club } from "@/lib/data";
import { useLocale } from "@/components/providers/LocaleProvider";
import { useTheme } from "@/components/providers/ThemeProvider";
import type { Locale } from "@/lib/i18n";

const links = [
  { href: "/", key: "home" as const },
  { href: "/about", key: "about" as const },
  { href: "/teams", key: "teams" as const },
  { href: "/matches", key: "matches" as const },
  { href: "/players", key: "players" as const },
  { href: "/management", key: "management" as const },
  { href: "/news", key: "news" as const },
  { href: "/gallery", key: "gallery" as const },
  { href: "/services", key: "services" as const },
  { href: "/shop", key: "shop" as const },
  { href: "/contact", key: "contact" as const },
];

const localeLabels: Record<Locale, string> = {
  en: "EN",
  fr: "FR",
  rw: "RW",
};

export function Navbar() {
  const pathname = usePathname();
  const { t, locale, setLocale } = useLocale();
  const { theme, toggleTheme } = useTheme();
  const [open, setOpen] = useState(false);
  const [langOpen, setLangOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);
  const [progress, setProgress] = useState(0);

  useEffect(() => {
    const onScroll = () => {
      setScrolled(window.scrollY > 40);
      const height = document.documentElement.scrollHeight - window.innerHeight;
      setProgress(height > 0 ? Math.min(1, window.scrollY / height) : 0);
    };
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  useEffect(() => {
    setOpen(false);
    setLangOpen(false);
  }, [pathname]);

  if (pathname.startsWith("/admin")) return null;

  const isDark = theme === "dark";

  return (
    <>
    <header
      className={`fixed inset-x-0 top-0 z-[100] border-b text-[var(--nav-text)] transition-[background-color,color,border-color,box-shadow,backdrop-filter,height] duration-[400ms] ease-[cubic-bezier(0.16,1,0.3,1)] ${
        scrolled
          ? "border-[var(--border)] bg-[var(--nav)]/95 shadow-[0_8px_30px_rgba(0,0,0,0.12)] backdrop-blur-xl"
          : "border-transparent bg-[var(--nav)]/80 backdrop-blur-md"
      }`}
    >
      <div className={`container-page flex items-center justify-between gap-3 transition-[height] duration-300 ${scrolled ? "h-14" : "h-16 md:h-[4.5rem]"}`}>
        <Link href="/" className="flex shrink-0 items-center gap-2.5">
          <Image
            src={club.logo}
            alt="Viking FC"
            width={48}
            height={48}
            className={`h-10 w-10 object-contain md:h-11 md:w-11 ${isDark ? "brightness-110" : ""}`}
            priority
          />
          <div className="leading-none">
            <p className="font-display text-xl tracking-[0.12em] text-[var(--nav-text)] md:text-2xl">
              VIKING FC
            </p>
            <p className="hidden font-data text-[10px] uppercase tracking-[0.2em] text-[var(--nav-text)]/70 sm:block">
              {t.nav.officialClub}
            </p>
          </div>
        </Link>

        <nav className="hidden items-center gap-0.5 xl:flex">
          {links.map((link) => {
            const active =
              link.href === "/"
                ? pathname === "/"
                : pathname.startsWith(link.href);

            return (
              <Link
                key={link.href}
                href={link.href}
                className={`nav-link px-2.5 py-2 font-data text-[10px] font-semibold uppercase tracking-[0.14em] transition-colors duration-300 ${
                  active
                    ? "is-active text-[var(--accent)]"
                    : "text-[var(--nav-text)]/90 hover:text-[var(--accent)]"
                }`}
              >
                {t.nav[link.key]}
              </Link>
            );
          })}
        </nav>

        <div className="flex items-center gap-1 sm:gap-1.5">
          <button
            type="button"
            aria-label="Search"
            onClick={() => setSearchOpen((v) => !v)}
            className="rounded-full p-2 text-[var(--nav-text)]/90 transition hover:bg-black/5 dark:hover:bg-white/10"
          >
            <Search size={18} />
          </button>

          <button
            type="button"
            aria-label="Toggle theme"
            onClick={toggleTheme}
            className="rounded-full p-2 text-[var(--nav-text)]/90 transition hover:bg-black/5 dark:hover:bg-white/10"
          >
            <AnimatePresence mode="wait" initial={false}>
              <motion.span
                key={theme}
                initial={{ rotate: -90, opacity: 0 }}
                animate={{ rotate: 0, opacity: 1 }}
                exit={{ rotate: 90, opacity: 0 }}
                transition={{ duration: 0.25 }}
                className="inline-flex"
              >
                {theme === "dark" ? <Sun size={18} /> : <Moon size={18} />}
              </motion.span>
            </AnimatePresence>
          </button>

          <div className="relative">
            <button
              type="button"
              onClick={() => setLangOpen((v) => !v)}
              className="flex items-center gap-1 rounded-full px-2 py-1.5 font-data text-[11px] font-semibold tracking-wider text-[var(--nav-text)] hover:bg-black/5 dark:hover:bg-white/10"
            >
              {localeLabels[locale]}
              <ChevronDown size={14} />
            </button>
            <AnimatePresence>
              {langOpen && (
                <motion.div
                  initial={{ opacity: 0, y: -6 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -6 }}
                  className="absolute right-0 mt-2 min-w-[110px] overflow-hidden rounded-md border border-[var(--border)] bg-[var(--card)] shadow-xl"
                >
                  {(Object.keys(localeLabels) as Locale[]).map((code) => (
                    <button
                      key={code}
                      type="button"
                      onClick={() => {
                        setLocale(code);
                        setLangOpen(false);
                      }}
                      className={`block w-full px-3 py-2 text-left font-data text-xs font-semibold ${
                        locale === code
                          ? "bg-[var(--accent)] text-[var(--accent-ink)]"
                          : "text-[var(--text)] hover:bg-black/5 dark:hover:bg-white/10"
                      }`}
                    >
                      {localeLabels[code]}
                    </button>
                  ))}
                </motion.div>
              )}
            </AnimatePresence>
          </div>

          <Link
            href="/login"
            className="hidden rounded-full p-2 text-[var(--nav-text)]/90 hover:bg-black/5 dark:hover:bg-white/10 sm:inline-flex"
          >
            <User size={18} />
          </Link>

          <button
            type="button"
            className="inline-flex rounded-full p-2 text-[var(--nav-text)] xl:hidden"
            onClick={() => setOpen((v) => !v)}
            aria-label="Menu"
          >
            {open ? <X size={22} /> : <Menu size={22} />}
          </button>
        </div>
      </div>

      <AnimatePresence>
        {searchOpen && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: "auto", opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            className="overflow-hidden border-t border-[var(--border)] bg-[var(--nav)]"
          >
            <div className="container-page py-3">
              <input
                type="search"
                placeholder={t.nav.search + "..."}
                className="w-full bg-[var(--bg-alt)] px-4 py-3 text-sm text-[var(--text)] outline-none ring-1 ring-[var(--border)] placeholder:text-[var(--text-muted)] focus:ring-[var(--accent)]"
              />
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      <AnimatePresence>
        {open && (
          <motion.nav
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: "auto", opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            className="overflow-hidden border-t border-[var(--border)] bg-[var(--nav)] xl:hidden"
          >
            <div className="container-page flex flex-col gap-1 py-4">
              {links.map((link) => {
                const active =
                  link.href === "/"
                    ? pathname === "/"
                    : pathname.startsWith(link.href);
                return (
                  <Link
                    key={link.href}
                    href={link.href}
                    className={`px-2 py-2.5 font-data text-sm font-semibold uppercase tracking-[0.14em] ${
                      active
                        ? "text-[var(--accent)]"
                        : "text-[var(--nav-text)] hover:text-[var(--accent)]"
                    }`}
                  >
                    {t.nav[link.key]}
                  </Link>
                );
              })}
              <Link
                href="/login"
                className="mt-2 px-2 py-2.5 font-data text-sm font-semibold uppercase tracking-[0.14em] text-[var(--accent)]"
              >
                {t.nav.login}
              </Link>
            </div>
          </motion.nav>
        )}
      </AnimatePresence>
      <div className="absolute inset-x-0 bottom-0 h-[2px] bg-[var(--border)]" aria-hidden="true">
        <div className="h-full bg-[var(--accent-brand)]" style={{ width: `${progress * 100}%` }} />
      </div>
    </header>
    <div className="h-16 shrink-0 md:h-[4.5rem]" aria-hidden="true" />
    </>
  );
}
