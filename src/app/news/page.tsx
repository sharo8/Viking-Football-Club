"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import Image from "next/image";
import { news, images } from "@/lib/data";
import { useLocale } from "@/components/providers/LocaleProvider";
import { localize } from "@/lib/localize";
import { PageHero } from "@/components/ui/PageHero";
import { Reveal } from "@/components/ui/Reveal";

type Cat =
  | "all"
  | "club"
  | "transfers"
  | "academy"
  | "matchReports"
  | "interviews"
  | "training"
  | "videos"
  | "retirement";

export default function NewsPage() {
  const { t, locale } = useLocale();
  const [cat, setCat] = useState<Cat>("all");

  const categories: { key: Cat; label: string }[] = [
    { key: "all", label: t.news.categories.all },
    { key: "club", label: t.news.categories.club },
    { key: "retirement", label: t.news.categories.retirement },
    { key: "transfers", label: t.news.categories.transfers },
    { key: "academy", label: t.news.categories.academy },
    { key: "matchReports", label: t.news.categories.matchReports },
    { key: "interviews", label: t.news.categories.interviews },
    { key: "training", label: t.news.categories.training },
    { key: "videos", label: t.news.categories.videos },
  ];

  const list = useMemo(
    () => (cat === "all" ? news : news.filter((n) => n.category === cat)),
    [cat],
  );

  return (
    <>
      <PageHero title={t.news.title} image={images.run} />
      <section className="section-pad bg-[var(--bg)]">
        <div className="container-page">
          <div className="mb-8 flex flex-wrap gap-2">
            {categories.map((c) => (
              <button
                key={c.key}
                type="button"
                onClick={() => setCat(c.key)}
                className={`px-3 py-2 font-data text-[11px] font-semibold uppercase tracking-[0.12em] ${
                  cat === c.key
                    ? "bg-[var(--accent)] text-[var(--accent-ink)]"
                    : "border border-[var(--border)] text-[var(--text-muted)]"
                }`}
              >
                {c.label}
              </button>
            ))}
          </div>
          <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
            {list.map((article, i) => (
              <Reveal key={article.id} variant="scale" delay={(i % 6) * 70}>
                <Link
                  href={`/news/${article.slug}`}
                  className="card-surface group block overflow-hidden"
                >
                  <div className="relative aspect-[16/10] overflow-hidden">
                    <Image
                      src={article.image}
                      alt={localize(article.title, locale)}
                      fill
                      className="object-cover transition duration-500 group-hover:scale-105"
                      sizes="(max-width:768px) 100vw, 33vw"
                    />
                  </div>
                  <div className="p-5">
                    <p className="font-data text-[11px] font-semibold uppercase tracking-[0.14em] text-[var(--accent)]">
                      {t.news.categories[article.category]}
                      {article.date ? ` · ${article.date}` : ""}
                    </p>
                    <h2 className="mt-2 font-display text-2xl font-bold tracking-wide">
                      {localize(article.title, locale)}
                    </h2>
                    <p className="mt-2 text-sm text-[var(--text-muted)]">
                      {localize(article.summary, locale)}
                    </p>
                    <span className="mt-4 inline-block font-data text-xs font-semibold uppercase tracking-[0.14em] text-[var(--accent)]">
                      {t.news.readMore} →
                    </span>
                  </div>
                </Link>
              </Reveal>
            ))}
          </div>
        </div>
      </section>
    </>
  );
}
