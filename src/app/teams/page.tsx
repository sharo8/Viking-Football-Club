"use client";

import Link from "next/link";
import Image from "next/image";
import { images } from "@/lib/data";
import { useLocale } from "@/components/providers/LocaleProvider";
import { PageHero } from "@/components/ui/PageHero";

export default function TeamsPage() {
  const { t } = useLocale();

  const teams = [
    {
      title: t.teams.firstTeam,
      image: images.portrait,
      href: "/players",
      desc: "The senior squad competing in the Premier League and Cup.",
    },
    {
      title: t.teams.academy,
      image: images.stretch,
      href: "/about",
      desc: "Homegrown talent developed with professional standards.",
    },
    {
      title: t.teams.youth,
      image: images.jog,
      href: "/services",
      desc: "Youth categories building the future of Viking FC.",
    },
  ];

  return (
    <>
      <PageHero
        title={t.teams.title}
        subtitle={`${t.hero.slogan1} ${t.hero.slogan2} ${t.hero.slogan3}`}
        image={images.squad}
      />
      <section className="section-pad bg-[var(--bg)]">
        <div className="container-page grid gap-6 md:grid-cols-3">
          {teams.map((team) => (
            <Link
              key={team.title}
              href={team.href}
              className="group overflow-hidden border border-[var(--border)] bg-[var(--bg-alt)] transition hover:border-red"
            >
              <div className="relative aspect-[4/5] overflow-hidden">
                <Image
                  src={team.image}
                  alt={team.title}
                  fill
                  className="object-cover transition duration-500 group-hover:scale-105"
                  sizes="(max-width:768px) 100vw, 33vw"
                />
                <div className="absolute inset-0 bg-gradient-to-t from-navy-deep via-transparent to-transparent" />
                <div className="absolute inset-x-0 bottom-0 p-5 text-white">
                  <h2 className="font-display text-3xl tracking-wide">
                    {team.title}
                  </h2>
                  <p className="mt-2 text-sm text-white/75">{team.desc}</p>
                </div>
              </div>
            </Link>
          ))}
        </div>
      </section>
    </>
  );
}
