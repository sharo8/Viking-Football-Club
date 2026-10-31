"use client";

import Image from "next/image";
import { motion } from "framer-motion";

type Props = {
  title: string;
  subtitle?: string;
  image: string;
};

export function PageHero({ title, subtitle, image }: Props) {
  return (
    <section className="relative flex min-h-[42vh] items-end overflow-hidden bg-navy-deep pb-12 pt-28 text-white md:min-h-[48vh]">
      <Image
        src={image}
        alt=""
        fill
        priority
        className="object-cover"
        sizes="100vw"
      />
      <div className="pointer-events-none absolute inset-x-0 bottom-0 h-[55%] bg-gradient-to-t from-black/75 via-black/30 to-transparent" />
      <div className="container-page relative text-center">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
          className="mx-auto flex w-full max-w-3xl flex-col items-center"
        >
          <h1 className="heading-xl drop-shadow-[0_6px_20px_rgba(0,0,0,0.4)]">
            {title}
          </h1>
          {subtitle && (
            <p className="mt-3 max-w-2xl text-base text-white/85 md:text-lg">
              {subtitle}
            </p>
          )}
        </motion.div>
      </div>
    </section>
  );
}
