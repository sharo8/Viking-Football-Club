"use client";

import { useEffect, useRef } from "react";
import { createPortal } from "react-dom";
import { motion } from "framer-motion";
import { AlertCircle, Loader2, X } from "lucide-react";

function still() {
  return typeof window !== "undefined" && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

export function Button({ variant = "secondary", size = "md", icon, loading, children, className = "", ...props }) {
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

export function Field({ label, required, error, hint, children }) {
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

export function SelectInput({ children, ...props }) {
  return <select className="vfc-control" {...props}>{children}</select>;
}

export function TextArea(props) {
  return <textarea className="vfc-control vfc-area" {...props} />;
}

export function Modal({ title, subtitle, onClose, size = "md", footer, children, closeLabel = "Close" }) {
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

export function Avatar({ name, src, size = "" }) {
  const letters = (name || "?").split(" ").filter(Boolean).slice(0, 2).map((part) => part[0]).join("").toUpperCase();
  return <span className={`vfc-avatar ${size ? `vfc-avatar--${size}` : ""}`}>{src ? <img src={src} alt="" /> : letters}</span>;
}
