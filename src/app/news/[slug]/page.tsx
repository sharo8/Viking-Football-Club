"use client";

import Image from "next/image";
import Link from "next/link";
import { useParams } from "next/navigation";
import { news } from "@/lib/data";
import { useLocale } from "@/components/providers/LocaleProvider";
import { localize } from "@/lib/localize";

export default function NewsDetailPage() {
  const params = useParams<{ slug: string }>();
  const { t, locale } = useLocale();
  const article = news.find((n) => n.slug === params.slug);

  if (!article) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-4 pt-24">
        <h1 className="heading-lg">
          {locale === "fr"
            ? "Article introuvable"
            : locale === "rw"
              ? "Ingingo ntabwo yabonetse"
              : "Article not found"}
        </h1>
        <Link href="/news" className="btn-primary">
          {t.common.back}
        </Link>
      </div>
    );
  }

  const title = localize(article.title, locale);
  const summary = localize(article.summary, locale);
  const content = localize(article.content, locale);

  return (
    <article className="bg-[var(--bg)] pt-20">
      <div className="relative h-[48vh] min-h-[320px]">
        <Image
          src={article.image}
          alt={title}
          fill
          className="object-cover brightness-[0.55]"
          priority
          sizes="100vw"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-navy-deep to-transparent" />
        <div className="container-page absolute inset-x-0 bottom-0 pb-10 text-white">
          <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-gold">
            {t.news.categories[article.category]} · {article.date}
          </p>
          <h1 className="mt-3 max-w-4xl heading-lg">{title}</h1>
        </div>
      </div>
      <div className="container-page section-pad max-w-3xl">
        <p className="text-lg leading-relaxed text-[var(--text-muted)]">
          {summary}
        </p>
        <p className="mt-6 leading-relaxed">{content}</p>
        <div className="mt-8 flex flex-wrap gap-3">
          <button type="button" className="btn-outline border-[var(--border)]">
            {locale === "fr" ? "J'aime" : locale === "rw" ? "Nkunda" : "Like"}
          </button>
          <button type="button" className="btn-outline border-[var(--border)]">
            {locale === "fr" ? "Partager" : locale === "rw" ? "Sangiza" : "Share"}
          </button>
          <button type="button" className="btn-outline border-[var(--border)]">
            {locale === "fr" ? "Commenter" : locale === "rw" ? "Tanga igitekerezo" : "Comment"}
          </button>
        </div>
        <Link
          href="/news"
          className="mt-8 inline-flex text-xs font-bold uppercase tracking-[0.14em] text-red"
        >
          ← {t.common.back}
        </Link>
      </div>
    </article>
  );
}
