"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { api, getData } from "@/services/api";
import { useLocale } from "@/components/providers/LocaleProvider";
import { useAuth } from "@/components/providers/AuthProvider";
import { isExecutive } from "@/lib/admin/access";
import { Avatar, Button, Field, Modal, Tabs, TextArea, TextInput } from "@/ui/kit";
import { DataTable, Kpis } from "@/crud/kit";
import { FORMER } from "./formerCopy";

const EXIT = ["TRANSFERRED", "RELEASED", "RETIRED", "CONTRACT_ENDED", "LOAN_ENDED", "OTHER"];

function fill(template, values) {
  return Object.entries(values).reduce((text, [key, value]) => text.replaceAll(`{${key}}`, String(value)), template || "");
}

export function FormerPlayersOffice() {
  const { locale } = useLocale();
  const t = FORMER[locale] || FORMER.en;
  const { user } = useAuth();
  const executive = isExecutive(user?.role || "");
  const params = useSearchParams();
  const router = useRouter();
  const [rows, setRows] = useState([]);
  const [kpis, setKpis] = useState(null);
  const [error, setError] = useState("");
  const [view, setView] = useState(null);
  const [tab, setTab] = useState("overview");
  const [restoreRow, setRestoreRow] = useState(null);
  const [reuse, setReuse] = useState(true);
  const [ask, setAsk] = useState(null);
  const [reason, setReason] = useState("");
  const [typed, setTyped] = useState("");
  const [dialogError, setDialogError] = useState("");
  const [requests, setRequests] = useState([]);

  const q = params.get("q") || "";
  const exitReason = params.get("exit") || "";
  const year = params.get("year") || "";
  const history = params.get("history") || "";
  const deletion = params.get("deletion") || "";

  const setParam = (key, value) => {
    const next = new URLSearchParams(params.toString());
    if (value) next.set(key, value); else next.delete(key);
    const main = document.querySelector(".admin-main");
    const top = main ? main.scrollTop : 0;
    router.replace(`/admin/former-players?${next.toString()}`, { scroll: false });
    const hold = () => { if (main) main.scrollTop = top; };
    requestAnimationFrame(hold);
    window.setTimeout(hold, 60);
  };

  const load = () => {
    setError("");
    Promise.all([
      getData("/api/former-players", { q, exitReason, year, history, deletion }),
      getData("/api/former-players/kpis"),
      executive ? getData("/api/deletion-requests").catch(() => []) : Promise.resolve([]),
    ]).then(([list, kpi, queue]) => {
      setRows(list?.rows || []);
      setKpis(kpi);
      setRequests(queue || []);
    }).catch(() => setError(t.loadFailed));
  };
  useEffect(load, [q, exitReason, year, history, deletion, executive, t.loadFailed]);

  const reasonLabel = (code) => ({
    TRANSFERRED: t.transferred, RELEASED: t.released, RETIRED: t.retired,
    CONTRACT_ENDED: t.contract, LOAN_ENDED: t.loan, OTHER: t.other,
  }[code] || code || "");

  const deletionLabel = (status) => {
    if (!status || status === "NONE") return t.none;
    if (status === "REJECTED") return t.rejected;
    if (String(status).startsWith("PENDING_")) return fill(t.pendingVotes, { n: status.split("_")[1] || 0 });
    return status;
  };

  const openRequest = (row) => requests.find((item) => item.playerId === row.id && item.status === "PENDING");
  const historyFor = (row) => requests.filter((item) => item.playerId === row.id);
  const ready = reason.trim().length >= 20 && reason.trim().length <= 2000 && typed.trim().toLowerCase() === String(ask?.fullName || "").trim().toLowerCase();

  const exportRows = () => {
    const header = [t.title, t.jersey, t.position, t.exitReason, t.exitDate, t.matches, t.goals, t.paid, t.fines, t.deletion];
    const body = rows.map((row) => [row.fullName, row.jersey, row.position, reasonLabel(row.exitReason), row.exitDate, row.matches, row.goals, row.paid, row.openFines, deletionLabel(row.deletionStatus)]);
    const csv = [header, ...body].map((line) => line.map((cell) => `"${String(cell ?? "").replaceAll("\"", "\"\"")}"`).join(",")).join("\n");
    const blob = new Blob([csv], { type: "text/csv" });
    const link = document.createElement("a");
    link.href = URL.createObjectURL(blob);
    link.download = "former-players.csv";
    link.click();
  };

  const columns = useMemo(() => [
    { key: "name", label: t.title, render: (row) => <span className="person-cell"><Avatar name={row.fullName} src={row.photoUrl} /> {row.fullName}</span> },
    { key: "jersey", label: t.jersey },
    { key: "position", label: t.position },
    { key: "exit", label: t.exitReason, render: (row) => reasonLabel(row.exitReason) },
    { key: "exitDate", label: t.exitDate },
    { key: "matches", label: t.matches },
    { key: "goals", label: t.goals },
    { key: "paid", label: t.paid },
    { key: "fines", label: t.fines, render: (row) => row.openFines },
    { key: "deletion", label: t.deletion, render: (row) => deletionLabel(row.deletionStatus) },
  ], [t, reasonLabel]);

  return (
    <section>
      <header className="crud-head">
        <h1>{t.title}</h1>
        <div className="crud-tools">
          <input className="vfc-control" value={q} aria-label={t.search} placeholder={t.search} onChange={(event) => setParam("q", event.target.value)} />
          <select className="vfc-control" aria-label={t.exitReason} value={exitReason} onChange={(event) => setParam("exit", event.target.value)}>
            <option value="">{t.anyReason}</option>
            {EXIT.map((code) => <option key={code} value={code}>{reasonLabel(code)}</option>)}
          </select>
          <input className="vfc-control" aria-label={t.anyYear} placeholder={t.anyYear} value={year} onChange={(event) => setParam("year", event.target.value)} />
          <select className="vfc-control" aria-label={t.anyHistory} value={history} onChange={(event) => setParam("history", event.target.value)}>
            <option value="">{t.anyHistory}</option>
            <option value="yes">{t.yes}</option>
            <option value="no">{t.no}</option>
          </select>
          <select className="vfc-control" aria-label={t.anyDeletion} value={deletion} onChange={(event) => setParam("deletion", event.target.value)}>
            <option value="">{t.anyDeletion}</option>
            <option value="NONE">{t.none}</option>
            <option value="REJECTED">{t.rejected}</option>
          </select>
          <Button onClick={exportRows}>CSV</Button>
        </div>
      </header>
      <Kpis items={[
        { label: t.total, value: kpis?.total ?? 0 },
        { label: t.pending, value: kpis?.pending ?? 0 },
        { label: t.withHistory, value: kpis?.withHistory ?? 0 },
        { label: t.withoutHistory, value: kpis?.withoutHistory ?? 0 },
      ]} />
      {error ? <p className="crud-error">{error}</p> : null}
      <DataTable
        columns={columns}
        rows={rows}
        empty={t.title}
        actionsLabel={t.actions}
        actions={(row) => (
          <>
            <Button onClick={() => { setView(row); setTab("overview"); }}>{t.view}</Button>
            <Button onClick={() => setRestoreRow(row)}>{t.restore}</Button>
            {executive && !openRequest(row) ? <Button variant="danger" onClick={() => { setAsk(row); setReason(""); setTyped(""); setDialogError(""); }}>{t.request}</Button> : null}
            {executive && openRequest(row) ? <Button onClick={() => setView(row)}>{t.viewRequest}</Button> : null}
          </>
        )}
      />
      {view ? (
        <Modal title={view.fullName} size="lg" onClose={() => setView(null)} footer={<Button onClick={() => setView(null)}>{t.close}</Button>}>
          <Tabs label={t.title} value={tab} onChange={setTab} items={[
            { id: "overview", label: t.overview },
            { id: "career", label: t.career },
            ...(executive ? [{ id: "payments", label: t.payments }] : []),
            { id: "fines", label: t.fines },
            { id: "history", label: t.deletionHistory },
          ]} />
          {tab === "overview" ? <p>{reasonLabel(view.exitReason)} · {view.exitDate} · {view.exitNote || ""}</p> : null}
          {tab === "career" ? <p>{t.matches}: {view.matches} · {t.goals}: {view.goals}</p> : null}
          {tab === "payments" ? <p>{t.paid}: {view.paid}</p> : null}
          {tab === "fines" ? <p>{t.fines}: {view.openFines}</p> : null}
          {tab === "history" ? (
            <ul>
              {historyFor(view).map((item) => <li key={item.id}>{deletionLabel(item.status === "PENDING" ? `PENDING_${item.approvals}` : item.status)} — {item.reason}</li>)}
            </ul>
          ) : null}
        </Modal>
      ) : null}
      {restoreRow ? (
        <Modal title={t.restore} onClose={() => setRestoreRow(null)} footer={(
          <div className="vfc-foot-right pair-actions">
            <Button onClick={() => setRestoreRow(null)}>{t.cancel}</Button>
            <Button variant="primary" onClick={async () => { await api.post(`/api/former-players/${restoreRow.id}/restore`, { reuseContract: reuse }); setRestoreRow(null); load(); }}>{t.save}</Button>
          </div>
        )}>
          <label><input type="checkbox" checked={reuse} onChange={(event) => setReuse(event.target.checked)} /> {t.reuse}</label>
        </Modal>
      ) : null}
      {ask ? (
        <Modal title={t.request} size="md" onClose={() => setAsk(null)} footer={(
          <div className="vfc-foot-right pair-actions">
            <Button onClick={() => setAsk(null)}>{t.cancel}</Button>
            <Button variant="danger" disabled={!ready} onClick={async () => {
              try {
                await api.post(`/api/former-players/${ask.id}/deletion-requests`, { reason, confirmName: typed });
                setAsk(null);
                load();
              } catch (err) {
                setDialogError(err.message || t.loadFailed);
              }
            }}>{t.send}</Button>
          </div>
        )}>
          <div className="person-cell"><Avatar name={ask.fullName} src={ask.photoUrl} /><strong>{ask.fullName}</strong></div>
          <p>{t.matches}: {ask.matches} · {t.goals}: {ask.goals} · {t.paid}: {ask.paid} · {t.fines}: {ask.openFines}</p>
          <p style={{ color: "var(--accent-brand)" }}>{t.warning}</p>
          <Field label={t.reason} required>
            <TextArea value={reason} maxLength={2000} onChange={(event) => setReason(event.target.value)} />
          </Field>
          <p>{fill(t.chars, { n: reason.trim().length })}</p>
          <Field label={fill(t.typeName, { name: ask.fullName })} required>
            <TextInput value={typed} onChange={(event) => setTyped(event.target.value)} />
          </Field>
          {dialogError ? <p className="crud-error">{dialogError}</p> : null}
        </Modal>
      ) : null}
    </section>
  );
}
