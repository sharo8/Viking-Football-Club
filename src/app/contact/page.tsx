"use client";

import {
  Instagram,
  Facebook,
  Youtube,
  MessageCircle,
  Phone,
  Mail,
  MapPin,
} from "lucide-react";
import { club, images } from "@/lib/data";
import { useLocale } from "@/components/providers/LocaleProvider";
import { PageHero } from "@/components/ui/PageHero";

function TikTokIcon({ size = 18 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="currentColor">
      <path d="M19.59 6.69a4.83 4.83 0 0 1-3.77-4.25V2h-3.45v13.67a2.89 2.89 0 0 1-2.88 2.5 2.89 2.89 0 0 1-2.89-2.89 2.89 2.89 0 0 1 2.89-2.89c.28 0 .54.04.79.1v-3.5a6.37 6.37 0 0 0-.79-.05A6.34 6.34 0 0 0 3.15 15.3a6.34 6.34 0 0 0 6.34 6.34 6.34 6.34 0 0 0 6.34-6.34V8.95a8.27 8.27 0 0 0 4.84 1.56V6.99a4.85 4.85 0 0 1-1.08-.3z" />
    </svg>
  );
}

export default function ContactPage() {
  const { t } = useLocale();

  return (
    <>
      <PageHero title={t.contact.title} image={images.squad} />
      <section className="section-pad bg-[var(--bg)]">
        <div className="container-page grid gap-10 lg:grid-cols-2">
          <div>
            <h2 className="heading-lg mb-6">{t.contact.title}</h2>
            <ul className="space-y-4 text-sm">
              <li className="flex items-start gap-3">
                <MapPin className="mt-0.5 text-red" size={18} />
                <div>
                  <p className="font-bold uppercase tracking-wider text-[11px] text-[var(--text-muted)]">
                    {t.contact.address}
                  </p>
                  <p>{club.address}</p>
                </div>
              </li>
              <li className="flex items-start gap-3">
                <Phone className="mt-0.5 text-red" size={18} />
                <div>
                  <p className="font-bold uppercase tracking-wider text-[11px] text-[var(--text-muted)]">
                    {t.contact.phone}
                  </p>
                  <a href={`tel:${club.phone}`}>{club.phone}</a>
                </div>
              </li>
              <li className="flex items-start gap-3">
                <Mail className="mt-0.5 text-red" size={18} />
                <div>
                  <p className="font-bold uppercase tracking-wider text-[11px] text-[var(--text-muted)]">
                    {t.contact.email}
                  </p>
                  <a href={`mailto:${club.email}`}>{club.email}</a>
                </div>
              </li>
            </ul>

            <p className="mt-8 mb-3 text-xs font-bold uppercase tracking-[0.16em] text-[var(--text-muted)]">
              {t.contact.social}
            </p>
            <div className="flex gap-3">
              <a
                href={club.social.instagram}
                target="_blank"
                rel="noreferrer"
                className="rounded-full bg-navy p-2.5 text-white hover:bg-red"
              >
                <Instagram size={18} />
              </a>
              <a
                href={club.social.tiktok}
                target="_blank"
                rel="noreferrer"
                className="rounded-full bg-navy p-2.5 text-white hover:bg-red"
              >
                <TikTokIcon />
              </a>
              <a
                href={club.social.facebook}
                target="_blank"
                rel="noreferrer"
                className="rounded-full bg-navy p-2.5 text-white hover:bg-red"
              >
                <Facebook size={18} />
              </a>
              <a
                href={club.social.youtube}
                target="_blank"
                rel="noreferrer"
                className="rounded-full bg-navy p-2.5 text-white hover:bg-red"
              >
                <Youtube size={18} />
              </a>
              <a
                href={club.social.whatsapp}
                target="_blank"
                rel="noreferrer"
                className="rounded-full bg-navy p-2.5 text-white hover:bg-red"
              >
                <MessageCircle size={18} />
              </a>
            </div>

            <div className="mt-8 overflow-hidden border border-[var(--border)]">
              <iframe
                title="Viking FC Map"
                src="https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3987.5!2d30.0619!3d-1.9441!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x0%3A0x0!2zMcKwNTYnMzguOCJTIDMwwrAwMyc0Mi44IkU!5e0!3m2!1sen!2srw!4v1700000000000"
                className="h-64 w-full grayscale"
                loading="lazy"
                referrerPolicy="no-referrer-when-downgrade"
              />
            </div>
          </div>

          <form
            onSubmit={(e) => e.preventDefault()}
            className="border border-[var(--border)] bg-[var(--bg-alt)] p-6 md:p-8"
          >
            <h3 className="font-display text-3xl tracking-wide mb-6">
              {t.contact.formTitle}
            </h3>
            <div className="space-y-4">
              <div>
                <label className="mb-1 block text-[11px] font-bold uppercase tracking-[0.14em]">
                  {t.contact.name}
                </label>
                <input
                  required
                  className="w-full bg-[var(--bg)] px-3 py-3 text-sm outline-none ring-1 ring-[var(--border)] focus:ring-red"
                />
              </div>
              <div>
                <label className="mb-1 block text-[11px] font-bold uppercase tracking-[0.14em]">
                  {t.contact.email}
                </label>
                <input
                  type="email"
                  required
                  className="w-full bg-[var(--bg)] px-3 py-3 text-sm outline-none ring-1 ring-[var(--border)] focus:ring-red"
                />
              </div>
              <div>
                <label className="mb-1 block text-[11px] font-bold uppercase tracking-[0.14em]">
                  {t.contact.message}
                </label>
                <textarea
                  required
                  rows={5}
                  className="w-full bg-[var(--bg)] px-3 py-3 text-sm outline-none ring-1 ring-[var(--border)] focus:ring-red"
                />
              </div>
              <button type="submit" className="btn-primary w-full">
                {t.contact.send}
              </button>
            </div>
          </form>
        </div>
      </section>
    </>
  );
}
