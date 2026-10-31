"use client";

import { useEffect, useState, Suspense } from "react";
import { useSearchParams } from "next/navigation";
import { matches, club, images } from "@/lib/data";
import { useLocale } from "@/components/providers/LocaleProvider";
import { PageHero } from "@/components/ui/PageHero";
import { MatchDayTitle } from "@/components/ui/MatchDayTitle";
import { PublishedLineup } from "@/components/matches/PublishedLineup";

function MatchesContent() {
  const { t } = useLocale();
  const searchParams = useSearchParams();
  const initial =
    searchParams.get("tab") === "results" ? "results" : "upcoming";
  const [tab, setTab] = useState<"upcoming" | "results">(initial);

  useEffect(() => {
    setTab(searchParams.get("tab") === "results" ? "results" : "upcoming");
  }, [searchParams]);

  const upcoming = matches.filter((m) => m.status === "upcoming");
  const results = matches.filter((m) => m.status === "played");
  const list = tab === "upcoming" ? upcoming : results;
  const featured = upcoming[0] ?? results[0];

  return (
    <>
      <PageHero
        title={t.matchesPage.title}
        subtitle={t.matchesPage.calendar}
        image={images.celebrate}
      />

      {featured && (
        <section className="overflow-x-hidden bg-navy text-white">
          <div className="container-page section-pad !py-12 md:!py-16">
            <p className="section-label text-[#c9a06a]">{t.match.matchDay}</p>
            <MatchDayTitle
              left={featured.home ? club.name : featured.opponent}
              vs={t.match.vs}
              right={featured.home ? featured.opponent : club.name}
            />
            <p className="font-data text-xs uppercase tracking-[0.16em] text-white/60">
              {featured.date} · {featured.time} · {featured.stadium} ·{" "}
              {featured.competition}
              {featured.matchday ? ` · ${featured.matchday}` : ""}
            </p>
          </div>
        </section>
      )}

      <PublishedLineup />

      <section className="section-pad bg-[var(--bg)]">
        <div className="container-page">
          <div className="mb-8 flex gap-2">
            <button
              type="button"
              onClick={() => setTab("upcoming")}
              className={`px-5 py-2.5 font-data text-xs font-semibold uppercase tracking-[0.14em] ${
                tab === "upcoming"
                  ? "bg-[var(--accent)] text-[var(--accent-ink)]"
                  : "border border-[var(--border)]"
              }`}
            >
              {t.match.upcoming}
            </button>
            <button
              type="button"
              onClick={() => setTab("results")}
              className={`px-5 py-2.5 font-data text-xs font-semibold uppercase tracking-[0.14em] ${
                tab === "results"
                  ? "bg-[var(--accent)] text-[var(--accent-ink)]"
                  : "border border-[var(--border)]"
              }`}
            >
              {t.match.results}
            </button>
          </div>

          <div className="space-y-3">
            {list.map((m) => (
              <article
                key={m.id}
                className="card-surface grid items-center gap-4 px-5 py-5 md:grid-cols-[1fr_auto_1fr_auto]"
              >
                <div className="text-left md:text-right">
                  <p className="font-display text-2xl font-bold tracking-wide">
                    {m.home ? club.name : m.opponent}
                  </p>
                </div>
                <div className="text-center">
                  {m.status === "played" ? (
                    <p className="font-data text-2xl font-bold text-[var(--accent)]">
                      {m.scoreHome} - {m.scoreAway}
                    </p>
                  ) : (
                    <p className="font-display text-xl text-[var(--accent)]">
                      {t.match.vs}
                    </p>
                  )}
                  <p className="mt-1 font-data text-[11px] uppercase tracking-wider text-[var(--text-muted)]">
                    {m.time}
                  </p>
                </div>
                <div>
                  <p className="font-display text-2xl font-bold tracking-wide">
                    {m.home ? m.opponent : club.name}
                  </p>
                </div>
                <div className="text-sm text-[var(--text-muted)] md:text-right">
                  <p className="font-semibold text-[var(--text)]">{m.date}</p>
                  <p>{m.competition}</p>
                  <p>{m.stadium}</p>
                </div>
              </article>
            ))}
          </div>
        </div>
      </section>
    </>
  );
}

export default function MatchesPage() {
  return (
    <Suspense fallback={null}>
      <MatchesContent />
    </Suspense>
  );
}
