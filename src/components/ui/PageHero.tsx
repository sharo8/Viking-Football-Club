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
        className="object-cover brightness-[0.45] contrast-[1.05]"
        sizes="100vw"
      />
      <div className="absolute inset-0 bg-gradient-to-t from-navy-deep via-navy/50 to-navy/30" />
      <div className="container-page relative">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
        >
          <h1 className="heading-xl">{title}</h1>
          {subtitle && (
            <p className="mt-3 max-w-2xl text-base text-white/75 md:text-lg">
              {subtitle}
            </p>
          )}
        </motion.div>
      </div>
    </section>
  );
}
