"use client";

import { useEffect, useMemo, useState } from "react";
import { Download, Eye, MoreHorizontal, Pencil, Wallet } from "lucide-react";
import { api, getData } from "@/services/api";
import { Avatar, Button, Field, IconButton, Modal, SelectInput, TextInput } from "@/ui/kit";
import { useAdminToast } from "./toast";
import { ExportDialog } from "./ExportDialog";
import { PlayerCombo } from "./PlayerCombo";

const STATUS = {
  en: { OUTSTANDING: "Outstanding", PARTIALLY_PAID: "Partial", PAID: "Paid", WAIVED: "Waived" },
  fr: { OUTSTANDING: "Impayée", PARTIALLY_PAID: "Partielle", PAID: "Payée", WAIVED: "Annulée" },
  rw: { OUTSTANDING: "Itishyuwe", PARTIALLY_PAID: "Yishyuwe igice", PAID: "Yishyuwe", WAIVED: "Yakuweho" },
};

const COPY = {
  en: {
    title: "Fines", subtitle: "A fine reduces the next open salary instalment when it is deducted.",
    person: "Person", rule: "Rule", source: "Source", amount: "Amount", paid: "Paid", balance: "Balance", status: "Status", due: "Due", issue: "Issue date", actions: "Actions",
    search: "Search", any: "Any status", player: "Player", period: "Period", export: "Export",
    today: "Today", week: "This week", month: "This month", year: "This year", all: "All time", custom: "Custom",
    deduct: "Deduct from salary", pay: "Record payment", waive: "Waive", edit: "Edit", view: "View player record", more: "More",
    empty: "No fines.", overdue: "Overdue", issued: "Issued", collected: "Collected", outstanding: "Outstanding",
    record: "Player fines record", close: "Close", payments: "Payments", editHint: "Change the attendance mark. The fine follows that mark.",
    reason: "Reason", amountLabel: "Amount", save: "Save",
  },
  fr: {
    title: "Amendes", subtitle: "Une amende réduit la prochaine échéance de salaire quand elle est retenue.",
    person: "Personne", rule: "Règle", source: "Origine", amount: "Montant", paid: "Payé", balance: "Solde", status: "Statut", due: "Échéance", issue: "Date", actions: "Actions",
    search: "Rechercher", any: "Tout statut", player: "Joueur", period: "Période", export: "Exporter",
    today: "Aujourd'hui", week: "Cette semaine", month: "Ce mois", year: "Cette année", all: "Tout", custom: "Personnalisé",
    deduct: "Retenir sur le salaire", pay: "Encaisser", waive: "Annuler", edit: "Modifier", view: "Voir la fiche", more: "Plus",
    empty: "Aucune amende.", overdue: "En retard", issued: "Émises", collected: "Encaissées", outstanding: "Dues",
    record: "Fiche des amendes", close: "Fermer", payments: "Paiements", editHint: "Modifiez le pointage. L'amende suit le pointage.",
    reason: "Motif", amountLabel: "Montant", save: "Enregistrer",
  },
  rw: {
    title: "Ibihano", subtitle: "Igihano kigabanya umushahara ukurikira iyo gikurwa ku mushahara.",
    person: "Umuntu", rule: "Itegeko", source: "Inkomoko", amount: "Amafaranga", paid: "Yishyuwe", balance: "Asigaye", status: "Imimerere", due: "Itariki", issue: "Itariki", actions: "Ibikorwa",
    search: "Shakisha", any: "Imimerere yose", player: "Umukinnyi", period: "Igihe", export: "Kohereza",
    today: "Uyu munsi", week: "Iki cyumweru", month: "Uku kwezi", year: "Uyu mwaka", all: "Byose", custom: "Ihitamo",
    deduct: "Kuraho ku mushahara", pay: "Kwishyura", waive: "Gukuraho", edit: "Guhindura", view: "Reba", more: "Ibindi",
    empty: "Nta gihano.", overdue: "Byatinze", issued: "Byatanzwe", collected: "Byakiriwe", outstanding: "Bitishyuwe",
    record: "Inyandiko y'amahazabu", close: "Funga", payments: "Ubwishyu", editHint: "Hindura uko yabaruye. Ihazabu rikorana na byo.",
    reason: "Impamvu", amountLabel: "Amafaranga", save: "Bika",
  },
};

function rwf(value) {
  return `RWF ${new Intl.NumberFormat("en-US").format(Number(value) || 0)}`;
}

async function saveExport(body) {
  const response = await api.post("/api/exports", body, { responseType: "blob" });
  const match = String(response.headers["content-disposition"] || "").match(/filename="([^"]+)"/);
  const link = document.createElement("a");
  link.href = URL.createObjectURL(response.data);
  link.download = match ? match[1] : "viking-fc-export";
  link.click();
  URL.revokeObjectURL(link.href);
}

