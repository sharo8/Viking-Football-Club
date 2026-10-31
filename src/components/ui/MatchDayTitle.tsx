"use client";

import { useEffect, useRef, useState } from "react";

type Props = {
  left: string;
  vs: string;
  right: string;
  className?: string;
};

export function MatchDayTitle({ left, vs, right, className = "" }: Props) {
  const ref = useRef<HTMLHeadingElement | null>(null);
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;

    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
      setVisible(true);
      return;
    }

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setVisible(true);
          observer.disconnect();
        }
      },
      { threshold: 0.25, rootMargin: "0px 0px -5% 0px" },
    );

    observer.observe(el);
    return () => observer.disconnect();
  }, []);

  return (
    <h2
      ref={ref}
      className={`heading-match-day mb-7 flex max-w-full flex-wrap items-baseline justify-center gap-x-2.5 gap-y-1 overflow-hidden text-center lg:justify-start lg:text-left ${className}`}
    >
      <span
        className={`match-title-slide inline-block will-change-transform ${
          visible ? "is-in" : "from-left"
        }`}
      >
        {left}
      </span>
      <span
        className={`match-title-slide inline-block will-change-transform ${
          visible ? "is-in" : "from-right"
        }`}
      >
        <span className="text-[var(--accent)]">{vs}</span> {right}
      </span>
    </h2>
  );
}
