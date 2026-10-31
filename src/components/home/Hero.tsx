"use client";

import Link from "next/link";
import { motion } from "framer-motion";
import { images } from "@/lib/data";
import { useLocale } from "@/components/providers/LocaleProvider";

export function Hero() {
  const { t } = useLocale();

  return (
    <section className="relative min-h-[100svh] overflow-hidden bg-navy-deep">
      <video
        className="hero-video"
        autoPlay
        muted
        loop
        playsInline
        poster={images.celebrate}
      >
        <source src={images.heroVideo} type="video/mp4" />
      </video>

      <div className="pointer-events-none absolute inset-0 bg-gradient-to-b from-black/45 via-black/25 to-black/55" />
      <div className="pointer-events-none absolute inset-x-0 bottom-0 h-[42%] bg-gradient-to-t from-black/70 via-black/25 to-transparent" />

      <div className="relative z-10 flex min-h-[100svh] w-full flex-col items-center justify-center px-4 pb-20 pt-24 text-center md:pb-24">
        <div className="mx-auto flex w-full max-w-3xl flex-col items-center text-center text-white">
          <motion.h1
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.7 }}
            className="font-hero-brand w-full text-center text-[clamp(2rem,5.5vw,3.6rem)] leading-[1.08] tracking-[0.08em] text-white drop-shadow-[0_8px_28px_rgba(0,0,0,0.65)]"
          >
            {t.hero.brand}
          </motion.h1>

          <motion.p
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.12, duration: 0.65 }}
            className="mt-4 w-full text-center text-sm font-medium tracking-wide text-white/95 drop-shadow-[0_2px_12px_rgba(0,0,0,0.5)] md:mt-5 md:text-base"
          >
            {t.hero.slogan1} {t.hero.slogan2} {t.hero.slogan3}
          </motion.p>

          <motion.div
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.24, duration: 0.65 }}
            className="mt-7 flex flex-wrap items-center justify-center gap-3 md:mt-8"
          >
            <Link
              href="/news"
              className="btn-primary !px-5 !py-2.5 !text-[0.72rem]"
            >
              {t.hero.latestNews}
            </Link>
            <Link
              href="/matches"
              className="btn-outline text-white !px-5 !py-2.5 !text-[0.72rem]"
            >
              {t.hero.nextMatch}
            </Link>
          </motion.div>
        </div>
      </div>
    </section>
  );
}
