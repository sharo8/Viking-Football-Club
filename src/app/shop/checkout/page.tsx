"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { shopProducts } from "@/lib/data";

type Item = { id: string; qty: number };

export default function CheckoutPage() {
  const [items, setItems] = useState<Item[]>([]);
  const [done, setDone] = useState(false);

  useEffect(() => {
    setItems(JSON.parse(localStorage.getItem("vfc_cart") || "[]"));
  }, []);

  const lines = items
    .map((item) => {
      const product = shopProducts.find((entry) => entry.id === item.id);
      return product ? { ...product, qty: item.qty } : null;
    })
    .filter(Boolean) as { id: string; name: string; price: number; qty: number }[];
  const total = lines.reduce((sum, line) => sum + line.price * line.qty, 0);

  return (
    <section className="section-pad bg-[var(--bg)] text-[var(--text)]">
      <div className="container-page max-w-2xl">
        <h1 className="font-display text-5xl">Checkout</h1>
        {lines.length === 0 ? (
          <p className="mt-4">
            The cart is empty. <Link href="/shop">Back to the shop</Link>
          </p>
        ) : (
          <>
            <ul className="mt-6 divide-y divide-[var(--border)]">
              {lines.map((line) => (
                <li key={line.id} className="flex justify-between py-3">
                  <span>{line.name} × {line.qty}</span>
                  <span>{(line.price * line.qty).toLocaleString("en-RW")} RWF</span>
                </li>
              ))}
            </ul>
            <p className="mt-4 text-xl">Total {total.toLocaleString("en-RW")} RWF</p>
            <form
              className="mt-6 grid gap-3"
              onSubmit={(event) => {
                event.preventDefault();
                localStorage.removeItem("vfc_cart");
                setItems([]);
                setDone(true);
              }}
            >
              <input required placeholder="Full name" className="border border-[var(--border)] bg-transparent px-3 py-3" />
              <input required type="email" placeholder="Email" className="border border-[var(--border)] bg-transparent px-3 py-3" />
              <button className="btn-primary" type="submit">Place demo order</button>
            </form>
          </>
        )}
        {done ? <p className="mt-4" role="status">Order received. This is a demo checkout, no payment was taken.</p> : null}
      </div>
    </section>
  );
}
