"use client";

import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { motion } from "framer-motion";
import { AlertCircle, Loader2, X } from "lucide-react";
import { getMediaUrl } from "@/services/media";

function still() {
  return typeof window !== "undefined" && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

export function Button({ variant = "secondary", size = "md", icon = /** @type {any} */ (null), loading = /** @type {any} */ (false), children, className = "", ...props }) {
  return (
    <button type="button" className={`vfc-btn vfc-btn--${variant} vfc-btn--${size} ${className}`} disabled={loading || props.disabled} {...props}>
      {loading ? <Loader2 className="vfc-spin" size={16} /> : icon}
      {children ? <span>{children}</span> : null}
    </button>
  );
}

export function IconButton({ label, children, ...props }) {
  return (
    <button type="button" className="vfc-icon" aria-label={label} title={label} {...props}>
      {children}
    </button>
  );
}

export function Field({ label, required = false, error = /** @type {any} */ (undefined), hint = /** @type {any} */ (undefined), children }) {
  return (
    <label className={`vfc-field${error ? " is-error" : ""}`}>
      {label ? <span className="vfc-label">{label}{required ? <i>*</i> : null}</span> : null}
      {children}
      {error ? <small className="vfc-error"><AlertCircle size={14} /> {error}</small> : hint ? <small className="vfc-hint">{hint}</small> : null}
    </label>
  );
}

export function TextInput(props) {
  return <input className="vfc-control" {...props} />;
}

export function PhoneInput({ value = "", onChange, ...props }) {
  const digits = String(value).replace(/\D/g, "").replace(/^250/, "").slice(0, 9);
  const shown = digits.replace(/(\d{3})(\d{0,3})(\d{0,3})/, (_, a, b, c) => [a, b, c].filter(Boolean).join(" "));
  return (
    <span className="vfc-phone">
      <span>+250</span>
      <input
        className="vfc-control"
        inputMode="numeric"
        autoComplete="tel"
        placeholder="7XX XXX XXX"
        value={shown}
        onChange={(event) => {
          const next = event.target.value.replace(/\D/g, "").slice(0, 9);
          onChange(next ? `+250${next}` : "");
        }}
        {...props}
      />
    </span>
  );
}

export function SelectInput({ children, ...props }) {
  return <select className="vfc-control" {...props}>{children}</select>;
}

export function TextArea(props) {
  return <textarea className="vfc-control vfc-area" {...props} />;
}

export function Switch({ checked, onChange, label }) {
  return (
    <button type="button" role="switch" aria-checked={checked} aria-label={label} className={`vfc-switch${checked ? " is-on" : ""}`} onClick={() => onChange(!checked)}>
      <span />
    </button>
  );
}

export function Modal({ title, subtitle = /** @type {any} */ (undefined), leading = /** @type {any} */ (null), onClose, size = "md", footer = /** @type {any} */ (null), children, closeLabel = "Close" }) {
  const ref = useRef(null);
  useEffect(() => {
    const previous = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    const onKey = (event) => {
      if (event.key === "Escape") onClose();
      if (event.key !== "Tab" || !ref.current) return;
      const nodes = [...ref.current.querySelectorAll("button, input, select, textarea, a[href]")].filter((node) => !node.disabled);
      if (!nodes.length) return;
      const first = nodes[0];
      const last = nodes[nodes.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    };
    document.addEventListener("keydown", onKey);
    return () => {
      document.body.style.overflow = previous;
      document.removeEventListener("keydown", onKey);
    };
  }, [onClose]);

  const motionProps = still()
    ? {}
    : { initial: { opacity: 0, scale: 0.96, y: 16 }, animate: { opacity: 1, scale: 1, y: 0 }, transition: { type: "spring", stiffness: 260, damping: 24 } };

  return createPortal(
    <div className="vfc-backdrop" onMouseDown={onClose}>
      <motion.div ref={ref} className={`vfc-modal vfc-modal--${size}`} role="dialog" aria-modal="true" aria-label={title} onMouseDown={(event) => event.stopPropagation()} {...motionProps}>
        <header className="vfc-modal-head">
          {leading}
          <div>
            <h2>{title}</h2>
            {subtitle ? <p>{subtitle}</p> : null}
          </div>
          <IconButton label={closeLabel} onClick={onClose}><X size={18} /></IconButton>
        </header>
        <div className="vfc-modal-body">{children}</div>
        {footer ? <footer className="vfc-modal-foot">{footer}</footer> : null}
      </motion.div>
    </div>,
    document.body,
  );
}

const AVATAR_SIZE = {
  xs: "vfc-avatar--xs",
  sm: "vfc-avatar--sm",
  md: "vfc-avatar--md",
  lg: "vfc-avatar--lg",
  xl: "vfc-avatar--xl",
  96: "vfc-avatar--xl",
  64: "vfc-avatar--64",
  112: "vfc-avatar--112",
};

const AVATAR_TONES = ["#a3283a", "#7a1e2c", "#1f2937", "#3f3a36", "#5c2a33", "#243044", "#6b2d3c", "#4a2c3a"];

function avatarLetters(name) {
  const parts = String(name || "?").trim().split(/\s+/).filter(Boolean);
  const letters = parts.slice(0, 2).map((part) => part[0]).join("");
  return (letters || "?").toUpperCase();
}

function avatarTone(name) {
  const text = String(name || "?");
  let hash = 0;
  for (let index = 0; index < text.length; index += 1) hash = (hash + text.charCodeAt(index) * (index + 1)) % AVATAR_TONES.length;
  return AVATAR_TONES[hash];
}

export function Avatar({ name, src, size = "md", ring = false, status = /** @type {any} */ (undefined) }) {
  const [failed, setFailed] = useState(false);
  useEffect(() => { setFailed(false); }, [src]);
  const url = !failed && src ? getMediaUrl(src) : "";
  const sizeClass = AVATAR_SIZE[size] || AVATAR_SIZE.md;
  return (
    <span className={`vfc-avatar ${sizeClass}${ring ? " vfc-avatar--ring" : ""}`} title={name || ""} style={url ? undefined : { background: avatarTone(name) }} aria-hidden="true">
      {url ? <img src={url} alt="" onError={() => setFailed(true)} /> : <span className="vfc-avatar-letters">{avatarLetters(name)}</span>}
      {status ? <i className={`vfc-avatar-dot is-${status}`} /> : null}
    </span>
  );
}

export function Badge({ tone = "neutral", children }) {
  return <span className={`vfc-badge vfc-badge--${tone}`}>{children}</span>;
}

export function Tabs({ label, value, onChange, items, id = "vfc-tab-line" }) {
  return (
    <div className="vfc-tabs" role="tablist" aria-label={label}>
      {items.map((item) => (
        <button key={item.id} type="button" role="tab" aria-selected={value === item.id} className={value === item.id ? "on" : ""} onClick={() => onChange(item.id)}>
          {item.label}
          {value === item.id ? <motion.span className="vfc-tab-line" layoutId={still() ? undefined : id} /> : null}
        </button>
      ))}
    </div>
  );
}

export function Stepper({ steps, current, onSelect, label }) {
  return (
    <ol className="vfc-stepper" aria-label={label}>
      {steps.map((step, index) => (
        <li key={step}>
          <button type="button" className={index === current ? "on" : index < current ? "done" : ""} aria-current={index === current ? "step" : undefined} onClick={() => onSelect(index)}>
            <span>{index + 1}</span>
            {step}
          </button>
        </li>
      ))}
    </ol>
  );
}
