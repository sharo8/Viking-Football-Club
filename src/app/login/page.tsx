"use client";

import Link from "next/link";
import Image from "next/image";
import { club } from "@/lib/data";
import { useLocale } from "@/components/providers/LocaleProvider";

export default function LoginPage() {
  const { t } = useLocale();

  return (
    <div className="flex min-h-screen items-center justify-center bg-navy-deep px-4 pt-24 pb-16">
      <div className="w-full max-w-md border border-white/10 bg-navy p-8 text-white">
        <Image
          src={club.logo}
          alt="Viking FC"
          width={72}
          height={72}
          className="mx-auto mb-4"
        />
        <h1 className="text-center font-display text-4xl tracking-wider">
          {t.nav.login}
        </h1>
        <form
          className="mt-8 space-y-4"
          onSubmit={(e) => {
            e.preventDefault();
            window.location.href = "/admin";
          }}
        >
          <input
            type="email"
            required
            placeholder="Email"
            className="w-full bg-white/5 px-3 py-3 text-sm outline-none ring-1 ring-white/15 focus:ring-red"
          />
          <input
            type="password"
            required
            placeholder="Password"
            className="w-full bg-white/5 px-3 py-3 text-sm outline-none ring-1 ring-white/15 focus:ring-red"
          />
          <button type="submit" className="btn-primary w-full">
            {t.nav.login}
          </button>
        </form>
        <p className="mt-4 text-center text-sm text-white/60">
          No account?{" "}
          <Link href="/register" className="text-red">
            {t.nav.register}
          </Link>
        </p>
        <p className="mt-2 text-center text-xs text-white/40">
          Demo: any email/password opens Admin
        </p>
      </div>
    </div>
  );
}
