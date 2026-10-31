"use client";

import Link from "next/link";
import Image from "next/image";
import { CalendarDays, MapPin, Trophy, CheckCircle2, Clock } from "lucide-react";
import {
  matches,
  news,
  sponsors,
  nextMatchDate,
  club,
  images,
  players,
  leagueTable,
  mercatoPlaceholders,
} from "@/lib/data";
import { useLocale } from "@/components/providers/LocaleProvider";
import { localize } from "@/lib/localize";
import { Countdown } from "@/components/home/Countdown";
import { Hero } from "@/components/home/Hero";
import { Reveal } from "@/components/ui/Reveal";
import { CountUp } from "@/components/ui/CountUp";
import { MatchDayTitle } from "@/components/ui/MatchDayTitle";

function MatchMetaRow({
  competition,
  stadium,
  date,
  time,
}: {
  competition: string;
  stadium: string;
  date: string;
  time: string;
}) {
  return (
    <div className="match-card-meta">
      <span className="match-card-meta-item">
        <Trophy size={11} className="text-[var(--bronze)]" aria-hidden />
        <span>{competition}</span>
      </span>
      <span className="match-card-meta-sep" aria-hidden>
        ·
      </span>
      <span className="match-card-meta-item">
        <MapPin size={11} className="text-[var(--bronze)]" aria-hidden />
        <span>{stadium}</span>
      </span>
      <span className="match-card-meta-sep" aria-hidden>
        ·
      </span>
      <span className="match-card-meta-item">
        <Clock size={11} className="text-[var(--bronze)]" aria-hidden />
        <span>
          {date} · {time}
        </span>
      </span>
    </div>
  );
}

