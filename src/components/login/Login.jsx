"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  AnimatePresence,
  MotionConfig,
  motion,
  useAnimation,
  useMotionValue,
  useSpring,
  useTransform,
} from "framer-motion";
import ParticlesBackground from "./ParticlesBackground";
import { writeSession } from "./session";
import { useLocale } from "@/components/providers/LocaleProvider";
import {
  PARTICLES_ENABLED,
  SPEED,
  easeOutExpo,
  fadeUp,
  shake,
  springSoft,
  staggerContainer,
  useReducedMotionSafe,
} from "./motion";
import "./Login.css";

export const LOGO_SRC = "/logo/viking-fc.png";

export const API_URL =
  process.env.NEXT_PUBLIC_API_URL ||
  process.env.REACT_APP_API_URL ||
  "http://localhost:8080";

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const wait = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

function IconMail() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <rect x="3" y="5" width="18" height="14" rx="2" stroke="currentColor" strokeWidth="1.7" />
      <path d="M4 7l8 6 8-6" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function IconLock() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <rect x="5" y="11" width="14" height="9" rx="2" stroke="currentColor" strokeWidth="1.7" />
      <path d="M8 11V8a4 4 0 0 1 8 0v3" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
    </svg>
  );
}

export function IconEye({ off = false }) {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path d="M2.5 12S6.5 6.5 12 6.5 21.5 12 21.5 12 17.5 17.5 12 17.5 2.5 12 2.5 12Z" stroke="currentColor" strokeWidth="1.7" />
      <circle cx="12" cy="12" r="2.5" stroke="currentColor" strokeWidth="1.7" />
      {off ? <path d="M5 19L19 5" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" /> : null}
    </svg>
  );
}

function IconAlert() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <circle cx="12" cy="12" r="9" stroke="currentColor" strokeWidth="1.7" />
      <path d="M12 8v5" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
      <circle cx="12" cy="16.5" r="0.9" fill="currentColor" />
    </svg>
  );
}

