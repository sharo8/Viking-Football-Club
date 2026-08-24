"use client";

import Image from "next/image";
import Link from "next/link";
import { services, images } from "@/lib/data";
import { useLocale } from "@/components/providers/LocaleProvider";
import { PageHero } from "@/components/ui/PageHero";

export default function ServicesPage() {
  const { t } = useLocale();

  return (
    <>
      <PageHero title={t.services.title} image={images.whiteKit} />
      <section className="section-pad bg-[var(--bg)]">
        <div className="container-page grid gap-6 md:grid-cols-2 lg:grid-cols-3">
          {services.map((service) => (
            <article
              key={service.id}
              className="overflow-hidden border border-[var(--border)] bg-[var(--bg-alt)]"
            >
              <div className="relative aspect-[16/10]">
                <Image
                  src={service.image}
                  alt={t.services[service.titleKey]}
                  fill
                  className="object-cover"
                  sizes="(max-width:768px) 100vw, 33vw"
                />
              </div>
              <div className="p-5">
                <h2 className="font-display text-2xl tracking-wide">
                  {t.services[service.titleKey]}
                </h2>
                <p className="mt-2 text-sm text-[var(--text-muted)]">
                  {service.desc}
                </p>
                <Link
                  href="/contact"
                  className="mt-4 inline-flex text-xs font-bold uppercase tracking-[0.14em] text-red"
                >
                  {t.common.learnMore} →
                </Link>
              </div>
            </article>
          ))}
        </div>
      </section>
    </>
  );
}
