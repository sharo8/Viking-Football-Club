"use client";

import Image from "next/image";
import { images } from "@/lib/data";
import { useLocale } from "@/components/providers/LocaleProvider";
import { PageHero } from "@/components/ui/PageHero";
import { Reveal } from "@/components/ui/Reveal";
import { CountUp } from "@/components/ui/CountUp";

const sections = [
  {
    key: "history" as const,
    image: images.squad,
    text: "Founded with passion and ambition, Viking FC has grown into a professional club built on discipline, unity and community pride.",
  },
  {
    key: "vision" as const,
    image: images.celebrate,
    text: "To become a benchmark African club with European standards — on the pitch, in the academy and in fan experience.",
  },
  {
    key: "mission" as const,
    image: images.pitch,
    text: "Develop elite players, compete for trophies and inspire the next generation through football excellence.",
  },
  {
    key: "values" as const,
    image: images.jog,
    text: "Courage. Family. Discipline. Ambition. Respect. These values guide every training session and every matchday.",
  },
];

export default function AboutPage() {
  const { t } = useLocale();

  return (
    <>
      <PageHero
        title={t.about.title}
        subtitle={`${t.hero.slogan1} ${t.hero.slogan2} ${t.hero.slogan3}`}
        image={images.portrait}
      />

      <section className="section-pad bg-[var(--bg)]">
        <div className="container-page space-y-16">
          {sections.map((s, i) => (
            <div
              key={s.key}
              className={`grid items-center gap-8 lg:grid-cols-2 ${
                i % 2 === 1 ? "lg:[&>*:first-child]:order-2" : ""
              }`}
            >
              <Reveal variant={i % 2 === 0 ? "left" : "right"}>
                <div className="relative aspect-[16/11] overflow-hidden">
                  <Image
                    src={s.image}
                    alt={t.about[s.key]}
                    fill
                    className="object-cover"
                    sizes="(max-width:1024px) 100vw, 50vw"
                  />
                </div>
              </Reveal>
              <Reveal variant={i % 2 === 0 ? "right" : "left"} delay={100}>
                <div>
                  <p className="section-label">{t.about[s.key]}</p>
                  <h2 className="heading-lg mb-4">{t.about[s.key]}</h2>
                  <p className="text-[var(--text-muted)] leading-relaxed">
                    {s.text}
                  </p>
                </div>
              </Reveal>
            </div>
          ))}
        </div>
      </section>

      <section className="section-pad bg-[var(--bg-alt)]">
        <div className="container-page grid gap-8 md:grid-cols-2">
          <Reveal variant="up">
            <blockquote className="card-surface border-l-4 border-[var(--accent)] p-8">
              <p className="section-label">{t.about.president}</p>
              <p className="mb-4 text-lg leading-relaxed">
                &ldquo;Viking FC is more than a football club. It is a family
                project built to last — for our city, our players and our
                supporters.&rdquo;
              </p>
              <footer className="font-display text-xl tracking-wider text-[var(--accent)]">
                Jean Claude Mugabo — President
              </footer>
            </blockquote>
          </Reveal>
          <Reveal variant="up" delay={120}>
            <blockquote className="card-surface border-l-4 border-[var(--bronze)] p-8">
              <p className="section-label">{t.about.manager}</p>
              <p className="mb-4 text-lg leading-relaxed">
                &ldquo;We play with intensity and identity. Every session, every
                match — we chase the Viking dream together.&rdquo;
              </p>
              <footer className="font-display text-xl tracking-wider text-[var(--accent)]">
                Coach Michel Nzeyimana — Manager
              </footer>
            </blockquote>
          </Reveal>
        </div>
      </section>

      <section className="section-pad bg-[var(--bg)]">
        <div className="container-page grid gap-10 lg:grid-cols-2">
          <Reveal variant="left">
            <div>
              <p className="section-label">{t.about.stadium}</p>
              <h2 className="heading-lg mb-4">{t.about.stadium}</h2>
              <p className="mb-4 text-[var(--text-muted)] leading-relaxed">
                Viking Stadium is the fortress of our supporters — a modern venue
                designed for elite football nights and unforgettable atmospheres.
              </p>
              <ul className="space-y-2 text-sm text-[var(--text-muted)]">
                <li>
                  • Capacity:{" "}
                  <CountUp end={8500} className="font-data font-semibold text-[var(--accent)]" />
                </li>
                <li>• Location: Kigali, Rwanda</li>
                <li>• Facilities: VIP lounge, academy pitch, media center</li>
              </ul>
            </div>
          </Reveal>
          <Reveal variant="right" delay={100}>
            <div className="relative aspect-[16/11] overflow-hidden">
              <Image
                src={images.blueKit}
                alt="Stadium"
                fill
                className="object-cover"
                sizes="(max-width:1024px) 100vw, 50vw"
              />
            </div>
          </Reveal>
        </div>
      </section>

      <section className="relative section-pad text-white">
        <Image
          src={images.stretch}
          alt=""
          fill
          className="object-cover brightness-[0.35]"
          sizes="100vw"
        />
        <div className="container-page relative max-w-3xl">
          <p className="section-label">{t.about.academy}</p>
          <h2 className="heading-lg mb-4">{t.about.academy}</h2>
          <p className="text-white/80 leading-relaxed">
            Our Academy develops young talent with professional coaching,
            education support and a clear pathway to the first team. Tomorrow&apos;s
            Vikings are forged here.
          </p>
        </div>
      </section>
    </>
  );
}
