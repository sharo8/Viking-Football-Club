"use client";

import Image from "next/image";
import { staff, images } from "@/lib/data";
import { useLocale } from "@/components/providers/LocaleProvider";
import { PageHero } from "@/components/ui/PageHero";

export default function ManagementPage() {
  const { t } = useLocale();
  const leadership = staff.slice(0, 5);
  const technical = staff.slice(5);

  return (
    <>
      <PageHero
        title={t.management.title}
        subtitle={t.management.technical}
        image={images.studio}
      />
      <section className="section-pad bg-[var(--bg)]">
        <div className="container-page">
          <h2 className="heading-lg mb-8">{t.management.title}</h2>
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {leadership.map((member) => (
              <article
                key={member.id}
                className="overflow-hidden border border-[var(--border)] bg-[var(--bg-alt)]"
              >
                <div className="relative aspect-[4/5]">
                  <Image
                    src={member.image}
                    alt={member.name}
                    fill
                    className="object-cover"
                    sizes="(max-width:768px) 100vw, 33vw"
                  />
                </div>
                <div className="p-5">
                  <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-red">
                    {member.role}
                  </p>
                  <h3 className="mt-1 font-display text-2xl tracking-wide">
                    {member.name}
                  </h3>
                  <p className="mt-3 text-sm text-[var(--text-muted)]">
                    {member.bio}
                  </p>
                </div>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="section-pad bg-[var(--bg-alt)]">
        <div className="container-page">
          <h2 className="heading-lg mb-8">{t.management.technical}</h2>
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {technical.map((member) => (
              <article
                key={member.id}
                className="border border-[var(--border)] bg-[var(--bg)] p-4"
              >
                <div className="relative mb-4 aspect-square overflow-hidden">
                  <Image
                    src={member.image}
                    alt={member.name}
                    fill
                    className="object-cover"
                    sizes="(max-width:768px) 50vw, 25vw"
                  />
                </div>
                <p className="text-[11px] font-bold uppercase tracking-[0.14em] text-red">
                  {member.role}
                </p>
                <h3 className="font-display text-xl tracking-wide">
                  {member.name}
                </h3>
                <p className="mt-2 text-xs text-[var(--text-muted)]">
                  {member.bio}
                </p>
              </article>
            ))}
          </div>
        </div>
      </section>
    </>
  );
}