export function FinesOffice({ locale = "en" }) {
  const t = COPY[locale] || COPY.en;
  const labels = STATUS[locale] || STATUS.en;
  const toast = useAdminToast();
  const [rows, setRows] = useState([]);
  const [summary, setSummary] = useState(null);
  const [players, setPlayers] = useState([]);
  const [error, setError] = useState("");
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("");
  const [preset, setPreset] = useState("all");
  const [picked, setPicked] = useState([]);
  const [busy, setBusy] = useState(0);
  const [record, setRecord] = useState(null);
  const [menu, setMenu] = useState(0);
  const [pay, setPay] = useState(null);
  const [waive, setWaive] = useState(null);
  const [amount, setAmount] = useState("");
  const [reason, setReason] = useState("");
  const [exporter, setExporter] = useState(false);
  const [exporting, setExporting] = useState(false);

  const load = () => {
    const params = { preset, status: status || undefined, players: picked.join(",") || undefined };
    Promise.all([getData("/api/fines", params), getData("/api/fines/summary"), getData("/api/players")])
      .then(([ledger, totals, squad]) => { setRows(ledger || []); setSummary(totals); setPlayers(squad || []); setError(""); })
      .catch((err) => setError(err.message));
  };
  useEffect(() => { load(); }, [preset, status, picked.join(",")]);

  const shown = useMemo(() => rows.filter((row) => {
    const blob = `${row.player || ""} ${row.source || ""}`.toLowerCase();
    return !query || blob.includes(query.toLowerCase());
  }), [rows, query]);
  const personFines = record ? rows.filter((row) => row.playerId === record.playerId) : [];
  const issued = personFines.reduce((sum, row) => sum + Number(row.amount || 0), 0);
  const paid = personFines.reduce((sum, row) => sum + Number(row.paid || 0), 0);
  const open = personFines.reduce((sum, row) => sum + Number(row.balance || 0), 0);

  async function deduct(row) {
    setBusy(row.id);
    try {
      const response = await api.post(`/api/fines/${row.id}/deduct`);
      toast(response.data?.message || t.deduct);
      load();
    } catch (err) {
      toast(err.message);
    } finally {
      setBusy(0);
    }
  }

  async function sendPayment() {
    try {
      await api.post(`/api/fines/${pay.id}/payments`, { amount: Number(amount), method: "CASH", reference: "" });
      setPay(null);
      setAmount("");
      load();
    } catch (err) {
      toast(err.message);
    }
  }

  async function sendWaiver() {
    try {
      await api.post(`/api/fines/${waive.id}/waive`, { reason });
      setWaive(null);
      setReason("");
      load();
    } catch (err) {
      toast(err.message);
    }
  }

  return (
    <section className="crud-page">
      <header className="crud-head">
        <div>
          <h1>{t.title} <span className="view-code">{shown.length}</span></h1>
          <p>{t.subtitle}</p>
        </div>
        <div className="crud-tools">
          <TextInput aria-label={t.search} placeholder={t.search} value={query} onChange={(event) => setQuery(event.target.value)} />
          <Button variant="primary" icon={<Download size={16} />} onClick={() => setExporter(true)}>{t.export}</Button>
        </div>
      </header>
      <div className="att-filters">
        <PlayerCombo locale={locale} players={players} selected={picked} onChange={(ids) => setPicked(ids.map((id) => Number(id)))} />
        <Field label={t.period}>
          <SelectInput aria-label={t.period} value={preset} onChange={(event) => setPreset(event.target.value)}>
            {[["all", t.all], ["today", t.today], ["week", t.week], ["month", t.month], ["year", t.year]].map(([id, label]) => <option key={id} value={id}>{label}</option>)}
          </SelectInput>
        </Field>
        <Field label={t.status}>
          <SelectInput aria-label={t.status} value={status} onChange={(event) => setStatus(event.target.value)}>
            <option value="">{t.any}</option>
            {Object.entries(labels).map(([key, label]) => <option key={key} value={key}>{label}</option>)}
          </SelectInput>
        </Field>
      </div>
      {summary ? (
        <div className="crud-kpis">
          <article className="crud-kpi"><strong>{rwf(summary.totalFinesDue)}</strong><span>{t.issued}</span></article>
          <article className="crud-kpi"><strong>{rwf(summary.collected)}</strong><span>{t.collected}</span></article>
          <article className="crud-kpi"><strong>{rwf(summary.outstanding)}</strong><span>{t.outstanding}</span></article>
        </div>
      ) : null}
      {error ? <Button onClick={load}>{error}</Button> : null}
      {!error && !shown.length ? <p className="crud-empty">{t.empty}</p> : null}
      {shown.length ? (
        <div className="crud-scroll">
          <table className="crud-table fines-table">
            <thead>
              <tr>
                <th>{t.person}</th><th>{t.issue}</th><th>{t.rule}</th><th>{t.source}</th><th>{t.amount}</th><th>{t.paid}</th><th>{t.balance}</th><th>{t.due}</th><th>{t.status}</th><th>{t.actions}</th>
              </tr>
            </thead>
            <tbody>
              {shown.map((row) => (
                <tr key={row.id}>
                  <td><button type="button" className="audit-link" onClick={() => setRecord(row)}>{row.player || "—"}</button></td>
                  <td>{row.date || "—"}</td>
                  <td>{row.rule ? <span className="fine-chip">Rule {row.rule}</span> : row.reason || "—"}</td>
                  <td>{row.source || "—"}</td>
                  <td>{rwf(row.amount)}</td>
                  <td>{rwf(row.paid)}</td>
                  <td>{rwf(row.balance)}</td>
                  <td>{row.due || "—"}{row.overdue ? ` · ${t.overdue}` : ""}</td>
                  <td><span className={`fine-chip ${row.overdue ? "overdue" : row.status}`}>{labels[row.status] || row.status}</span></td>
                  <td>
                    <div className="crud-actions">
                      <IconButton label={t.view} onClick={() => setRecord(row)}><Eye size={16} /></IconButton>
                      <IconButton label={t.pay} onClick={() => { setPay(row); setAmount(String(row.balance || "")); }}><Wallet size={16} /></IconButton>
                      <IconButton label={t.deduct} disabled={!(row.balance > 0)} onClick={() => deduct(row)}>{busy === row.id ? "…" : "−"}</IconButton>
                      <IconButton label={t.waive} onClick={() => setWaive(row)}><span>W</span></IconButton>
                      <IconButton label={t.edit} onClick={() => toast(t.editHint)}><Pencil size={16} /></IconButton>
                      <div className="vfc-pop att-pop-end">
                        <IconButton label={t.more} onClick={() => setMenu(menu === row.id ? 0 : row.id)}><MoreHorizontal size={16} /></IconButton>
                        {menu === row.id ? <div className="vfc-popover att-menu"><Button onClick={() => { setMenu(0); saveExport({ resource: "FINES", format: "pdf", scope: { preset: "all" }, people: row.playerId ? [row.playerId] : [], split: "none", level: "details" }).catch((err) => toast(err.message)); }}>{t.export}</Button></div> : null}
                      </div>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
      {record ? (
        <Modal title={t.record} subtitle={record.player} size="lg" onClose={() => setRecord(null)} closeLabel={t.close} leading={<Avatar name={record.player} src={record.photo} />}>
          <div className="crud-kpis">
            <article className="crud-kpi"><strong>{rwf(issued)}</strong><span>{t.issued}</span></article>
            <article className="crud-kpi"><strong>{rwf(paid)}</strong><span>{t.collected}</span></article>
            <article className="crud-kpi"><strong>{rwf(open)}</strong><span>{t.outstanding}</span></article>
          </div>
          <div className="crud-scroll">
            <table className="crud-table">
              <thead><tr><th>{t.issue}</th><th>{t.rule}</th><th>{t.source}</th><th>{t.amount}</th><th>{t.paid}</th><th>{t.balance}</th><th>{t.status}</th><th>{t.payments}</th></tr></thead>
              <tbody>
                {personFines.map((row) => (
                  <tr key={row.id}>
                    <td>{row.date}</td>
                    <td>{row.rule ? `Rule ${row.rule}` : "—"}</td>
                    <td>{row.source}</td>
                    <td>{rwf(row.amount)}</td>
                    <td>{rwf(row.paid)}</td>
                    <td>{rwf(row.balance)}</td>
                    <td>{labels[row.status] || row.status}</td>
                    <td>{(row.payments || []).map((payment) => String(payment.date).slice(0, 10)).join(", ") || "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Modal>
      ) : null}
      {pay ? (
        <Modal title={t.pay} subtitle={pay.player} onClose={() => setPay(null)} closeLabel={t.close} footer={<Button variant="primary" onClick={sendPayment}>{t.save}</Button>}>
          <Field label={t.amountLabel}><TextInput type="number" value={amount} onChange={(event) => setAmount(event.target.value)} /></Field>
        </Modal>
      ) : null}
      {waive ? (
        <Modal title={t.waive} subtitle={waive.player} onClose={() => setWaive(null)} closeLabel={t.close} footer={<Button variant="primary" onClick={sendWaiver}>{t.save}</Button>}>
          <Field label={t.reason}><TextInput value={reason} onChange={(event) => setReason(event.target.value)} /></Field>
        </Modal>
      ) : null}
      <ExportDialog open={exporter} onClose={() => setExporter(false)} defaultResource="FINES" players={players} busy={exporting} onDownload={(body) => {
        setExporting(true);
        saveExport(body).then(() => setExporter(false)).catch((err) => toast(err.message)).finally(() => setExporting(false));
      }} />
    </section>
  );
}
