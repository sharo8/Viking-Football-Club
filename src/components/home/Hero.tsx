"use client";

import Link from "next/link";
import Image from "next/image";
import { motion } from "framer-motion";
import { club, images } from "@/lib/data";
import { useLocale } from "@/components/providers/LocaleProvider";

export function Hero() {
  const { t } = useLocale();

  return (
    <section className="relative min-h-[100svh] overflow-hidden bg-navy-deep">
      <video
        className="absolute inset-0 h-full w-full object-cover brightness-[0.55] contrast-[1.08] saturate-[1.05]"
        autoPlay
        muted
        loop
        playsInline
        poster={images.celebrate}
      >
        <source src={images.heroVideo} type="video/mp4" />
      </video>
      <div className="absolute inset-0 bg-gradient-to-b from-navy-deep/55 via-navy/35 to-navy-deep" />
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,transparent_20%,rgba(6,20,39,0.75)_100%)]" />

      <div className="relative z-10 flex min-h-[100svh] flex-col items-center justify-center px-4 pb-16 pt-28 text-center text-white">
        <motion.div
          initial={{ opacity: 0, scale: 0.88 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.8 }}
        >
          <Image
            src={club.logo}
            alt="Viking FC"
            width={160}
            height={160}
            priority
            className="mx-auto mb-6 h-28 w-28 object-contain drop-shadow-[0_8px_30px_rgba(0,0,0,0.55)] md:h-40 md:w-40"
          />
        </motion.div>

        <motion.h1
          initial={{ opacity: 0, y: 28 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.15, duration: 0.7 }}
          className="font-display text-[clamp(3.4rem,12vw,8rem)] leading-[0.9] tracking-[0.06em] text-white"
        >
          {t.hero.brand}
        </motion.h1>

        <motion.p
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3, duration: 0.7 }}
          className="mt-5 max-w-xl text-base font-medium tracking-wide text-white/85 md:text-xl"
        >
          {t.hero.slogan1} {t.hero.slogan2} {t.hero.slogan3}
        </motion.p>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.45, duration: 0.7 }}
          className="mt-9 flex flex-wrap items-center justify-center gap-3"
        >
          <Link href="/news" className="btn-primary">
            {t.hero.latestNews}
          </Link>
          <Link href="/matches" className="btn-outline text-white">
            {t.hero.nextMatch}
          </Link>
        </motion.div>
      </div>
    </section>
  );
}
