"use client";

import Link from "next/link";
import Image from "next/image";
import { motion } from "framer-motion";
import {
  matches,
  news,
  leagueTable,
  sponsors,
  nextMatchDate,
  club,
  images,
} from "@/lib/data";
import { useLocale } from "@/components/providers/LocaleProvider";
import { Countdown } from "@/components/home/Countdown";
import { Hero } from "@/components/home/Hero";

export function HomePage() {
  const { t } = useLocale();
  const featured = matches.find((m) => m.status === "upcoming")!;
  const fixtures = matches.filter((m) => m.status === "upcoming").slice(0, 4);
  const results = matches.filter((m) => m.status === "played").slice(0, 5);
  const latestNews = news.slice(0, 3);

  return (
    <>
      <Hero />

      <section className="relative overflow-hidden bg-navy text-white">
        <div className="absolute inset-0 opacity-30">
          <Image
            src={images.blueKit}
            alt=""
            fill
            className="object-cover"
            sizes="100vw"
          />
        </div>
        <div className="absolute inset-0 bg-gradient-to-r from-navy via-navy/92 to-navy/80" />
        <div className="container-page relative section-pad grid gap-10 lg:grid-cols-[1.1fr_0.9fr]">
          <motion.div
            initial={{ opacity: 0, x: -24 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true }}
          >
            <p className="section-label">{t.match.matchDay}</p>
            <h2 className="heading-lg mb-6">
              {club.name}{" "}
              <span className="text-red">{t.match.vs}</span> {featured.opponent}
            </h2>
            <div className="grid gap-3 text-sm text-white/75 sm:grid-cols-2">
              <p>
                <span className="block text-[11px] font-bold uppercase tracking-[0.16em] text-gold">
                  Date
                </span>
                {featured.date}
              </p>
              <p>
                <span className="block text-[11px] font-bold uppercase tracking-[0.16em] text-gold">
                  {t.match.kickOff}
                </span>
                {featured.time}
              </p>
              <p>
                <span className="block text-[11px] font-bold uppercase tracking-[0.16em] text-gold">
                  {t.match.stadium}
                </span>
                {featured.stadium}
              </p>
              <p>
                <span className="block text-[11px] font-bold uppercase tracking-[0.16em] text-gold">
                  {t.match.competition}
                </span>
                {featured.competition}
              </p>
            </div>
          </motion.div>
          <motion.div
            initial={{ opacity: 0, x: 24 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true }}
          >
            <p className="mb-4 text-center text-xs font-bold uppercase tracking-[0.2em] text-white/60">
              {t.match.kickOff}
            </p>
            <Countdown
              targetDate={nextMatchDate}
              labels={{
                days: t.match.days,
                hours: t.match.hours,
                minutes: t.match.minutes,
                seconds: t.match.seconds,
              }}
            />
          </motion.div>
        </div>
      </section>

      <section className="section-pad bg-[var(--bg)]">
        <div className="container-page">
          <div className="mb-8 flex items-end justify-between gap-4">
            <div>
              <p className="section-label">{t.news.title}</p>
              <h2 className="heading-lg">{t.news.title}</h2>
            </div>
            <Link href="/news" className="text-sm font-bold uppercase tracking-wider text-red">
              {t.common.viewAll}
            </Link>
          </div>
          <div className="grid gap-6 md:grid-cols-3">
            {latestNews.map((article, i) => (
              <motion.article
                key={article.id}
                initial={{ opacity: 0, y: 24 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: i * 0.08 }}
                className="group overflow-hidden border border-[var(--border)] bg-[var(--bg-alt)]"
              >
                <div className="relative aspect-[16/10] overflow-hidden">
                  <Image
                    src={article.image}
                    alt={article.title}
                    fill
                    className="object-cover transition duration-500 group-hover:scale-105"
                    sizes="(max-width:768px) 100vw, 33vw"
                  />
                </div>
                <div className="p-5">
                  <p className="mb-2 text-[11px] font-bold uppercase tracking-[0.16em] text-red">
                    {article.date}
                  </p>
                  <h3 className="mb-2 font-display text-2xl tracking-wide">
                    {article.title}
                  </h3>
                  <p className="mb-4 text-sm text-[var(--text-muted)]">
                    {article.summary}
                  </p>
                  <Link
                    href={`/news/${article.slug}`}
                    className="text-xs font-bold uppercase tracking-[0.14em] text-red"
                  >
                    {t.news.readMore} →
                  </Link>
                </div>
              </motion.article>
            ))}
          </div>
        </div>
      </section>

      <section className="section-pad bg-[var(--bg-alt)]">
        <div className="container-page grid gap-10 lg:grid-cols-2">
          <div>
            <p className="section-label">{t.match.nextFixtures}</p>
            <h2 className="heading-lg mb-6">{t.match.nextFixtures}</h2>
            <div className="space-y-3">
              {fixtures.map((m) => (
                <div
                  key={m.id}
                  className="flex flex-wrap items-center justify-between gap-3 border border-[var(--border)] bg-[var(--bg)] px-4 py-4"
                >
                  <div>
                    <p className="font-display text-xl tracking-wide">
                      {club.name} <span className="text-red">{t.match.vs}</span>{" "}
                      {m.opponent}
                    </p>
                    <p className="text-xs uppercase tracking-wider text-[var(--text-muted)]">
                      {m.competition} · {m.stadium}
                    </p>
                  </div>
                  <p className="font-data text-sm font-semibold">{m.date}</p>
                </div>
              ))}
            </div>
          </div>
          <div>
            <p className="section-label">{t.match.latestResults}</p>
            <h2 className="heading-lg mb-6">{t.match.latestResults}</h2>
            <div className="space-y-3">
              {results.map((m) => (
                <div
                  key={m.id}
                  className="flex items-center justify-between gap-3 border border-[var(--border)] bg-[var(--bg)] px-4 py-4"
                >
                  <p className="font-display text-lg tracking-wide">
                    {m.home ? club.name : m.opponent}
                  </p>
                  <p className="font-data text-lg font-bold text-red">
                    {m.scoreHome} - {m.scoreAway}
                  </p>
                  <p className="font-display text-lg tracking-wide">
                    {m.home ? m.opponent : club.name}
                  </p>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      <section className="section-pad bg-[var(--bg)]">
        <div className="container-page">
          <p className="section-label">{t.match.leagueTable}</p>
          <h2 className="heading-lg mb-6">{t.match.leagueTable}</h2>
          <div className="overflow-x-auto border border-[var(--border)]">
            <table className="min-w-full text-sm">
              <thead className="bg-navy text-white">
                <tr className="text-left text-[11px] uppercase tracking-[0.14em]">
                  <th className="px-3 py-3">{t.match.pos}</th>
                  <th className="px-3 py-3">Team</th>
                  <th className="px-3 py-3">{t.match.played}</th>
                  <th className="px-3 py-3">{t.match.won}</th>
                  <th className="px-3 py-3">{t.match.drawn}</th>
                  <th className="px-3 py-3">{t.match.lost}</th>
                  <th className="px-3 py-3">{t.match.gf}</th>
                  <th className="px-3 py-3">{t.match.ga}</th>
                  <th className="px-3 py-3">{t.match.gd}</th>
                  <th className="px-3 py-3">{t.match.pts}</th>
                </tr>
              </thead>
              <tbody>
                {leagueTable.map((row) => (
                  <tr
                    key={row.team}
                    className={`border-t border-[var(--border)] ${
                      row.team === "Viking FC"
                        ? "bg-red/10 font-bold"
                        : "bg-[var(--bg)]"
                    }`}
                  >
                    <td className="px-3 py-3">{row.pos}</td>
                    <td className="px-3 py-3">{row.team}</td>
                    <td className="px-3 py-3">{row.played}</td>
                    <td className="px-3 py-3">{row.won}</td>
                    <td className="px-3 py-3">{row.drawn}</td>
                    <td className="px-3 py-3">{row.lost}</td>
                    <td className="px-3 py-3">{row.gf}</td>
                    <td className="px-3 py-3">{row.ga}</td>
                    <td className="px-3 py-3">{row.gd}</td>
                    <td className="px-3 py-3 text-red">{row.pts}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </section>

      <section className="section-pad bg-navy text-white">
        <div className="container-page text-center">
          <p className="section-label">{t.sponsors.title}</p>
          <h2 className="heading-lg mb-10">{t.sponsors.title}</h2>
          <div className="flex flex-wrap items-center justify-center gap-4 md:gap-6">
            {sponsors.map((s) => (
              <div
                key={s.name}
                className="flex min-w-[140px] items-center justify-center border border-white/15 bg-white/5 px-6 py-5"
              >
                <span className="text-xs font-bold uppercase tracking-[0.16em] text-white/80">
                  {s.name}
                </span>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="relative overflow-hidden section-pad">
        <div className="absolute inset-0">
          <Image
            src={images.squad}
            alt=""
            fill
            className="object-cover brightness-[0.35]"
            sizes="100vw"
          />
        </div>
        <div className="container-page relative max-w-2xl text-center text-white">
          <p className="section-label">{t.newsletter.title}</p>
          <h2 className="heading-lg mb-3">{t.newsletter.title}</h2>
          <p className="mb-6 text-white/75">{t.newsletter.subtitle}</p>
          <form
            onSubmit={(e) => e.preventDefault()}
            className="flex flex-col gap-3 sm:flex-row"
          >
            <input
              type="email"
              required
              placeholder={t.newsletter.placeholder}
              className="flex-1 bg-white/10 px-4 py-3 text-sm outline-none ring-1 ring-white/20 placeholder:text-white/50 focus:ring-red"
            />
            <button type="submit" className="btn-primary">
              {t.newsletter.subscribe}
            </button>
          </form>
        </div>
      </section>
    </>
  );
}
