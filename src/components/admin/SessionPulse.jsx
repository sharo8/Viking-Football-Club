"use client";

import { useEffect, useState } from "react";
import { usePathname } from "next/navigation";
import { api } from "@/services/api";
import { useAuth } from "@/components/providers/AuthProvider";

const WARN_MS = 13 * 60 * 1000;
const END_MS = 15 * 60 * 1000;

export function SessionPulse() {
  const { user, logout } = useAuth();
  const pathname = usePathname();
  const [warn, setWarn] = useState(false);

  useEffect(() => {
    if (!user) return undefined;
    let last = Date.now();
    const mark = () => {
      last = Date.now();
      setWarn(false);
    };
    const beat = () => {
      if (document.visibilityState !== "visible") return;
      if (Date.now() - last > 2 * 60 * 1000) return;
      api.post("/api/session/heartbeat").catch(() => {});
    };
    const watch = window.setInterval(() => {
      const idle = Date.now() - last;
      if (idle >= END_MS) {
        api.post("/api/session/end", { reason: "TIMEOUT" }).catch(() => {});
        logout();
        return;
      }
      setWarn(idle >= WARN_MS);
    }, 15000);
    const pulse = window.setInterval(beat, 60000);
    beat();
    window.addEventListener("pointerdown", mark);
    window.addEventListener("keydown", mark);
    return () => {
      window.clearInterval(watch);
      window.clearInterval(pulse);
      window.removeEventListener("pointerdown", mark);
      window.removeEventListener("keydown", mark);
    };
  }, [user, logout, pathname]);

  if (!warn) return null;
  return (
    <div className="session-warn" role="alertdialog" aria-label="Still there?">
      <p>You have been idle. The session closes in about two minutes.</p>
      <button type="button" onClick={() => setWarn(false)}>Stay signed in</button>
    </div>
  );
}
