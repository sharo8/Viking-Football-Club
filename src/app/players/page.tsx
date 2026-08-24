"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import Image from "next/image";
import { players, images, type PlayerPosition } from "@/lib/data";
import { useLocale } from "@/components/providers/LocaleProvider";
import { PageHero } from "@/components/ui/PageHero";

type Filter = "all" | PlayerPosition;

export default function PlayersPage() {
  const { t } = useLocale();
  const [filter, setFilter] = useState<Filter>("all");

  const filters: { key: Filter; label: string }[] = [
    { key: "all", label: t.players.filters.all },
    { key: "Goalkeeper", label: t.players.filters.goalkeepers },
    { key: "Defender", label: t.players.filters.defenders },
    { key: "Midfielder", label: t.players.filters.midfielders },
    { key: "Forward", label: t.players.filters.forwards },
  ];

  const list = useMemo(
    () =>
      filter === "all"
        ? players
        : players.filter((p) => p.position === filter),
    [filter],
  );

  return (
    <>
      <PageHero
        title={t.players.title}
        subtitle="Meet the Viking squad"
        image={images.blueKit}
      />
      <section className="section-pad bg-[var(--bg)]">
        <div className="container-page">
          <div className="mb-8 flex flex-wrap gap-2">
            {filters.map((f) => (
              <button
                key={f.key}
                type="button"
                onClick={() => setFilter(f.key)}
                className={`px-4 py-2 text-xs font-bold uppercase tracking-[0.14em] transition ${
                  filter === f.key
                    ? "bg-red text-white"
                    : "border border-[var(--border)] text-[var(--text-muted)] hover:border-red hover:text-red"
                }`}
              >
                {f.label}
              </button>
            ))}
          </div>

          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {list.map((player) => (
              <Link
                key={player.id}
                href={`/players/${player.slug}`}
                className="group relative overflow-hidden border border-[var(--border)] bg-navy text-white"
              >
                <div className="relative aspect-[3/4]">
                  <Image
                    src={player.image}
                    alt={player.name}
                    fill
                    className="object-cover opacity-80 transition duration-500 group-hover:scale-105 group-hover:opacity-100"
                    sizes="(max-width:640px) 100vw, 25vw"
                  />
                  <div className="absolute inset-0 bg-gradient-to-t from-navy-deep via-navy/20 to-transparent" />
                  <p className="absolute right-3 top-3 font-display text-5xl text-white/25">
                    {player.number}
                  </p>
                  <div className="absolute inset-x-0 bottom-0 p-4">
                    <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-red">
                      {player.position}
                    </p>
                    <h3 className="font-display text-2xl tracking-wide">
                      {player.name}
                    </h3>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        </div>
      </section>
    </>
  );
}
