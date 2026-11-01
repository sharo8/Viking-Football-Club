"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { getData } from "@/services/api";
import { readSession } from "@/components/login/session";

export default function PlayerHome() {
  const router = useRouter();
  const [me, setMe] = useState(null);

  useEffect(() => {
    const session = readSession();
    if (!session?.accessToken) {
      router.replace("/login");
      return;
    }
    if (session.user?.role !== "PLAYER") {
      router.replace("/admin");
      return;
    }
    getData("/api/auth/me").then(setMe).catch(() => router.replace("/login"));
  }, [router]);

  return (
    <main style={{ minHeight: "100vh", padding: 24, background: "#0c0c0c", color: "#fff" }}>
      <p style={{ letterSpacing: ".18em", color: "#a3283a" }}>VIKING FC</p>
      <h1 style={{ marginTop: 8 }}>{me ? me.fullName : "Player"}</h1>
      <p>Your matches, call-ups and messages stay on this page. Club offices are closed to player accounts.</p>
    </main>
  );
}
