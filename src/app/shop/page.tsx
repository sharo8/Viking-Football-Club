"use client";

import { useMemo, useState } from "react";
import Image from "next/image";
import { shopProducts, images } from "@/lib/data";
import { useLocale } from "@/components/providers/LocaleProvider";
import { PageHero } from "@/components/ui/PageHero";

type Cat = "all" | "jerseys" | "caps" | "scarves" | "boots" | "accessories";

export default function ShopPage() {
  const { t } = useLocale();
  const [cat, setCat] = useState<Cat>("all");

  const filters: { key: Cat; label: string }[] = [
    { key: "all", label: t.news.categories.all },
    { key: "jerseys", label: t.shop.jerseys },
    { key: "caps", label: t.shop.caps },
    { key: "scarves", label: t.shop.scarves },
    { key: "boots", label: t.shop.boots },
    { key: "accessories", label: t.shop.accessories },
  ];

  const list = useMemo(
    () =>
      cat === "all"
        ? shopProducts
        : shopProducts.filter((p) => p.category === cat),
    [cat],
  );

  return (
    <>
      <PageHero title={t.shop.title} image={images.blueKit} />
      <section className="section-pad bg-[var(--bg)]">
        <div className="container-page">
          <div className="mb-8 flex flex-wrap gap-2">
            {filters.map((f) => (
              <button
                key={f.key}
                type="button"
                onClick={() => setCat(f.key)}
                className={`px-4 py-2 text-xs font-bold uppercase tracking-[0.14em] ${
                  cat === f.key
                    ? "bg-red text-white"
                    : "border border-[var(--border)]"
                }`}
              >
                {f.label}
              </button>
            ))}
          </div>
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
            {list.map((product) => (
              <article
                key={product.id}
                className="border border-[var(--border)] bg-[var(--bg-alt)]"
              >
                <div className="relative aspect-square overflow-hidden bg-navy/10">
                  <Image
                    src={product.image}
                    alt={product.name}
                    fill
                    className="object-cover"
                    sizes="(max-width:768px) 50vw, 25vw"
                  />
                </div>
                <div className="p-4">
                  <h3 className="font-display text-xl tracking-wide">
                    {product.name}
                  </h3>
                  <p className="mt-1 font-data text-sm font-semibold text-red">
                    {product.price.toLocaleString()} RWF
                  </p>
                  <button type="button" className="btn-primary mt-4 w-full">
                    {t.shop.addToCart}
                  </button>
                </div>
              </article>
            ))}
          </div>
        </div>
      </section>
    </>
  );
}