export function HomePage() {
  const { t, locale } = useLocale();
  const featured = matches.find((m) => m.status === "upcoming")!;
  const fixtures = matches.filter((m) => m.status === "upcoming").slice(0, 3);
  const results = matches.filter((m) => m.status === "played").slice(0, 3);
  const latestNews = news.slice(0, 6);
  const vikingRow = leagueTable.find((r) => r.team === "Viking FC");

  const statusLabel = {
    official: t.mercato.statusOfficial,
    signing: t.mercato.statusSigning,
    extension: t.mercato.statusExtension,
    trial: t.mercato.statusTrial,
  } as const;

  return (
    <>
      <Hero />

      <section
        id="match-day"
        className="relative overflow-x-hidden overflow-y-hidden bg-navy text-white"
      >
        <div className="absolute inset-0 opacity-25">
          <Image
            src={images.blueKit}
            alt=""
            fill
            className="object-cover"
            sizes="100vw"
          />
        </div>
        <div className="absolute inset-0 bg-gradient-to-r from-navy via-navy/90 to-navy/75" />
        <div className="container-page relative section-pad grid items-center gap-12 lg:grid-cols-[1.05fr_0.95fr] lg:gap-16 lg:items-stretch">
          <Reveal variant="left" className="flex h-full flex-col justify-center text-left">
            <p className="section-label justify-start text-[#c9a06a]">
              {t.match.matchDay}
            </p>
            <MatchDayTitle
              left={featured.home ? club.name : featured.opponent}
              vs={t.match.vs}
              right={featured.home ? featured.opponent : club.name}
              className="!justify-start !text-left"
            />
            <div className="match-meta">
              <div className="match-meta-item">
                <span className="match-meta-label">{t.match.date}</span>
                <span className="match-meta-value">{featured.date}</span>
              </div>
              <div className="match-meta-item">
                <span className="match-meta-label">{t.match.kickOff}</span>
                <span className="match-meta-value">{featured.time}</span>
              </div>
              <div className="match-meta-item">
                <span className="match-meta-label">{t.match.stadium}</span>
                <span className="match-meta-value">{featured.stadium}</span>
              </div>
              <div className="match-meta-item">
                <span className="match-meta-label">{t.match.competition}</span>
                <span className="match-meta-value">{featured.competition}</span>
              </div>
            </div>
          </Reveal>
          <Reveal variant="right" delay={140} className="flex h-full flex-col justify-center">
            <p className="mb-5 text-center font-data text-[0.68rem] font-semibold uppercase tracking-[0.2em] text-white/55">
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
            {vikingRow && (
              <div className="mt-6 grid grid-cols-3 gap-2.5 text-center sm:mt-8 sm:gap-3">
                <div className="stat-cell">
                  <p className="font-display text-2xl text-white sm:text-3xl">
                    <CountUp end={vikingRow.pos} />
                  </p>
                  <p className="mt-1 font-data text-[9px] uppercase tracking-[0.14em] text-white/55">
                    {t.match.pos}
                  </p>
                </div>
                <div className="stat-cell">
                  <p className="font-display text-2xl text-white sm:text-3xl">
                    <CountUp end={vikingRow.pts} />
                  </p>
                  <p className="mt-1 font-data text-[9px] uppercase tracking-[0.14em] text-white/55">
                    {t.match.pts}
                  </p>
                </div>
                <div className="stat-cell">
                  <p className="font-display text-2xl text-white sm:text-3xl">
                    <CountUp end={players.length} />
                  </p>
                  <p className="mt-1 font-data text-[9px] uppercase tracking-[0.14em] text-white/55">
                    {t.match.squad}
                  </p>
                </div>
              </div>
            )}
          </Reveal>
        </div>
      </section>

      <section id="latest-news" className="section-pad bg-[var(--bg)]">
        <div className="container-page">
          <Reveal variant="blur" className="section-head-center mb-9">
            <p className="section-label">{t.news.eyebrow}</p>
            <h2 className="heading-md">{t.news.title}</h2>
            <Link
              href="/news"
              className="mt-3 inline-block font-data text-xs font-semibold uppercase tracking-[0.14em] text-[var(--accent)]"
            >
              {t.common.viewAll}
            </Link>
          </Reveal>
          <div className="grid gap-5 md:grid-cols-3">
            {latestNews.map((article, i) => (
              <Reveal key={article.id} variant="up" delay={i * 110}>
                <article className="card-premium group h-full overflow-hidden">
                  <div className="relative aspect-[16/10] overflow-hidden">
                    <Image
                      src={article.image}
                      alt={localize(article.title, locale)}
                      fill
                      className="object-cover transition duration-500 group-hover:scale-105"
                      sizes="(max-width:768px) 100vw, 33vw"
                    />
                  </div>
                  <div className="p-4 sm:p-5">
                    <div className="mb-2 flex flex-wrap items-center gap-2">
                      <span className="badge-pill border border-[var(--accent)]/25 bg-[var(--accent)]/10 text-[var(--accent)]">
                        {t.news.categories[article.category]}
                      </span>
                      {article.date && (
                        <span className="font-data text-[10px] uppercase tracking-[0.12em] text-[var(--text-muted)]">
                          {article.date}
                        </span>
                      )}
                    </div>
                    <h3 className="mb-2 font-display text-lg font-bold tracking-wide sm:text-xl">
                      {localize(article.title, locale)}
                    </h3>
                    <p className="mb-3 line-clamp-3 text-sm text-[var(--text-muted)]">
                      {localize(article.summary, locale)}
                    </p>
                    <Link
                      href={`/news/${article.slug}`}
                      className="font-data text-xs font-semibold uppercase tracking-[0.14em] text-[var(--accent)]"
                    >
                      {t.news.readMore} →
                    </Link>
                  </div>
                </article>
              </Reveal>
            ))}
          </div>
        </div>
      </section>

      <section id="fixtures-results" className="section-pad bg-[var(--bg-alt)]">
        <div className="container-page grid gap-10 lg:grid-cols-2 lg:gap-12">
          <div>
            <Reveal variant="left" className="section-head-center mb-6">
              <p className="section-label">{t.match.fixturesEyebrow}</p>
              <h2 className="heading-md">{t.match.nextFixtures}</h2>
            </Reveal>
            <div className="space-y-3">
              {fixtures.map((m, i) => (
                <Reveal key={m.id} variant="left" delay={i * 95}>
                  <article className="card-match relative overflow-hidden border-l-[3px] border-l-[var(--accent)] px-4 py-3.5 sm:px-5">
                    <div className="mb-2 flex flex-wrap items-center gap-2">
                      <span className="badge-pill border border-[var(--accent)]/25 bg-[var(--accent)]/10 text-[var(--accent)]">
                        <CalendarDays size={10} />
                        {t.match.upcomingBadge}
                      </span>
                      {m.matchday && (
                        <span className="font-data text-[9px] uppercase tracking-[0.12em] text-[var(--bronze)]">
                          {m.matchday}
                        </span>
                      )}
                    </div>
                    <h3 className="heading-sm tracking-wide">
                      {club.name}{" "}
                      <span className="text-[var(--accent)]">{t.match.vs}</span>{" "}
                      {m.opponent}
                    </h3>
                    <MatchMetaRow
                      competition={m.competition}
                      stadium={m.stadium}
                      date={m.date}
                      time={m.time}
                    />
                  </article>
                </Reveal>
              ))}
            </div>
            <div className="mt-5 text-center">
              <Link
                href="/matches?tab=upcoming"
                className="btn-outline inline-flex !min-h-[2.6rem] !rounded-md !px-5 !py-0 !text-[0.7rem]"
              >
                {t.common.viewAllFixtures}
              </Link>
            </div>
          </div>

          <div>
            <Reveal variant="right" className="section-head-center mb-6">
              <p className="section-label">{t.match.resultsEyebrow}</p>
              <h2 className="heading-md">{t.match.latestResults}</h2>
            </Reveal>
            <div className="space-y-3">
              {results.map((m, i) => (
                <Reveal key={m.id} variant="right" delay={i * 85}>
                  <article className="card-match relative overflow-hidden border-l-[3px] border-l-[var(--bronze)] px-4 py-3.5 sm:px-5">
                    <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
                      <span className="badge-pill border border-[var(--bronze)]/30 bg-[var(--bronze)]/10 text-[var(--bronze)]">
                        <CheckCircle2 size={10} />
                        {t.match.winBadge}
                      </span>
                      <span className="font-data text-[10px] text-[var(--text-muted)]">
                        {m.date}
                      </span>
                    </div>
                    <div className="grid grid-cols-[1fr_auto_1fr] items-center gap-2 sm:gap-3">
                      <p className="text-right font-display text-sm font-bold tracking-wide sm:text-base">
                        {m.home ? club.name : m.opponent}
                      </p>
                      <p className="min-w-[3.75rem] rounded bg-[var(--accent)] px-2.5 py-1.5 text-center font-data text-sm font-bold text-white shadow-[0_6px_16px_-8px_rgba(138,37,48,0.55)] sm:min-w-[4.25rem] sm:text-base">
                        {m.scoreHome} - {m.scoreAway}
                      </p>
                      <p className="font-display text-sm font-bold tracking-wide sm:text-base">
                        {m.home ? m.opponent : club.name}
                      </p>
                    </div>
                    <MatchMetaRow
                      competition={m.competition}
                      stadium={m.stadium}
                      date={m.date}
                      time={m.time}
                    />
                  </article>
                </Reveal>
              ))}
            </div>
            <div className="mt-5 text-center">
              <Link
                href="/matches?tab=results"
                className="btn-outline inline-flex !min-h-[2.6rem] !rounded-md !px-5 !py-0 !text-[0.7rem]"
              >
                {t.common.viewAllResults}
              </Link>
            </div>
          </div>
        </div>
      </section>

      <section id="mercato" className="section-pad bg-[var(--bg)]">
        <div className="container-page">
          <Reveal variant="blur" className="section-head-center mb-9">
            <p className="section-label">{t.mercato.eyebrow}</p>
            <h2 className="heading-md">{t.mercato.title}</h2>
          </Reveal>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-5">
            {mercatoPlaceholders.map((item, i) => (
              <Reveal key={item.id} variant="up" delay={i * 70}>
                <article className="mercato-card group">
                  <div className="mercato-card-photo">
                    <span className="mercato-card-initials">{item.initials}</span>
                    <div className="mercato-card-overlay" />
                  </div>
                  <div className="p-4">
                    <span
                      className={`badge-pill mb-2 border ${
                        item.status === "signing"
                          ? "border-[var(--accent)]/25 bg-[var(--accent)]/10 text-[var(--accent)]"
                          : item.status === "extension"
                            ? "border-[var(--bronze)]/30 bg-[var(--bronze)]/10 text-[var(--bronze)]"
                            : "border-[var(--border)] bg-[var(--bg-alt)] text-[var(--text-muted)]"
                      }`}
                    >
                      {statusLabel[item.status]}
                    </span>
                    <h3 className="mt-2 font-display text-lg font-bold tracking-wide">
                      {item.name}
                    </h3>
                    <p className="mt-1 font-data text-[10px] uppercase tracking-[0.14em] text-[var(--bronze)]">
                      {item.role}
                    </p>
                    <p className="mt-2 text-xs leading-relaxed text-[var(--text-muted)]">
                      {item.detail}
                    </p>
                  </div>
                </article>
              </Reveal>
            ))}
          </div>
        </div>
      </section>

      <section id="partners" className="section-pad bg-navy text-white">
        <div className="container-page text-center">
          <Reveal variant="blur" className="mb-8">
            <p className="section-label justify-center text-[#c9a06a]">
              {t.sponsors.title}
            </p>
            <h2 className="heading-md">{t.sponsors.title}</h2>
          </Reveal>
          <div className="mx-auto flex max-w-xl flex-col items-center justify-center gap-4 sm:flex-row sm:gap-5">
            {sponsors.map((s, i) => (
              <Reveal key={s.name} variant="scale" delay={i * 110} className="w-full sm:w-1/2">
                <div className="partner-chip mx-auto">
                  <span>{s.name}</span>
                </div>
              </Reveal>
            ))}
          </div>
        </div>
      </section>

      {vikingRow && (
        <section id="club-highlights" className="section-pad bg-[var(--bg-alt)]">
          <div className="container-page">
            <Reveal variant="blur" className="section-head-center mb-10">
              <p className="section-label">{t.highlights.eyebrow}</p>
              <h2 className="heading-md">{t.highlights.title}</h2>
            </Reveal>
            <div className="grid grid-cols-2 gap-4 md:grid-cols-4 md:gap-5">
              {[
                { end: vikingRow.pos, label: t.highlights.position },
                { end: vikingRow.pts, label: t.highlights.points },
                { end: vikingRow.won, label: t.highlights.wins },
                { end: vikingRow.played, label: t.highlights.unbeaten },
              ].map((stat, i) => (
                <Reveal key={stat.label} variant="up" delay={i * 100}>
                  <div className="highlight-stat">
                    <p className="highlight-stat-value">
                      <CountUp end={stat.end} />
                    </p>
                    <p className="mt-2 font-data text-[10px] font-semibold uppercase tracking-[0.16em] text-[var(--text-muted)]">
                      {stat.label}
                    </p>
                  </div>
                </Reveal>
              ))}
            </div>
          </div>
        </section>
      )}

      <section id="newsletter" className="relative overflow-hidden section-pad">
        <div className="absolute inset-0">
          <Image
            src={images.squad}
            alt=""
            fill
            className="object-cover brightness-[0.4]"
            sizes="100vw"
          />
        </div>
        <div className="pointer-events-none absolute inset-x-0 bottom-0 h-1/2 bg-gradient-to-t from-black/60 to-transparent" />
        <Reveal
          variant="up"
          className="container-page relative mx-auto max-w-2xl text-center text-white"
        >
          <p className="section-label justify-center text-[#c9a06a]">
            {t.newsletter.title}
          </p>
          <h2 className="heading-md mb-3">{t.newsletter.title}</h2>
          <p className="mb-6 text-sm text-white/80 md:text-base">
            {t.newsletter.subtitle}
          </p>
          <form
            onSubmit={(e) => e.preventDefault()}
            className="mx-auto flex max-w-xl flex-col gap-3 sm:flex-row sm:items-stretch"
          >
            <input
              type="email"
              required
              placeholder={t.newsletter.placeholder}
              className="min-h-[2.85rem] flex-1 rounded-md bg-white/10 px-4 text-sm outline-none ring-1 ring-white/20 placeholder:text-white/50 focus:ring-[var(--accent)]"
            />
            <button
              type="submit"
              className="btn-primary !min-h-[2.85rem] !rounded-md !py-0"
            >
              {t.newsletter.subscribe}
            </button>
          </form>
        </Reveal>
      </section>
    </>
  );
}
