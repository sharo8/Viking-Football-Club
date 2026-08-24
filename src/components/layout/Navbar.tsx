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
  Bell,
  ChevronDown,
} from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
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
  en: "🇬🇧 EN",
  fr: "🇫🇷 FR",
  rw: "🇷🇼 RW",
};

export function Navbar() {
  const pathname = usePathname();
  const { t, locale, setLocale } = useLocale();
  const { theme, toggleTheme } = useTheme();
  const [open, setOpen] = useState(false);
  const [langOpen, setLangOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 24);
    onScroll();
    window.addEventListener("scroll", onScroll);
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  useEffect(() => {
    setOpen(false);
  }, [pathname]);

  if (pathname.startsWith("/admin")) return null;

  return (
    <header
      className={`fixed inset-x-0 top-0 z-50 transition-all duration-300 ${
        scrolled || open
          ? "bg-[var(--nav)] shadow-lg shadow-black/20 backdrop-blur-xl"
          : "bg-gradient-to-b from-navy-deep/80 to-transparent"
      }`}
    >
      <div className="container-page flex h-16 items-center justify-between gap-3 md:h-20">
        <Link href="/" className="flex items-center gap-2.5 shrink-0">
          <Image
            src={club.logo}
            alt="Viking FC"
            width={48}
            height={48}
            className="h-10 w-10 object-contain md:h-12 md:w-12"
            priority
          />
          <div className="leading-none">
            <p className="font-display text-xl text-white tracking-[0.12em] md:text-2xl">
              VIKING FC
            </p>
            <p className="hidden text-[10px] uppercase tracking-[0.2em] text-white/60 sm:block">
              Official Club
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
                className={`px-2.5 py-2 text-[11px] font-bold uppercase tracking-[0.12em] transition ${
                  active
                    ? "text-red"
                    : "text-white/75 hover:text-white"
                }`}
              >
                {t.nav[link.key]}
              </Link>
            );
          })}
        </nav>

        <div className="flex items-center gap-1.5 sm:gap-2">
          <button
            type="button"
            aria-label="Search"
            onClick={() => setSearchOpen((v) => !v)}
            className="rounded-full p-2 text-white/80 hover:bg-white/10 hover:text-white"
          >
            <Search size={18} />
          </button>
          <button
            type="button"
            aria-label="Notifications"
            className="hidden rounded-full p-2 text-white/80 hover:bg-white/10 hover:text-white sm:inline-flex"
          >
            <Bell size={18} />
          </button>
          <button
            type="button"
            aria-label="Toggle theme"
            onClick={toggleTheme}
            className="rounded-full p-2 text-white/80 hover:bg-white/10 hover:text-white"
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
              className="flex items-center gap-1 rounded-full px-2 py-1.5 text-xs font-bold text-white/85 hover:bg-white/10"
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
                  className="absolute right-0 mt-2 min-w-[120px] overflow-hidden rounded-md border border-white/10 bg-navy-deep shadow-xl"
                >
                  {(Object.keys(localeLabels) as Locale[]).map((code) => (
                    <button
                      key={code}
                      type="button"
                      onClick={() => {
                        setLocale(code);
                        setLangOpen(false);
                      }}
                      className={`block w-full px-3 py-2 text-left text-xs font-semibold ${
                        locale === code
                          ? "bg-red text-white"
                          : "text-white/80 hover:bg-white/10"
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
            className="hidden items-center gap-1 rounded-full px-2 py-1.5 text-white/80 hover:bg-white/10 hover:text-white sm:inline-flex"
          >
            <User size={18} />
          </Link>

          <button
            type="button"
            className="inline-flex rounded-full p-2 text-white xl:hidden"
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
            className="border-t border-white/10 bg-navy-deep/95"
          >
            <div className="container-page py-3">
              <input
                type="search"
                placeholder={t.nav.search + "..."}
                className="w-full bg-white/5 px-4 py-3 text-sm text-white outline-none ring-1 ring-white/15 placeholder:text-white/40 focus:ring-red"
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
            className="overflow-hidden border-t border-white/10 bg-navy-deep xl:hidden"
          >
            <div className="container-page flex flex-col gap-1 py-4">
              {links.map((link) => (
                <Link
                  key={link.href}
                  href={link.href}
                  className="px-2 py-2.5 text-sm font-bold uppercase tracking-[0.14em] text-white/85 hover:text-red"
                >
                  {t.nav[link.key]}
                </Link>
              ))}
              <Link
                href="/login"
                className="mt-2 px-2 py-2.5 text-sm font-bold uppercase tracking-[0.14em] text-red"
              >
                {t.nav.login}
              </Link>
            </div>
          </motion.nav>
        )}
      </AnimatePresence>
    </header>
  );
}
