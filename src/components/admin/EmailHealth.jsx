"use client";

import { useEffect, useState } from "react";
import { api, getData } from "@/services/api";
import { Button, Field, TextInput } from "@/ui/kit";

export function EmailHealth() {
  const [info, setInfo] = useState(null);
  const [to, setTo] = useState("");
  const [note, setNote] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    getData("/api/email/outbox/diagnostics").then(setInfo).catch((err) => setError(err.message));
  }, []);

  async function sendTest() {
    setError("");
    setNote("");
    try {
      const response = await api.post("/api/email/outbox/test", { to, subject: "Viking FC test", body: "This is a test from the club office." });
      setNote(response.data?.message || response.data?.data?.note || "Queued");
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <section className="admin-card" style={{ marginTop: 16 }}>
      <h2>Email</h2>
      {info ? <p>{info.reason} · {info.host}:{info.port} · {info.username || "no username"}</p> : null}
      <Field label="Send test email">
        <TextInput type="email" value={to} onChange={(event) => setTo(event.target.value)} placeholder="name@club.rw" />
      </Field>
      <div style={{ marginTop: 8 }}><Button variant="primary" onClick={sendTest} disabled={!to}>Send test email</Button></div>
      {note ? <p>{note}</p> : null}
      {error ? <p className="vfc-error">{error}</p> : null}
    </section>
  );
}
