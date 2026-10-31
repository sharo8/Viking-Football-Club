"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { motion } from "framer-motion";
import { API_URL, IconEye, LogoEmblem } from "./Login";
import { readSession, writeSession } from "./session";
import { api } from "@/services/api";
import { useLocale } from "@/components/providers/LocaleProvider";
import { useAuth } from "@/components/providers/AuthProvider";
import { easeOutExpo, fadeUp, staggerContainer } from "./motion";
import "./Login.css";

function Reveal({ shown, onToggle, label, show = "Show", hide = "Hide" }) {
  return (
    <button
      type="button"
      className="icon-btn"
      aria-label={shown ? `${hide} ${label}` : `${show} ${label}`}
      aria-pressed={shown}
      onClick={onToggle}
    >
      <IconEye off={shown} />
    </button>
  );
}

function Frame({ title, subtitle, children, backLabel = "Back to sign in" }) {
  return (
    <div className="login-screen login-screen--solo">
      <motion.div
        className="card-shell"
        initial={{ opacity: 0, y: 28, scale: 0.98 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        transition={{ duration: 0.6, ease: easeOutExpo }}
      >
        <div className="login-card">
          <motion.div
            className="login-card__body"
            variants={staggerContainer(0.05, 0.07)}
            initial="hidden"
            animate="show"
          >
            <motion.div className="mobile-logo" style={{ display: "flex" }} variants={fadeUp}>
              <LogoEmblem labelled compact />
            </motion.div>
            <motion.h1 className="card-title" variants={fadeUp}>
              {title}
            </motion.h1>
            <motion.p className="card-sub" variants={fadeUp}>
              {subtitle}
            </motion.p>
            {children}
            <motion.div className="foot-links" variants={fadeUp}>
              <Link href="/login" className="back-link">
                <svg className="arrow" width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                  <path d="M15 6L9 12l6 6" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
                {backLabel}
              </Link>
            </motion.div>
          </motion.div>
        </div>
      </motion.div>
    </div>
  );
}

export function ForgotPassword() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [token, setToken] = useState("");
  const [code, setCode] = useState("");
  const [sent, setSent] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  const onRequest = async (event) => {
    event.preventDefault();
    setError("");
    setMessage("");
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
      setError("Enter a valid email address");
      return;
    }
    setBusy(true);
    try {
      const response = await fetch(`${API_URL}/api/auth/forgot-password`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: email.trim() }),
      });
      const body = await response.json().catch(() => ({}));
      if (!response.ok || body.success === false) {
        throw new Error(body.message || "Could not start a reset");
      }
      setSent(true);
      setMessage(body.message || "If that email belongs to a club account, a code is on its way.");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not start a reset");
    } finally {
      setBusy(false);
    }
  };

  const onReset = async (event) => {
    event.preventDefault();
    setError("");
    setBusy(true);
    try {
      const response = await fetch(`${API_URL}/api/auth/reset-password`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: email.trim(), otp: code, token, newPassword: password }),
      });
      const body = await response.json().catch(() => ({}));
      if (!response.ok || body.success === false) {
        throw new Error(body.message || "Could not update the password");
      }
      setMessage("Password updated. Sign in with the new one.");
      setToken("");
      setPassword("");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not update the password");
    } finally {
      setBusy(false);
    }
  };

  return (
    <Frame title="Reset password" subtitle="We will prepare a reset code for this email.">
      <motion.form className="form" onSubmit={sent ? onReset : onRequest} noValidate variants={fadeUp}>
        {error ? (
          <div className="banner" role="alert" aria-live="polite">
            <div className="banner__inner">{error}</div>
          </div>
        ) : null}
        {message ? <p className="card-sub">{message}</p> : null}
        {!sent ? (
          <div className="field">
            <label htmlFor="reset-email">Email</label>
            <div className="control">
              <input
                id="reset-email"
                type="email"
                autoComplete="username"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
              />
              <span className="underline" aria-hidden="true" />
            </div>
          </div>
        ) : (
          <>
          <div className="field">
            <label htmlFor="reset-code">6-digit code</label>
            <div className="control">
              <input id="reset-code" inputMode="numeric" autoComplete="one-time-code" maxLength={6} value={code} onChange={(event) => setCode(event.target.value.replace(/\D/g, "").slice(0, 6))} />
              <span className="underline" aria-hidden="true" />
            </div>
          </div>
          <div className="field">
            <label htmlFor="new-password">New password</label>
            <div className="control">
              <input
                id="new-password"
                type={showPassword ? "text" : "password"}
                autoComplete="new-password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
              />
              <Reveal shown={showPassword} onToggle={() => setShowPassword((current) => !current)} label="new password" />
              <span className="underline" aria-hidden="true" />
            </div>
            <p className="field-msg" style={{ color: "var(--muted)" }}>
              8+ characters with upper, lower, a digit and a symbol.
            </p>
          </div>
          </>
        )}
        <button type="submit" className="submit" disabled={busy}>
          <span className="submit-face">{busy ? "Please wait…" : sent ? "Update password" : "Send reset code"}</span>
        </button>
      </motion.form>
    </Frame>
  );
}

