"use client";

import { useState } from "react";
import { api } from "@/services/api";
import { Button, Modal } from "@/ui/kit";

export function ImportWizard({ copy, onClose, onDone }) {
  const [step, setStep] = useState(0);
  const [preview, setPreview] = useState(null);
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function upload(file) {
    setBusy(true);
    setError("");
    try {
      const body = new FormData();
      body.append("file", file);
      const response = await api.post("/api/players/import/preview", body);
      setPreview(response.data.data);
      setStep(1);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  async function commit() {
    setBusy(true);
    setError("");
    try {
      const response = await api.post(`/api/players/import/${preview.importId}/commit`);
      setResult(response.data.data);
      setStep(2);
      onDone();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  async function template() {
    const response = await api.get("/api/players/import/template", { responseType: "blob" });
    const url = URL.createObjectURL(response.data);
    const link = document.createElement("a");
    link.href = url;
    link.download = "viking-fc-players.xlsx";
    link.click();
    URL.revokeObjectURL(url);
  }

  const summary = preview?.summary || {};
  const valid = (summary.NEW || 0) + (summary.UPDATE || 0);

  return (
    <Modal size="lg" title="Import" onClose={onClose} closeLabel={copy.close}
      footer={step === 1 ? (
        <>
          <Button variant="ghost" onClick={onClose}>{copy.cancel}</Button>
          <div className="vfc-foot-right"><Button variant="primary" loading={busy} disabled={!valid} onClick={commit}>Import {valid}</Button></div>
        </>
      ) : null}>
      {error ? <p className="vfc-error">{error}</p> : null}
      {step === 0 ? (
        <div className="vfc-grid">
          <Button onClick={template}>Download template</Button>
          <label className="file-btn">Upload .xlsx / .csv
            <input type="file" accept=".csv,.xlsx,text/csv" onChange={(event) => event.target.files?.[0] && upload(event.target.files[0])} />
          </label>
          {busy ? <p>Reading…</p> : null}
        </div>
      ) : null}
      {step === 1 && preview ? (
        <>
          <div className="kpi-strip">
            <article><small>New</small><strong>{summary.NEW || 0}</strong></article>
            <article><small>Updates</small><strong>{summary.UPDATE || 0}</strong></article>
            <article><small>Rejected</small><strong>{summary.REJECTED || 0}</strong></article>
            <article><small>Unchanged</small><strong>{summary.UNCHANGED || 0}</strong></article>
          </div>
          <ul className="depth-list">
            {preview.rows.map((row) => (
              <li key={row.row}><span>#{row.row} {row.fullName || "—"}</span><b>{row.status}</b><small>{row.reason}</small></li>
            ))}
          </ul>
        </>
      ) : null}
      {step === 2 && result ? (
        <div className="vfc-check" role="status">{result.created} created · {result.updated} updated · {result.skipped} skipped</div>
      ) : null}
    </Modal>
  );
}
