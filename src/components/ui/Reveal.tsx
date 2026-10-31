"use client";

import {
  useEffect,
  useRef,
  useState,
  type CSSProperties,
  type ElementType,
  type ReactNode,
} from "react";

type RevealVariant = "up" | "left" | "right" | "scale" | "blur" | "clip";

type RevealProps = {
  children: ReactNode;
  variant?: RevealVariant;
  delay?: number;
  className?: string;
  as?: ElementType;
  once?: boolean;
};

const variantClass: Record<RevealVariant, string> = {
  up: "reveal-up",
  left: "reveal-left",
  right: "reveal-right",
  scale: "reveal-scale",
  blur: "reveal-blur",
  clip: "reveal-clip",
};

export function Reveal({
  children,
  variant = "up",
  delay = 0,
  className = "",
  as: Tag = "div",
  once = false,
}: RevealProps) {
  const ref = useRef<HTMLElement | null>(null);
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
          if (once) observer.unobserve(el);
        } else if (!once) {
          setVisible(false);
        }
      },
      { threshold: 0.15, rootMargin: "0px 0px -6% 0px" },
    );

    observer.observe(el);
    return () => observer.disconnect();
  }, [once]);

  const style = {
    transitionDelay: delay ? `${delay}ms` : undefined,
  } as CSSProperties;

  return (
    <Tag
      ref={ref as never}
      style={style}
      className={`reveal ${variantClass[variant]} ${visible ? "is-visible" : ""} ${className}`}
    >
      {children}
    </Tag>
  );
}