const RULES = [
  ["length", (value) => value.length >= 8, "ruleLength"],
  ["upper", (value) => /[A-Z]/.test(value), "ruleUpper"],
  ["lower", (value) => /[a-z]/.test(value), "ruleLower"],
  ["digit", (value) => /\d/.test(value), "ruleDigit"],
  ["symbol", (value) => /[^A-Za-z0-9]/.test(value), "ruleSymbol"],
];

function passwordStrong(value) {
  return RULES.every(([, test]) => test(value));
}

export function ChangePassword() {
  const router = useRouter();
  const { setUser } = useAuth();
  const { locale, setLocale, t } = useLocale();
  const copy = t.password;
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [error, setError] = useState("");
  const [fields, setFields] = useState({});
  const [busy, setBusy] = useState(false);
  const [toast, setToast] = useState("");
  const [showCurrent, setShowCurrent] = useState(false);
  const [showNew, setShowNew] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);

  const clearField = (key) => setFields((current) => ({ ...current, [key]: undefined }));

  const onSubmit = async (event) => {
    event.preventDefault();
    setError("");
    const nextFields = {};
    if (!currentPassword) nextFields.current = copy.required;
    if (!newPassword) nextFields.next = copy.required;
    else if (!passwordStrong(newPassword)) nextFields.next = copy.weak;
    else if (newPassword === currentPassword) nextFields.next = copy.same;
    if (!confirm) nextFields.confirm = copy.required;
    else if (confirm !== newPassword) nextFields.confirm = copy.mismatch;
    setFields(nextFields);
    if (Object.keys(nextFields).length) return;

    const session = readSession();
    if (!session?.accessToken) {
      setError(copy.signInFirst);
      return;
    }
    setBusy(true);
    try {
      const response = await api.post("/api/auth/change-password", { currentPassword, newPassword });
      const data = response.data?.data || {};
      const remembered = Boolean(window.localStorage.getItem("vikingfc.accessToken"));
      writeSession(
        {
          accessToken: data.accessToken,
          refreshToken: data.refreshToken,
          user: {
            ...(session.user || {}),
            email: data.email || session.user?.email,
            fullName: data.fullName || session.user?.fullName,
            role: data.role || session.user?.role,
            mustChangePassword: false,
          },
        },
        remembered,
      );
      setUser({
        fullName: data.fullName || session.user?.fullName || "",
        email: data.email || session.user?.email || "",
        role: data.role || session.user?.role || "",
        phone: "",
        photo: null,
        mustChangePassword: false,
      });
      setToast(copy.updated);
      window.setTimeout(() => {
        setToast("");
        router.push("/admin");
      }, 4000);
    } catch (err) {
      const code = err?.code || "";
      if (code === "INVALID_CURRENT_PASSWORD") setFields({ current: copy.wrongCurrent });
      else if (code === "PASSWORD_REUSE") setFields({ next: copy.same });
      else if (code === "WEAK_PASSWORD") setFields({ next: copy.weak });
      else setError(err instanceof Error ? err.message : copy.weak);
    } finally {
      setBusy(false);
    }
  };

  return (
    <Frame title={copy.title} subtitle={copy.subtitle} backLabel={copy.back}>
      <div className="lang-switch" role="group" aria-label="Language">
        {["en", "fr", "rw"].map((code) => (
          <button
            key={code}
            type="button"
            className={locale === code ? "is-on" : ""}
            aria-pressed={locale === code}
            onClick={() => setLocale(code)}
          >
            {code.toUpperCase()}
          </button>
        ))}
      </div>
      <motion.form className="form" onSubmit={onSubmit} noValidate variants={fadeUp}>
        {error ? (
          <div className="banner" role="alert" aria-live="polite">
            <div className="banner__inner">{error}</div>
          </div>
        ) : null}
        <div className={`field${fields.current ? " has-error" : ""}`}>
          <label htmlFor="current-password">{copy.current}</label>
          <div className="control">
            <input
              id="current-password"
              type={showCurrent ? "text" : "password"}
              autoComplete="current-password"
              value={currentPassword}
              aria-invalid={fields.current ? "true" : "false"}
              aria-describedby={fields.current ? "current-password-error" : undefined}
              onChange={(event) => {
                setCurrentPassword(event.target.value);
                clearField("current");
              }}
            />
            <Reveal shown={showCurrent} onToggle={() => setShowCurrent((current) => !current)} label={copy.current} show={copy.show} hide={copy.hide} />
            <span className="underline" aria-hidden="true" />
          </div>
          {fields.current ? <p id="current-password-error" className="field-msg">{fields.current}</p> : null}
        </div>
        <div className={`field${fields.next ? " has-error" : ""}`}>
          <label htmlFor="fresh-password">{copy.next}</label>
          <div className="control">
            <input
              id="fresh-password"
              type={showNew ? "text" : "password"}
              autoComplete="new-password"
              value={newPassword}
              aria-invalid={fields.next ? "true" : "false"}
              aria-describedby="password-rules"
              onChange={(event) => {
                setNewPassword(event.target.value);
                clearField("next");
              }}
            />
            <Reveal shown={showNew} onToggle={() => setShowNew((current) => !current)} label={copy.next} show={copy.show} hide={copy.hide} />
            <span className="underline" aria-hidden="true" />
          </div>
          {fields.next ? <p className="field-msg">{fields.next}</p> : null}
          <ul id="password-rules" className="pw-rules">
            {RULES.map(([key, test, label]) => {
              const ok = test(newPassword);
              return (
                <li key={key} className={ok ? "is-ok" : ""}>
                  <motion.span
                    aria-hidden="true"
                    initial={false}
                    animate={{ scale: ok ? 1 : 0.86, opacity: ok ? 1 : 0.55 }}
                    transition={{ type: "spring", stiffness: 420, damping: 24 }}
                  >
                    {ok ? "✓" : "•"}
                  </motion.span>
                  {copy[label]}
                </li>
              );
            })}
          </ul>
        </div>
        <div className={`field${fields.confirm ? " has-error" : ""}`}>
          <label htmlFor="confirm-password">{copy.confirm}</label>
          <div className="control">
            <input
              id="confirm-password"
              type={showConfirm ? "text" : "password"}
              autoComplete="new-password"
              value={confirm}
              aria-invalid={fields.confirm ? "true" : "false"}
              aria-describedby={fields.confirm ? "confirm-password-error" : undefined}
              onChange={(event) => {
                setConfirm(event.target.value);
                clearField("confirm");
              }}
            />
            <Reveal shown={showConfirm} onToggle={() => setShowConfirm((current) => !current)} label={copy.confirm} show={copy.show} hide={copy.hide} />
            <span className="underline" aria-hidden="true" />
          </div>
          {fields.confirm ? <p id="confirm-password-error" className="field-msg">{fields.confirm}</p> : null}
        </div>
        <button type="submit" className="submit" disabled={busy}>
          <span className="submit-face">{busy ? copy.saving : copy.save}</span>
        </button>
      </motion.form>
      {toast ? <p className="auth-toast" role="status">{toast}</p> : null}
    </Frame>
  );
}
