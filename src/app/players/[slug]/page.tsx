"use client";

import Image from "next/image";
import Link from "next/link";
import { useParams } from "next/navigation";
import { players, images } from "@/lib/data";
import { useLocale } from "@/components/providers/LocaleProvider";

export default function PlayerDetailPage() {
  const params = useParams<{ slug: string }>();
  const { t } = useLocale();
  const player = players.find((p) => p.slug === params.slug);

  if (!player) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-4 pt-24">
        <h1 className="heading-lg">Player not found</h1>
        <Link href="/players" className="btn-primary">
          {t.common.back}
        </Link>
      </div>
    );
  }

  const details = [
    { label: t.players.number, value: player.number },
    { label: t.players.position, value: player.position },
    { label: t.players.nationality, value: player.nationality },
    { label: t.players.age, value: player.age },
    { label: t.players.height, value: player.height },
    { label: t.players.weight, value: player.weight },
    { label: t.players.dob, value: player.dob },
  ];

  const stats = [
    { label: t.players.appearances, value: player.stats.appearances },
    { label: t.players.goals, value: player.stats.goals },
    { label: t.players.assists, value: player.stats.assists },
    ...(player.stats.cleanSheets != null
      ? [{ label: t.players.cleanSheets, value: player.stats.cleanSheets }]
      : []),
  ];

  return (
    <div className="bg-[var(--bg)] pt-20">
      <section className="relative overflow-hidden bg-navy text-white">
        <div className="absolute inset-0 opacity-30">
          <Image src={player.image} alt="" fill className="object-cover" />
        </div>
        <div className="absolute inset-0 bg-gradient-to-r from-navy-deep via-navy/90 to-navy/70" />
        <div className="container-page relative grid gap-8 py-16 lg:grid-cols-[320px_1fr]">
          <div className="relative mx-auto aspect-[3/4] w-full max-w-xs overflow-hidden border border-white/20">
            <Image
              src={player.image}
              alt={player.name}
              fill
              className="object-cover"
              sizes="320px"
              priority
            />
          </div>
          <div className="flex flex-col justify-center">
            <p className="font-display text-7xl text-red/80 md:text-8xl">
              {player.number}
            </p>
            <h1 className="heading-lg">{player.name}</h1>
            <p className="mt-2 text-sm font-bold uppercase tracking-[0.18em] text-gold">
              {player.position} · {player.nationality}
            </p>
            <p className="mt-5 max-w-xl text-white/75">{player.bio}</p>
            <Link
              href="/players"
              className="mt-6 inline-flex text-xs font-bold uppercase tracking-[0.14em] text-red"
            >
              ← {t.common.back}
            </Link>
          </div>
        </div>
      </section>

      <section className="section-pad">
        <div className="container-page grid gap-10 lg:grid-cols-2">
          <div>
            <h2 className="heading-lg mb-6">Profile</h2>
            <dl className="grid grid-cols-2 gap-3">
              {details.map((d) => (
                <div
                  key={d.label}
                  className="border border-[var(--border)] bg-[var(--bg-alt)] px-4 py-3"
                >
                  <dt className="text-[11px] font-bold uppercase tracking-[0.14em] text-[var(--text-muted)]">
                    {d.label}
                  </dt>
                  <dd className="mt-1 font-display text-2xl tracking-wide">
                    {d.value}
                  </dd>
                </div>
              ))}
            </dl>
          </div>
          <div>
            <h2 className="heading-lg mb-6">{t.players.stats}</h2>
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 lg:grid-cols-2 xl:grid-cols-4">
              {stats.map((s) => (
                <div
                  key={s.label}
                  className="border border-[var(--border)] bg-navy px-4 py-5 text-center text-white"
                >
                  <p className="font-display text-4xl text-red">{s.value}</p>
                  <p className="mt-2 text-[11px] font-bold uppercase tracking-[0.14em] text-white/60">
                    {s.label}
                  </p>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      <section className="section-pad bg-[var(--bg-alt)]">
        <div className="container-page">
          <h2 className="heading-lg mb-6">{t.players.gallery}</h2>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {[player.image, images.pitch, images.run].map((img, i) => (
              <div key={i} className="relative aspect-[16/11] overflow-hidden">
                <Image
                  src={img}
                  alt={`${player.name} gallery ${i + 1}`}
                  fill
                  className="object-cover"
                  sizes="(max-width:768px) 100vw, 33vw"
                />
              </div>
            ))}
          </div>
        </div>
      </section>
    </div>
  );
}
