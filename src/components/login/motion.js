"use client";

import { useEffect, useState } from "react";

/**
 * One knob for the login choreography.
 * 1 is the designed speed. 0.6 is snappier, 1.4 is slower.
 */
export const SPEED = 1;

/** Set to false to remove the canvas particles and their connecting lines. */
export const PARTICLES_ENABLED = true;

export const easeOutExpo = [0.22, 1, 0.36, 1];

const seconds = (value) => value * SPEED;

export const fadeUp = {
  hidden: { opacity: 0, y: 24 },
  show: {
    opacity: 1,
    y: 0,
    transition: { duration: seconds(0.55), ease: easeOutExpo },
  },
};

export const fadeIn = {
  hidden: { opacity: 0 },
  show: {
    opacity: 1,
    transition: { duration: seconds(0.45), ease: easeOutExpo },
  },
};

export const scaleIn = {
  hidden: { opacity: 0, scale: 0.96 },
  show: {
    opacity: 1,
    scale: 1,
    transition: { duration: seconds(0.55), ease: easeOutExpo },
  },
};

export function staggerContainer(delay = 0, stagger = 0.08) {
  return {
    hidden: {},
    show: {
      transition: {
        delayChildren: seconds(delay),
        staggerChildren: seconds(stagger),
      },
    },
  };
}

export const shake = {
  x: [0, -10, 10, -8, 8, -4, 4, 0],
  transition: { duration: seconds(0.45) },
};

export const springSoft = {
  type: "spring",
  stiffness: 120,
  damping: 14,
};

export function useReducedMotionSafe() {
  const [reduced, setReduced] = useState(false);

  useEffect(() => {
    const query = window.matchMedia("(prefers-reduced-motion: reduce)");
    const apply = () => setReduced(query.matches);
    apply();
    query.addEventListener("change", apply);
    return () => query.removeEventListener("change", apply);
  }, []);

  return reduced;
}
