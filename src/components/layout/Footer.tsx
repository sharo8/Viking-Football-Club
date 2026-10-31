"use client";

import Link from "next/link";
import Image from "next/image";
import { usePathname } from "next/navigation";
import { Instagram, Facebook, Youtube, MessageCircle } from "lucide-react";
import { club, sponsors } from "@/lib/data";
import { useLocale } from "@/components/providers/LocaleProvider";
import { Reveal } from "@/components/ui/Reveal";

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

  const social = [
    {
      href: club.social.instagram,
      label: "Instagram",
      icon: <Instagram size={18} />,
    },
    { href: club.social.tiktok, label: "TikTok", icon: <TikTokIcon size={18} /> },
    {
      href: club.social.facebook,
      label: "Facebook",
      icon: <Facebook size={18} />,
    },
    {
      href: club.social.youtube,
      label: "YouTube",
      icon: <Youtube size={18} />,
    },
    {
      href: club.social.whatsapp,
      label: "WhatsApp",
      icon: <MessageCircle size={18} />,
    },
  ];

  return (
    <footer className="footer-shell">
      <div className="container-page py-12 md:py-14">
        <div className="grid items-start gap-10 sm:grid-cols-2 lg:grid-cols-[1.4fr_0.9fr_0.85fr_1fr] lg:gap-8 xl:gap-10">
          <Reveal variant="up" once delay={0} className="min-w-0">
            <div className="mb-4 flex items-center gap-3">
              <Image
                src={club.logo}
                alt="Viking FC"
                width={52}
                height={52}
                className="h-12 w-12 object-contain"
              />
              <p className="font-display text-xl tracking-[0.1em] text-[var(--footer-ink)] md:text-2xl">
                VIKING FC
              </p>
            </div>
            <p className="mb-3 text-sm italic tracking-wide text-[var(--footer-muted)]">
              {club.slogan}
            </p>
            <p className="max-w-sm text-sm leading-relaxed text-[var(--footer-muted)]">
              {t.footer.mission}
            </p>
          </Reveal>

          <Reveal variant="up" once delay={90} className="min-w-0">
            <h3 className="footer-group-title">{t.footer.quickLinks}</h3>
            <ul className="space-y-2.5">
              {quick.map((item) => (
                <li key={item.href}>
                  <Link href={item.href} className="footer-link-bar">
                    {item.label}
                  </Link>
                </li>
              ))}
            </ul>
          </Reveal>

          <Reveal variant="up" once delay={180} className="min-w-0">
            <h3 className="footer-group-title">{t.sponsors.title}</h3>
            <ul className="space-y-3">
              {sponsors.map((s) => (
                <li key={s.name}>
                  <span className="footer-partner-chip">{s.name}</span>
                </li>
              ))}
            </ul>
          </Reveal>

          <Reveal variant="up" once delay={270} className="min-w-0">
            <h3 className="footer-group-title">{t.footer.follow}</h3>
            <div className="mb-5 flex flex-wrap gap-2.5">
              {social.map((item) => (
                <a
                  key={item.label}
                  href={item.href}
                  target="_blank"
                  rel="noreferrer"
                  aria-label={item.label}
                  className="social-orb-lg"
                >
                  {item.icon}
                </a>
              ))}
            </div>
            <Link
              href="#newsletter"
              className="btn-outline inline-flex !min-h-[2.5rem] !rounded-md !border-[var(--footer-ink)] !px-4 !py-0 !text-[0.68rem] !text-[var(--footer-ink)] hover:!bg-[var(--footer-ink)] hover:!text-[var(--footer-bg)]"
            >
              {t.newsletter.subscribe}
            </Link>
          </Reveal>
        </div>
      </div>

      <div className="footer-bottom">
        <div className="container-page flex items-center justify-center py-4 md:py-5">
          <p className="font-data text-[0.58rem] uppercase tracking-[0.14em] text-[var(--footer-muted)]">
            {t.footer.copyright}
          </p>
        </div>
      </div>
    </footer>
  );
}
