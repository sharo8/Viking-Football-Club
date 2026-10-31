"use client";

import { useMemo, useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { shopProducts, images } from "@/lib/data";
import { useLocale } from "@/components/providers/LocaleProvider";
import { PageHero } from "@/components/ui/PageHero";
import { Reveal } from "@/components/ui/Reveal";

type Cat = "all" | "jerseys" | "caps" | "scarves" | "boots" | "accessories";

export default function ShopPage() {
  const { t } = useLocale();
  const router = useRouter();
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
                className={`px-4 py-2 font-data text-xs font-semibold uppercase tracking-[0.14em] ${
                  cat === f.key
                    ? "bg-[var(--accent)] text-[var(--accent-ink)]"
                    : "border border-[var(--border)]"
                }`}
              >
                {f.label}
              </button>
            ))}
          </div>
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
            {list.map((product, i) => (
              <Reveal key={product.id} variant="up" delay={(i % 8) * 60}>
                <article className="card-surface overflow-hidden">
                  <div className="relative aspect-square overflow-hidden bg-navy/10">
                    <Image
                      src={product.image}
                      alt={product.name}
                      fill
                      className="object-cover transition duration-500 hover:scale-105"
                      sizes="(max-width:768px) 50vw, 25vw"
                    />
                  </div>
                  <div className="p-4">
                    <h3 className="font-display text-xl font-bold tracking-wide">
                      <Link href={`/shop/${product.id}`}>{product.name}</Link>
                    </h3>
                    <p className="mt-1 font-data text-sm font-semibold text-[var(--accent)]">
                      {product.price.toLocaleString()} RWF
                    </p>
                    <button
                      type="button"
                      className="btn-primary mt-4 w-full"
                      onClick={() => {
                        const cart = JSON.parse(localStorage.getItem("vfc_cart") || "[]") as { id: string; qty: number }[];
                        const found = cart.find((item) => item.id === product.id);
                        const next = found
                          ? cart.map((item) => (item.id === product.id ? { ...item, qty: item.qty + 1 } : item))
                          : [...cart, { id: product.id, qty: 1 }];
                        localStorage.setItem("vfc_cart", JSON.stringify(next));
                        router.push("/shop/checkout");
                      }}
                    >
                      {t.shop.addToCart}
                    </button>
                  </div>
                </article>
              </Reveal>
            ))}
          </div>
        </div>
      </section>
    </>
  );
}
