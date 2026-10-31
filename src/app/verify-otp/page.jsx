"use client";

import { useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense } from "react";
import { API_URL } from "@/services/api";
import { useLocale } from "@/components/providers/LocaleProvider";

const COPY = {
  en: { title: "Enter the code", resend: "Resend code", wait: "Resend in", sent: "Code accepted" },
  fr: { title: "Entrez le code", resend: "Renvoyer le code", wait: "Renvoyer dans", sent: "Code accepté" },
  rw: { title: "Andika kode", resend: "Ongera wohereze", wait: "Ongera mu", sent: "Kode yemejwe" },
};

function OtpForm() {
  const { locale } = useLocale();
  const copy = COPY[locale] || COPY.en;
  const params = useSearchParams();
  const router = useRouter();
  const email = params.get("email") || "";
  const purpose = params.get("purpose") || "PASSWORD_RESET";
  const [digits, setDigits] = useState(["", "", "", "", "", ""]);
  const [error, setError] = useState("");
  const [ok, setOk] = useState(false);
  const [left, setLeft] = useState(0);
  const boxes = useRef([]);

  function write(index, value) {
    const next = digits.slice();
    next[index] = value.slice(-1);
    setDigits(next);
    if (value && boxes.current[index + 1]) boxes.current[index + 1].focus();
  }

  function onPaste(event) {
    const chars = event.clipboardData.getData("text").replace(/\D/g, "").slice(0, 6).split("");
    if (!chars.length) return;
    event.preventDefault();
    setDigits([...chars, "", "", "", "", ""].slice(0, 6));
  }

  async function verify(event) {
    event.preventDefault();
    const response = await fetch(`${API_URL}/api/auth/otp/verify`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, purpose, code: digits.join("") }),
    });
    const body = await response.json().catch(() => ({}));
    if (!response.ok || body.success === false) {
      setError(body.message || "That code is not valid");
      return;
    }
    setOk(true);
    window.setTimeout(() => router.push(purpose === "PASSWORD_RESET" ? `/forgot-password?email=${encodeURIComponent(email)}` : "/admin"), 900);
  }

  async function resend() {
    if (left > 0) return;
    await fetch(`${API_URL}/api/auth/otp/request`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, purpose }),
    });
    setLeft(60);
    const timer = window.setInterval(() => {
      setLeft((value) => {
        if (value <= 1) { window.clearInterval(timer); return 0; }
        return value - 1;
      });
    }, 1000);
  }

  return (
    <main className="login-screen login-screen--solo">
      <form className="card" onSubmit={verify} style={{ display: "grid", gap: 16, padding: 28 }}>
        <h1>{copy.title}</h1>
        <div style={{ display: "flex", gap: 8 }} onPaste={onPaste}>
          {digits.map((digit, index) => (
            <input
              key={index}
              ref={(node) => { boxes.current[index] = node; }}
              value={digit}
              inputMode="numeric"
              aria-label={`Digit ${index + 1}`}
              onChange={(event) => write(index, event.target.value.replace(/\D/g, ""))}
              onKeyDown={(event) => {
                if (event.key === "Backspace" && !digits[index] && boxes.current[index - 1]) boxes.current[index - 1].focus();
              }}
              style={{ width: 44, height: 52, textAlign: "center", fontSize: 22, borderRadius: 10 }}
            />
          ))}
        </div>
        {error ? <p role="alert">{error}</p> : null}
        {ok ? <p>{copy.sent}</p> : null}
        <button type="button" onClick={resend} disabled={left > 0}>{left ? `${copy.wait} 0:${String(left).padStart(2, "0")}` : copy.resend}</button>
        <button type="submit">OK</button>
      </form>
    </main>
  );
}

export default function VerifyOtpPage() {
  return <Suspense fallback={null}><OtpForm /></Suspense>;
}
