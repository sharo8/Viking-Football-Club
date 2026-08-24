"use client";

import Link from "next/link";
import Image from "next/image";
import { usePathname } from "next/navigation";
import { Instagram, Facebook, Youtube, MessageCircle } from "lucide-react";
import { club, sponsors } from "@/lib/data";
import { useLocale } from "@/components/providers/LocaleProvider";

function TikTokIcon({ size = 18 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="currentColor">
      <path d="M19.59 6.69a4.83 4.83 0 0 1-3.77-4.25V2h-3.45v13.67a2.89 2.89 0 0 1-2.88 2.5 2.89 2.89 0 0 1-2.89-2.89 2.89 2.89 0 0 1 2.89-2.89c.28 0 .54.04.79.1v-3.5a6.37 6.37 0 0 0-.79-.05A6.34 6.34 0 0 0 3.15 15.3a6.34 6.34 0 0 0 6.34 6.34 6.34 6.34 0 0 0 6.34-6.34V8.95a8.27 8.27 0 0 0 4.84 1.56V6.99a4.85 4.85 0 0 1-1.08-.3z" />
    </svg>
  );
}

export function Footer() {
  const pathname = usePathname();
  const { t } = useLocale();

  if (pathname.startsWith("/admin")) return null;

  const quick = [
    { href: "/about", label: t.nav.about },
    { href: "/players", label: t.nav.players },
    { href: "/matches", label: t.nav.matches },
    { href: "/news", label: t.nav.news },
    { href: "/shop", label: t.nav.shop },
    { href: "/contact", label: t.nav.contact },
  ];

  return (
    <footer className="border-t border-[var(--border)] bg-navy text-white">
      <div className="container-page section-pad grid gap-10 md:grid-cols-2 lg:grid-cols-4">
        <div>
          <div className="mb-4 flex items-center gap-3">
            <Image
              src={club.logo}
              alt="Viking FC"
              width={56}
              height={56}
              className="h-14 w-14 object-contain"
            />
            <div>
              <p className="font-display text-2xl tracking-[0.12em]">VIKING FC</p>
              <p className="text-xs text-white/60">{club.slogan}</p>
            </div>
          </div>
          <p className="text-sm leading-relaxed text-white/70">
            Professional football. Family spirit. European ambition.
          </p>
        </div>

        <div>
          <h3 className="mb-4 font-display text-xl tracking-wider text-gold">
            {t.footer.quickLinks}
          </h3>
          <ul className="space-y-2">
            {quick.map((item) => (
              <li key={item.href}>
                <Link
                  href={item.href}
                  className="text-sm text-white/70 transition hover:text-red"
                >
                  {item.label}
                </Link>
              </li>
            ))}
          </ul>
        </div>

        <div>
          <h3 className="mb-4 font-display text-xl tracking-wider text-gold">
            {t.sponsors.title}
          </h3>
          <div className="flex flex-wrap gap-3">
            {sponsors.slice(0, 4).map((s) => (
              <span
                key={s.name}
                className="border border-white/15 px-3 py-2 text-xs font-semibold uppercase tracking-wider text-white/70"
              >
                {s.name}
              </span>
            ))}
          </div>
        </div>

        <div>
          <h3 className="mb-4 font-display text-xl tracking-wider text-gold">
            {t.newsletter.title}
          </h3>
          <form
            className="flex flex-col gap-2"
            onSubmit={(e) => e.preventDefault()}
          >
            <input
              type="email"
              required
              placeholder={t.newsletter.placeholder}
              className="bg-white/5 px-3 py-2.5 text-sm outline-none ring-1 ring-white/15 placeholder:text-white/40 focus:ring-red"
            />
            <button type="submit" className="btn-primary">
              {t.newsletter.subscribe}
            </button>
          </form>
          <div className="mt-5">
            <p className="mb-3 text-xs font-bold uppercase tracking-[0.16em] text-white/50">
              {t.footer.follow}
            </p>
            <div className="flex gap-3">
              <a
                href={club.social.instagram}
                target="_blank"
                rel="noreferrer"
                aria-label="Instagram"
                className="rounded-full bg-white/10 p-2 hover:bg-red"
              >
                <Instagram size={18} />
              </a>
              <a
                href={club.social.tiktok}
                target="_blank"
                rel="noreferrer"
                aria-label="TikTok"
                className="rounded-full bg-white/10 p-2 hover:bg-red"
              >
                <TikTokIcon />
              </a>
              <a
                href={club.social.facebook}
                target="_blank"
                rel="noreferrer"
                aria-label="Facebook"
                className="rounded-full bg-white/10 p-2 hover:bg-red"
              >
                <Facebook size={18} />
              </a>
              <a
                href={club.social.youtube}
                target="_blank"
                rel="noreferrer"
                aria-label="YouTube"
                className="rounded-full bg-white/10 p-2 hover:bg-red"
              >
                <Youtube size={18} />
              </a>
              <a
                href={club.social.whatsapp}
                target="_blank"
                rel="noreferrer"
                aria-label="WhatsApp"
                className="rounded-full bg-white/10 p-2 hover:bg-red"
              >
                <MessageCircle size={18} />
              </a>
            </div>
          </div>
        </div>
      </div>
      <div className="border-t border-white/10 py-5 text-center text-xs text-white/50">
        {t.footer.copyright}
      </div>
    </footer>
  );
}
