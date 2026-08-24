"use client";

import { useState } from "react";
import { matches, club, images } from "@/lib/data";
import { useLocale } from "@/components/providers/LocaleProvider";
import { PageHero } from "@/components/ui/PageHero";

export default function MatchesPage() {
  const { t } = useLocale();
  const [tab, setTab] = useState<"upcoming" | "results">("upcoming");

  const upcoming = matches.filter((m) => m.status === "upcoming");
  const results = matches.filter((m) => m.status === "played");
  const list = tab === "upcoming" ? upcoming : results;

  return (
    <>
      <PageHero
        title={t.matchesPage.title}
        subtitle={t.matchesPage.calendar}
        image={images.celebrate}
      />
      <section className="section-pad bg-[var(--bg)]">
        <div className="container-page">
          <div className="mb-8 flex gap-2">
            <button
              type="button"
              onClick={() => setTab("upcoming")}
              className={`px-5 py-2.5 text-xs font-bold uppercase tracking-[0.14em] ${
                tab === "upcoming"
                  ? "bg-red text-white"
                  : "border border-[var(--border)]"
              }`}
            >
              {t.match.upcoming}
            </button>
            <button
              type="button"
              onClick={() => setTab("results")}
              className={`px-5 py-2.5 text-xs font-bold uppercase tracking-[0.14em] ${
                tab === "results"
                  ? "bg-red text-white"
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
                className="grid items-center gap-4 border border-[var(--border)] bg-[var(--bg-alt)] px-5 py-5 md:grid-cols-[1fr_auto_1fr_auto]"
              >
                <div className="text-left md:text-right">
                  <p className="font-display text-2xl tracking-wide">
                    {m.home ? club.name : m.opponent}
                  </p>
                </div>
                <div className="text-center">
                  {m.status === "played" ? (
                    <p className="font-data text-2xl font-bold text-red">
                      {m.scoreHome} - {m.scoreAway}
                    </p>
                  ) : (
                    <p className="font-display text-xl text-red">{t.match.vs}</p>
                  )}
                  <p className="mt-1 text-[11px] uppercase tracking-wider text-[var(--text-muted)]">
                    {m.time}
                  </p>
                </div>
                <div>
                  <p className="font-display text-2xl tracking-wide">
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