function IconArrow() {
  return (
    <svg className="arrow" width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path d="M15 6L9 12l6 6" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function CheckPath({ className, size = 18 }) {
  return (
    <motion.svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <motion.path
        d="M5 12.5 9.2 16.5 19 7.5"
        stroke="currentColor"
        strokeWidth="2.2"
        strokeLinecap="round"
        strokeLinejoin="round"
        initial={{ pathLength: 0 }}
        animate={{ pathLength: 1 }}
        transition={{ duration: 0.35 * SPEED, ease: easeOutExpo }}
      />
    </motion.svg>
  );
}

function CrestMark() {
  return (
    <svg className="crest" viewBox="0 0 80 80" aria-hidden="true">
      <path d="M40 6 64 16v20c0 16.5-10.2 27.2-24 34C26.2 63.2 16 52.5 16 36V16L40 6Z" fill="#7d1c2b" stroke="#e0707f" strokeWidth="1.5" />
      <path d="M27 30 40 52 53 30" fill="none" stroke="#f1f3f5" strokeWidth="3.2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function ClubMark({ labelled = false }) {
  const [phase, setPhase] = useState("pending");
  const reveal = (node) => {
    if (node && node.complete && node.naturalWidth > 0) setPhase("photo");
  };

  return (
    <>
      {phase !== "photo" ? <CrestMark /> : null}
      {phase !== "fail" ? (
        <img
          ref={reveal}
          src={LOGO_SRC}
          alt={labelled && phase === "photo" ? "Viking FC" : ""}
          className="logo-img"
          style={{ display: phase === "photo" ? "block" : "none" }}
          onLoad={() => setPhase("photo")}
          onError={() => setPhase("fail")}
        />
      ) : null}
    </>
  );
}

export function LogoEmblem({ labelled = false, compact = false }) {
  const reduced = useReducedMotionSafe();
  const tiltX = useMotionValue(0);
  const tiltY = useMotionValue(0);
  const springX = useSpring(tiltX, { stiffness: 180, damping: 18 });
  const springY = useSpring(tiltY, { stiffness: 180, damping: 18 });
  const rotateX = useTransform(springY, [-0.5, 0.5], [10, -10]);
  const rotateY = useTransform(springX, [-0.5, 0.5], [-10, 10]);

  const onTilt = (event) => {
    if (reduced) return;
    const rect = event.currentTarget.getBoundingClientRect();
    tiltX.set((event.clientX - rect.left) / rect.width - 0.5);
    tiltY.set((event.clientY - rect.top) / rect.height - 0.5);
  };

  return (
    <motion.div
      className={compact ? "logo-emblem logo-emblem--sm" : "logo-emblem"}
      style={reduced ? undefined : { rotateX, rotateY }}
      initial={reduced ? { opacity: 0 } : { scale: 0.6, rotate: -8, opacity: 0 }}
      animate={{ scale: 1, rotate: 0, opacity: 1 }}
      transition={reduced ? { duration: 0.25 } : springSoft}
      onMouseMove={onTilt}
      onMouseLeave={() => {
        tiltX.set(0);
        tiltY.set(0);
      }}
    >
      <span className="logo-emblem__spin" aria-hidden="true">
        <span className="logo-emblem__orbit" />
        <span className="logo-emblem__dot" />
      </span>
      <span className="logo-emblem__face">
        <ClubMark labelled={labelled} />
      </span>
    </motion.div>
  );
}

function useCountUp(target, duration = 1600) {
  const reduced = useReducedMotionSafe();
  const [value, setValue] = useState(0);

  useEffect(() => {
    if (reduced) {
      setValue(target);
      return undefined;
    }
    let frame = 0;
    const start = performance.now();
    const step = (now) => {
      const progress = Math.min(1, (now - start) / (duration * SPEED));
      const eased = 1 - (1 - progress) ** 3;
      setValue(Math.round(target * eased));
      if (progress < 1) frame = requestAnimationFrame(step);
    };
    frame = requestAnimationFrame(step);
    return () => cancelAnimationFrame(frame);
  }, [target, duration, reduced]);

  return value;
}

function BrandPanel({ reduced }) {
  const [wide, setWide] = useState(true);
  const players = useCountUp(25);
  const division = useCountUp(3);
  const season = useCountUp(2026);
  const letter = useMemo(
    () =>
      reduced
        ? { hidden: { opacity: 0 }, show: { opacity: 1, transition: { duration: 0.25 } } }
        : {
            hidden: { opacity: 0, y: 30, filter: "blur(8px)" },
            show: {
              opacity: 1,
              y: 0,
              filter: "blur(0px)",
              transition: { duration: 0.5 * SPEED, ease: easeOutExpo },
            },
          },
    [reduced],
  );

  useEffect(() => {
    const query = window.matchMedia("(max-width: 900px)");
    const apply = () => setWide(!query.matches);
    apply();
    query.addEventListener("change", apply);
    return () => query.removeEventListener("change", apply);
  }, []);

  return (
    <aside className="brand" aria-hidden="false">
      <div className="aurora" aria-hidden="true">
        <motion.div
          className="blob blob-a"
          animate={reduced ? undefined : { x: [0, 70, -30], y: [0, -50, 30], scale: [1, 1.12, 0.96] }}
          transition={{ duration: 22 * SPEED, repeat: Infinity, repeatType: "mirror", ease: "easeInOut" }}
        />
        <motion.div
          className="blob blob-b"
          animate={reduced ? undefined : { x: [0, -60, 40], y: [0, 36, -24], scale: [1, 0.94, 1.1] }}
          transition={{ duration: 18 * SPEED, repeat: Infinity, repeatType: "mirror", ease: "easeInOut" }}
        />
      </div>
      {PARTICLES_ENABLED && !reduced && wide ? <ParticlesBackground /> : null}
      <div className="brand-grid" aria-hidden="true" />
      <div className="brand-copy">
        <div className="logo-stage">
          <LogoEmblem />
        </div>

        <motion.p
          className="wordmark"
          aria-label="Viking FC"
          variants={staggerContainer(0.15, 0.05)}
          initial="hidden"
          animate="show"
        >
          {"VIKING FC".split("").map((char, index) => (
            <motion.span
              key={`${char}-${index}`}
              aria-hidden="true"
              className={char === " " ? "wordmark-gap" : undefined}
              variants={letter}
            >
              {char === " " ? "" : char}
            </motion.span>
          ))}
          <span className="wordmark-shine" aria-hidden="true" />
        </motion.p>

        <motion.p
          className="tagline"
          variants={staggerContainer(0.85, 0.25)}
          initial="hidden"
          animate="show"
        >
          {["ONE CLUB", "ONE FAMILY", "ONE MISSION"].map((phrase, index) => (
            <motion.span
              key={phrase}
              variants={fadeUp}
              style={{ display: "inline-flex", alignItems: "center", gap: 12 }}
            >
              {index > 0 ? (
                <motion.span
                  className="sep"
                  aria-hidden="true"
                  variants={{
                    hidden: { scaleX: 0, opacity: 0 },
                    show: { scaleX: 1, opacity: 1, transition: { duration: 0.35 * SPEED, ease: easeOutExpo } },
                  }}
                  style={{ originX: 0 }}
                />
              ) : null}
              {phrase}
            </motion.span>
          ))}
        </motion.p>

        <motion.span
          className="divider"
          aria-hidden="true"
          initial={{ width: 0 }}
          animate={{ width: 64 }}
          transition={{ delay: 1.45 * SPEED, duration: 0.55 * SPEED, ease: easeOutExpo }}
        />

        <motion.p
          className="description"
          initial={{ opacity: 0, y: reduced ? 0 : 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 1.65 * SPEED, duration: 0.55 * SPEED, ease: easeOutExpo }}
        >
          The staff door to Viking FC. Squad, training, matches and the 2026/2027 season, kept in one place.
        </motion.p>
      </div>

      <div className="brand-bottom">
        <motion.div
          className="stats"
          initial={{ opacity: 0, y: reduced ? 0 : 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 1.85 * SPEED, duration: 0.5 * SPEED, ease: easeOutExpo }}
        >
          <div className="stat">
            <b>{players}</b>
            <span>Players</span>
          </div>
          <div className="stat">
            <b>{division === 3 ? "3rd" : division}</b>
            <span>Division</span>
          </div>
          <div className="stat">
            <b>{season === 2026 ? "2026/2027" : season}</b>
            <span>Season</span>
          </div>
        </motion.div>
        <motion.p
          className="place"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 2.15 * SPEED, duration: 0.45 * SPEED }}
        >
          Kigali, Rwanda
        </motion.p>
      </div>
    </aside>
  );
}

export function OtpFields({ id, value, onChange }) {
  const refs = useRef([]);
  const digits = Array.from({ length: 6 }, (_, index) => value[index] || "");

  function write(next, focusIndex) {
    const clean = next.replace(/\D/g, "").slice(0, 6);
    onChange(clean);
    const target = Math.min(Math.max(focusIndex, 0), 5);
    window.requestAnimationFrame(() => refs.current[target]?.focus());
  }

  return (
    <div className="otp-boxes" role="group">
      {digits.map((digit, index) => (
        <input
          key={index}
          id={index === 0 ? id : undefined}
          ref={(node) => { refs.current[index] = node; }}
          className={digit ? "otp-box is-filled" : "otp-box"}
          inputMode="numeric"
          autoComplete={index === 0 ? "one-time-code" : "off"}
          aria-label={`Digit ${index + 1}`}
          value={digit}
          onChange={(event) => {
            const raw = event.target.value.replace(/\D/g, "");
            if (!raw) {
              write(digits.map((item, itemIndex) => (itemIndex === index ? "" : item)).join(""), index);
              return;
            }
            if (raw.length > 1) {
              write(raw, Math.min(raw.length, 5));
              return;
            }
            const next = digits.slice();
            next[index] = raw;
            write(next.join(""), index + 1);
          }}
          onKeyDown={(event) => {
            if (event.key === "Backspace" && !digit) {
              event.preventDefault();
              write(digits.map((item, itemIndex) => (itemIndex === index - 1 ? "" : item)).join(""), index - 1);
            }
            if (event.key === "ArrowLeft") refs.current[index - 1]?.focus();
            if (event.key === "ArrowRight") refs.current[index + 1]?.focus();
          }}
          onPaste={(event) => {
            const raw = event.clipboardData.getData("text");
            if (!/\d/.test(raw)) return;
            event.preventDefault();
            write(raw, 5);
          }}
        />
      ))}
    </div>
  );
}

export default function Login() {
  const router = useRouter();
  const { t } = useLocale();
  const reduced = useReducedMotionSafe();
  const [mounted, setMounted] = useState(false);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [remember, setRemember] = useState(true);
  const [caps, setCaps] = useState(false);
  const [errors, setErrors] = useState({});
  const [banner, setBanner] = useState("");
  const [flash, setFlash] = useState(false);
  const [phase, setPhase] = useState("idle");
  const [challenge, setChallenge] = useState("");
  const [masked, setMasked] = useState("");
  const [otp, setOtp] = useState("");
  const [wide, setWide] = useState(true);
  const busy = useRef(false);
  const shakeControls = useAnimation();

  useEffect(() => {
    setMounted(true);
    const query = window.matchMedia("(max-width: 900px)");
    const apply = () => setWide(!query.matches);
    apply();
    query.addEventListener("change", apply);
    return () => query.removeEventListener("change", apply);
  }, []);

  const emailValid = EMAIL_PATTERN.test(email.trim());
  const motionOff = !mounted || reduced;

  const onCardMove = (event) => {
    const rect = event.currentTarget.getBoundingClientRect();
    const x = ((event.clientX - rect.left) / rect.width) * 100;
    const y = ((event.clientY - rect.top) / rect.height) * 100;
    event.currentTarget.style.setProperty("--mx", `${x}%`);
    event.currentTarget.style.setProperty("--my", `${y}%`);
  };

  const playShake = () => {
    setFlash(true);
    window.setTimeout(() => setFlash(false), 480);
    if (!motionOff) shakeControls.start(shake);
  };

  const syncCaps = (event) => {
    if (typeof event.getModifierState === "function") {
      setCaps(event.getModifierState("CapsLock"));
    }
  };

  const succeed = async (path) => {
    setPhase("success");
    await wait(motionOff ? 180 : 720);
    setPhase("leave");
    await wait(motionOff ? 80 : 450);
    router.push(path);
  };

  const onSubmit = async (event) => {
    event.preventDefault();
    if (busy.current) return;

    const nextErrors = {};
    if (!email.trim()) nextErrors.email = "Email is required";
    else if (!emailValid) nextErrors.email = "Enter a valid email address";
    if (!password) nextErrors.password = "Password is required";
    setErrors(nextErrors);
    setBanner("");

    if (Object.keys(nextErrors).length) {
      playShake();
      return;
    }

    busy.current = true;
    setPhase("loading");

    try {
      const response = await fetch(`${API_URL}/api/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: email.trim(), password }),
      });
      const body = await response.json().catch(() => ({}));
      if (!response.ok || body.success === false) {
        throw new Error(body.message || "Those details were not accepted");
      }
      const data = body.data ?? body;
      if (data.otpRequired) {
        busy.current = false;
        setPhase("idle");
        setChallenge(data.challengeId);
        setMasked(data.maskedEmail || "");
        setOtp("");
        return;
      }
      if (!data.accessToken) throw new Error("The server did not return a session");
      const user = data.user ?? {
        email: data.email ?? email.trim(),
        fullName: data.fullName ?? "",
        role: data.role ?? "",
        executive: Boolean(data.executive),
        mustChangePassword: Boolean(data.mustChangePassword),
      };
      writeSession(
        { accessToken: data.accessToken, refreshToken: data.refreshToken, user },
        remember,
      );
      const nextPath = user.mustChangePassword ? "/change-password" : user.role === "PLAYER" ? "/player" : "/admin";
      await succeed(nextPath);
    } catch (error) {
      busy.current = false;
      setPhase("idle");
      setBanner(error instanceof Error ? error.message : "Sign-in failed");
      playShake();
    }
  };

  return (
    <MotionConfig reducedMotion="user">
      <motion.div
        className="login-screen"
        initial={{ opacity: 0 }}
        animate={{ opacity: phase === "leave" ? 0 : 1 }}
        transition={{ duration: phase === "leave" ? 0.45 : 0.35, ease: easeOutExpo }}
      >
        {!wide && PARTICLES_ENABLED && !motionOff ? (
          <div className="mobile-particles">
            <ParticlesBackground />
          </div>
        ) : null}
        <div className="login-grid">
          <BrandPanel reduced={motionOff} />
          <section className="pane">
            <motion.div
              className="card-shell"
              initial={motionOff ? { opacity: 0 } : { y: 40, opacity: 0, scale: 0.97 }}
              animate={
                phase === "leave"
                  ? { y: 0, opacity: 0, scale: 1.04 }
                  : { y: 0, opacity: 1, scale: 1 }
              }
              transition={{
                duration: phase === "leave" ? 0.45 : 0.7,
                ease: easeOutExpo,
              }}
            >
              <motion.div
                className={`login-card${flash ? " is-flash" : ""}`}
                animate={shakeControls}
                onMouseMove={onCardMove}
              >
                <motion.div
                  className="login-card__body"
                  variants={staggerContainer(0.08, 0.08)}
                  initial="hidden"
                  animate="show"
                >
                  <motion.div className="mobile-logo" variants={fadeUp}>
                    <LogoEmblem labelled compact />
                  </motion.div>
                  <motion.h1 className="card-title" variants={fadeUp}>
                    Sign in
                  </motion.h1>
                  <motion.p className="card-sub" variants={fadeUp}>
                    Viking FC club portal
                  </motion.p>

                  <AnimatePresence initial={false}>
                    {banner ? (
                      <motion.div
                        key="banner"
                        className="banner"
                        role="alert"
                        aria-live="polite"
                        initial={{ height: 0, opacity: 0 }}
                        animate={{ height: "auto", opacity: 1 }}
                        exit={{ height: 0, opacity: 0 }}
                        transition={{ duration: 0.28, ease: easeOutExpo }}
                      >
                        <div className="banner__inner">
                          <IconAlert />
                          <span>{banner}</span>
                        </div>
                      </motion.div>
                    ) : null}
                  </AnimatePresence>

                  {challenge ? (
                    <form
                      className="form"
                      onSubmit={async (event) => {
                        event.preventDefault();
                        if (busy.current) return;
                        busy.current = true;
                        setPhase("loading");
                        setBanner("");
                        try {
                          const response = await fetch(`${API_URL}/api/auth/otp/verify-login`, {
                            method: "POST",
                            headers: { "Content-Type": "application/json" },
                            body: JSON.stringify({ challengeId: challenge, code: otp }),
                          });
                          const body = await response.json().catch(() => ({}));
                          if (!response.ok || body.success === false) throw new Error(body.message || "That code was not accepted");
                          const data = body.data ?? body;
                          const user = {
                            email: data.email ?? email.trim(),
                            fullName: data.fullName ?? "",
                            role: data.role ?? "",
                            executive: Boolean(data.executive),
                            mustChangePassword: Boolean(data.mustChangePassword),
                          };
                          writeSession({ accessToken: data.accessToken, refreshToken: data.refreshToken, user }, remember);
                          const nextPath = user.mustChangePassword ? "/change-password" : user.role === "PLAYER" ? "/player" : "/admin";
                          await succeed(nextPath);
                        } catch (error) {
                          busy.current = false;
                          setPhase("idle");
                          setBanner(error instanceof Error ? error.message : "That code was not accepted");
                          playShake();
                        }
                      }}
                    >
                      <p className="card-sub">Enter the 6-digit code sent to {masked}</p>
                      <div className="field">
                        <label htmlFor="otp">Code</label>
                        <OtpFields id="otp" value={otp} onChange={setOtp} />
                      </div>
                      <button className="submit" type="submit" disabled={otp.length !== 6 || phase === "loading"}>Confirm</button>
                      <div style={{ display: "flex", gap: 16, marginTop: 8 }}>
                      <button
                        type="button"
                        className="text-link"
                        onClick={async () => {
                          setBanner("");
                          const response = await fetch(`${API_URL}/api/auth/otp/resend-login`, {
                            method: "POST",
                            headers: { "Content-Type": "application/json" },
                            body: JSON.stringify({ challengeId: challenge }),
                          });
                          const body = await response.json().catch(() => ({}));
                          if (!response.ok || body.success === false) setBanner(body.message || "Wait a minute before asking for another code");
                        }}
                      >
                        Resend code
                      </button>
                      <button type="button" className="text-link" onClick={() => { setChallenge(""); setOtp(""); setBanner(""); }}>Back to login</button>
                      </div>
                    </form>
                  ) : (
                  <motion.form
                    className="form"
                    onSubmit={onSubmit}
                    noValidate
                    variants={staggerContainer(0, 0.08)}
                  >
                    <motion.div className={`field${errors.email ? " has-error" : ""}`} variants={fadeUp}>
                      <label htmlFor="email">Email</label>
                      <div className="control">
                        <span className="field-icon">
                          <IconMail />
                        </span>
                        <input
                          id="email"
                          name="email"
                          type="email"
                          autoComplete="username"
                          value={email}
                          onChange={(event) => {
                            setEmail(event.target.value);
                            setErrors((current) => ({ ...current, email: undefined }));
                          }}
                          aria-invalid={errors.email ? "true" : "false"}
                          aria-describedby={errors.email ? "email-error" : undefined}
                        />
                        <AnimatePresence>
                          {emailValid ? (
                            <motion.span
                              key="email-ok"
                              className="field-check"
                              initial={{ opacity: 0, scale: 0.7 }}
                              animate={{ opacity: 1, scale: 1 }}
                              exit={{ opacity: 0, scale: 0.7 }}
                            >
                              <CheckPath />
                            </motion.span>
                          ) : null}
                        </AnimatePresence>
                        <span className="underline" aria-hidden="true" />
                      </div>
                      <AnimatePresence initial={false}>
                        {errors.email ? (
                          <motion.p
                            id="email-error"
                            className="field-msg"
                            initial={{ height: 0, opacity: 0 }}
                            animate={{ height: "auto", opacity: 1 }}
                            exit={{ height: 0, opacity: 0 }}
                          >
                            {errors.email}
                          </motion.p>
                        ) : null}
                      </AnimatePresence>
                    </motion.div>

                    <motion.div className={`field${errors.password ? " has-error" : ""}`} variants={fadeUp}>
                      <label htmlFor="password">Password</label>
                      <div className="control">
                        <span className="field-icon">
                          <IconLock />
                        </span>
                        <input
                          id="password"
                          name="password"
                          type={showPassword ? "text" : "password"}
                          autoComplete="current-password"
                          value={password}
                          onChange={(event) => {
                            setPassword(event.target.value);
                            setErrors((current) => ({ ...current, password: undefined }));
                          }}
                          onKeyDown={syncCaps}
                          onKeyUp={syncCaps}
                          onBlur={() => setCaps(false)}
                          aria-invalid={errors.password ? "true" : "false"}
                          aria-describedby={errors.password ? "password-error" : undefined}
                        />
                        <button
                          type="button"
                          className="icon-btn"
                          aria-label={showPassword ? "Hide password" : "Show password"}
                          onClick={() => setShowPassword((current) => !current)}
                        >
                          <AnimatePresence mode="wait" initial={false}>
                            <motion.span
                              key={showPassword ? "hide" : "show"}
                              initial={{ opacity: 0, rotate: -18 }}
                              animate={{ opacity: 1, rotate: 0 }}
                              exit={{ opacity: 0, rotate: 18 }}
                              transition={{ duration: 0.15 }}
                              style={{ display: "grid" }}
                            >
                              <IconEye off={showPassword} />
                            </motion.span>
                          </AnimatePresence>
                        </button>
                        <span className="underline" aria-hidden="true" />
                      </div>
                      <AnimatePresence initial={false}>
                        {caps ? (
                          <motion.p
                            className="caps"
                            initial={{ height: 0, opacity: 0 }}
                            animate={{ height: "auto", opacity: 1 }}
                            exit={{ height: 0, opacity: 0 }}
                          >
                            Caps Lock is on
                          </motion.p>
                        ) : null}
                      </AnimatePresence>
                      <AnimatePresence initial={false}>
                        {errors.password ? (
                          <motion.p
                            id="password-error"
                            className="field-msg"
                            initial={{ height: 0, opacity: 0 }}
                            animate={{ height: "auto", opacity: 1 }}
                            exit={{ height: 0, opacity: 0 }}
                          >
                            {errors.password}
                          </motion.p>
                        ) : null}
                      </AnimatePresence>
                    </motion.div>

                    <motion.div className="row" variants={fadeUp}>
                      <label className="remember">
                        <input
                          type="checkbox"
                          checked={remember}
                          onChange={(event) => setRemember(event.target.checked)}
                        />
                        <motion.span
                          className="box"
                          aria-hidden="true"
                          animate={remember && !motionOff ? { scale: [1, 1.12, 1] } : { scale: 1 }}
                          transition={{ duration: 0.28 }}
                        >
                          {remember ? <CheckPath size={14} /> : null}
                        </motion.span>
                        Remember me
                      </label>
                      <Link href="/forgot-password" className="text-link">
                        Forgot password?
                      </Link>
                    </motion.div>

                    <motion.div variants={fadeUp}>
                      <motion.button
                        type="submit"
                        className="submit"
                        disabled={phase !== "idle"}
                        whileHover={phase === "idle" && !motionOff ? { y: -2 } : undefined}
                        whileTap={phase === "idle" ? { scale: 0.97 } : undefined}
                        aria-busy={phase === "loading"}
                      >
                        <span className="submit-shine" aria-hidden="true" />
                        <span className="submit-face">
                          <AnimatePresence mode="wait" initial={false}>
                            {phase === "success" || phase === "leave" ? (
                              <motion.span
                                key="ok"
                                initial={{ opacity: 0, scale: 0.8 }}
                                animate={{ opacity: 1, scale: 1 }}
                                exit={{ opacity: 0 }}
                                style={{ color: "#f4fff8", filter: "drop-shadow(0 0 6px #3dd68c)" }}
                              >
                                <CheckPath />
                              </motion.span>
                            ) : phase === "loading" ? (
                              <motion.span
                                key="spin"
                                initial={{ opacity: 0 }}
                                animate={{ opacity: 1 }}
                                exit={{ opacity: 0 }}
                              >
                                {motionOff ? "Signing in…" : <Spinner />}
                              </motion.span>
                            ) : (
                              <motion.span key="label" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
                                Sign in
                              </motion.span>
                            )}
                          </AnimatePresence>
                        </span>
                      </motion.button>
                    </motion.div>

                    <motion.p className="switch" variants={fadeUp}>
                      New to the club?{" "}
                      <Link href="/register" className="text-link">
                        Sign up
                      </Link>
                    </motion.p>
                  </motion.form>
                  )}

                  <motion.div className="foot-links" variants={fadeUp}>
                    <Link href="/" className="back-link">
                      <IconArrow />
                      Back to website
                    </Link>
                  </motion.div>
                </motion.div>
              </motion.div>
            </motion.div>
          </section>
        </div>
      </motion.div>
    </MotionConfig>
  );
}

function Spinner() {
  return (
    <svg className="spinner" viewBox="0 0 24 24" aria-hidden="true">
      <circle cx="12" cy="12" r="8" fill="none" stroke="currentColor" strokeWidth="2" opacity="0.25" />
      <path d="M12 4a8 8 0 0 1 8 8" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
    </svg>
  );
}
