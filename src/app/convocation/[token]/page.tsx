"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { getData, api } from "@/services/api";

export default function ConvocationPage() {
  const params = useParams<{ token: string }>();
  const token = params?.token;
  const [sheet, setSheet] = useState<Record<string, string> | null>(null);
  const [error, setError] = useState("");
  const [done, setDone] = useState("");

  useEffect(() => {
    if (!token) return;
    getData(`/api/public/convocation/${token}`).then(setSheet).catch((err) => setError(err.message));
  }, [token]);

  async function answer(response: string) {
    try {
      const result = await api.post(`/api/public/convocation/${token}`, { response });
      setDone(result.data?.data?.response || response);
      setSheet((current) => ({ ...(current || {}), response }));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Request failed");
    }
  }

  return (
    <main style={{ minHeight: "100vh", display: "grid", placeItems: "center", padding: 24, background: "#0e0e0e", color: "#fff" }}>
      <section style={{ width: "min(480px, 100%)", background: "#161616", border: "1px solid #333", borderRadius: 16, padding: 24 }}>
        <img src="/logo/viking-fc.png" alt="Viking FC" width={72} height={72} />
        <h1 style={{ fontSize: 28, margin: "12px 0" }}>Viking FC</h1>
        {error ? <p>{error}</p> : null}
        {!sheet && !error ? <p>Loading</p> : null}
        {sheet ? (
          <>
            <p>{sheet.player}</p>
            <p>vs {sheet.opponent}</p>
            <p>{sheet.kickoff}</p>
            <p>{sheet.venue}</p>
            {sheet.meetingPlace ? <p>{sheet.meetingPlace}</p> : null}
            {sheet.meetingAt ? <p>{sheet.meetingAt}</p> : null}
            <p>{done || sheet.response}</p>
            <div style={{ display: "flex", gap: 8 }}>
              <button type="button" onClick={() => answer("CONFIRMED")} style={{ flex: 1, minHeight: 44, background: "#a3283a", color: "#fff", border: 0, borderRadius: 10 }}>Confirm</button>
              <button type="button" onClick={() => answer("DECLINED")} style={{ flex: 1, minHeight: 44, background: "transparent", color: "#fff", border: "1px solid #fff", borderRadius: 10 }}>Decline</button>
            </div>
          </>
        ) : null}
      </section>
    </main>
  );
}
