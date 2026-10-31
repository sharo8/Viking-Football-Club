"use client";

import Image from "next/image";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { shopProducts } from "@/lib/data";

export default function ProductPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const product = shopProducts.find((item) => item.id === params.id);

  if (!product) {
    return (
      <section className="section-pad">
        <div className="container-page">
          <h1>Product not found</h1>
          <Link href="/shop">Back to the shop</Link>
        </div>
      </section>
    );
  }

  return (
    <section className="section-pad bg-[var(--bg)] text-[var(--text)]">
      <div className="container-page grid items-center gap-8 md:grid-cols-2">
        <div className="relative aspect-square overflow-hidden border border-[var(--border)]">
          <Image src={product.image} alt={product.name} fill className="object-cover" />
        </div>
        <div>
          <p className="text-sm uppercase tracking-[0.16em] text-[var(--text-muted)]">{product.category}</p>
          <h1 className="font-display text-5xl">{product.name}</h1>
          <p className="mt-3 font-data text-xl">{product.price.toLocaleString("en-RW")} RWF</p>
          <button
            type="button"
            className="btn-primary mt-6"
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
            Add to cart
          </button>
        </div>
      </div>
    </section>
  );
}
