"use client";

import { useEffect, useState } from "react";
import { api, getData } from "@/services/api";
import { useLocale } from "@/components/providers/LocaleProvider";
import { adminCopy } from "@/lib/admin/copy";

export function EmailLog() {
  const { locale } = useLocale();
  const copy = adminCopy[locale] || adminCopy.en;
  const [status, setStatus] = useState("");
  const [rows, setRows] = useState([]);
  const [error, setError] = useState("");

  function load(next = status) {
    getData("/api/email/outbox", next ? { status: next } : {})
      .then(setRows)
      .catch((err) => setError(err.message));
  }

  useEffect(() => { load(status); }, [status]);

  return (
    <section className="squad">
      <header className="squad-head"><h1>{copy.emails}</h1></header>
      <div className="squad-tools">
        {["", "QUEUED", "SENT", "FAILED"].map((item) => (
          <button key={item || "all"} type="button" className={status === item ? "is-on" : ""} onClick={() => setStatus(item)}>{item || "All"}</button>
        ))}
      </div>
      {error ? <p className="squad-error">{error}</p> : null}
      {rows.length === 0 ? <p className="squad-empty">{copy.empty}</p> : (
        <table className="squad-table">
          <thead><tr><th>To</th><th>Subject</th><th>Status</th><th>Attempts</th><th></th></tr></thead>
          <tbody>
            {rows.map((row) => (
              <tr key={row.id}>
                <td>{row.toEmail}</td>
                <td>{row.subject}</td>
                <td>{row.status}</td>
                <td>{row.attempts}</td>
                <td>{row.status === "FAILED" ? <button type="button" onClick={() => api.post(`/api/email/outbox/${row.id}/retry`).then(() => load())}>Retry</button> : null}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
