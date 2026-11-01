"use client";

import { useEffect, useState } from "react";
import { api, getData } from "@/services/api";
import { getMediaUrl } from "@/services/media";
import { Avatar, Badge, Button, Field, IconButton, Modal, SelectInput, TextInput } from "@/ui/kit";
import { BONUS_LABEL, label } from "./playersCopy";
import { rwf } from "./attendanceCopy";
import { BONUS_TYPES } from "./playerRules";
import { Pencil, Plus } from "lucide-react";

const TEXT = {
  en: {
    byPlayer: "By player", earned: "Earned bonuses", noRates: "No rates set", setRates: "Set rates",
    edit: "Edit rates", addType: "Add bonus type", amount: "Amount (RWF)", remove: "Remove", save: "Save", close: "Close",
    emptyEarned: "No bonuses recorded for this period. Bonus rates are set per player; earned bonuses appear after matches are finalized.",
    noneYet: "No finalized match with a goal, assist or appearance yet.",
    player: "Player", rates: "Rates", accrued: "Accrued", approved: "Approved", paid: "Paid", actions: "Actions",
    type: "Type", more: "more", missing: "Choose a type and an amount greater than zero", clubRate: "Club rate, used until this player has a personal rate",
  },
  fr: {
    byPlayer: "Par joueur", earned: "Primes gagnées", noRates: "Aucun barème", setRates: "Définir",
    edit: "Modifier les barèmes", addType: "Ajouter un type", amount: "Montant (RWF)", remove: "Retirer", save: "Enregistrer", close: "Fermer",
    emptyEarned: "Aucune prime pour cette période. Les barèmes sont par joueur ; les primes gagnées apparaissent après un match terminé.",
    noneYet: "Aucun match terminé avec un but, une passe ou une apparition.",
    player: "Joueur", rates: "Barèmes", accrued: "Acquises", approved: "Approuvées", paid: "Payées", actions: "Actions",
    type: "Type", more: "de plus", missing: "Choisissez un type et un montant supérieur à zéro", clubRate: "Barème du club, tant que le joueur n'a pas le sien",
  },
  rw: {
    byPlayer: "Ku mukinnyi", earned: "Ibihembo byabonetse", noRates: "Nta giciro", setRates: "Shyiraho",
    edit: "Hindura ibiciro", addType: "Ongeraho ubwoko", amount: "Amafaranga (RWF)", remove: "Kuraho", save: "Bika", close: "Funga",
    emptyEarned: "Nta gihembo muri iki gihe. Ibiciro bishyirwa kuri buri mukinnyi.",
    noneYet: "Nta mukino warangiye ufite igitego, ubufasha cyangwa kwitabira.",
    player: "Umukinnyi", rates: "Ibiciro", accrued: "Byabonetse", approved: "Byemejwe", paid: "Byishyuwe", actions: "Ibikorwa",
    type: "Ubwoko", more: "ibindi", missing: "Hitamo ubwoko n'amafaranga arenze zeru", clubRate: "Igiciro cy'ikipe",
  },
};

export function BonusesPanel({ locale = "en" }) {
  const t = TEXT[locale] || TEXT.en;
  const [view, setView] = useState("players");
  const [rows, setRows] = useState([]);
  const [earned, setEarned] = useState([]);
  const [editing, setEditing] = useState(null);
  const [rates, setRates] = useState([]);
  const [error, setError] = useState("");

  function load() {
    getData("/api/bonuses/by-player").then((list) => setRows(Array.isArray(list) ? list : [])).catch(() => setRows([]));
    getData("/api/bonuses").then((list) => setEarned(Array.isArray(list) ? list : [])).catch(() => setEarned([]));
  }

  useEffect(() => { load(); }, []);

  function openRates(row) {
    setEditing(row);
    setRates((row.rates || []).map((rate) => ({ type: rate.type, amount: String(rate.amount), customName: rate.customName || "" })));
    setError("");
  }

  async function saveRates(event) {
    event.preventDefault();
    const ready = rates.filter((rate) => Number(rate.amount) > 0 && rate.type);
    if (!ready.length) {
      setError(t.missing);
      return;
    }
    try {
      await api.put(`/api/players/${editing.id}/bonus-rules`, ready.map((rate) => ({
        type: rate.type, amount: Number(rate.amount), active: true, customName: rate.customName || null,
      })));
      setEditing(null);
      load();
    } catch (err) {
      setError(err.message || t.missing);
    }
  }

  function chips(row) {
    const list = row.rates || [];
    const shown = list.slice(0, 3);
    return (
      <span className="bonus-chips">
        {shown.map((rate) => <Badge key={`${rate.type}-${rate.amount}`} tone="neutral" title={rate.inherited ? t.clubRate : ""}>{label(BONUS_LABEL, rate.type, locale)} {rwf(rate.amount)}</Badge>)}
        {list.length > 3 ? <Badge tone="neutral" title={list.map((rate) => `${label(BONUS_LABEL, rate.type, locale)} ${rwf(rate.amount)}`).join(", ")}>+{list.length - 3} {t.more}</Badge> : null}
      </span>
    );
  }

  return (
    <section className="crud-card">
      <div className="bonus-switch" role="group" aria-label={t.byPlayer}>
        <button type="button" className={view === "players" ? "is-on" : ""} onClick={() => setView("players")}>{t.byPlayer}</button>
        <button type="button" className={view === "earned" ? "is-on" : ""} onClick={() => setView("earned")}>{t.earned}</button>
      </div>
      {view === "players" ? (
        <div className="crud-scroll">
          <table className="crud-table finance-table">
            <thead><tr><th>{t.player}</th><th>{t.rates}</th><th>{t.accrued}</th><th>{t.approved}</th><th>{t.paid}</th><th>{t.actions}</th></tr></thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.id}>
                  <td><span className="finance-person"><Avatar name={row.fullName} src={getMediaUrl(row.photoUrl)} size="sm" />{row.fullName}{row.jerseyNumber ? ` #${row.jerseyNumber}` : ""}</span></td>
                  <td>{row.rates?.length ? chips(row) : <Badge>{t.noRates}</Badge>}</td>
                  <td>{rwf(row.accrued)}</td>
                  <td>{rwf(row.approved)}</td>
                  <td>{rwf(row.paid)}</td>
                  <td>
                    <IconButton label={row.rates?.length ? t.edit : t.setRates} onClick={() => openRates(row)}><Pencil size={16} /></IconButton>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : !earned.length ? (
        <p className="crud-empty">{t.emptyEarned} {t.noneYet}</p>
      ) : (
        <table className="crud-table">
          <thead><tr><th>{t.player}</th><th>{t.type}</th><th>{t.amount}</th><th>Status</th></tr></thead>
          <tbody>
            {earned.map((row) => (
              <tr key={row.id}><td>{row.playerName}</td><td>{label(BONUS_LABEL, row.type, locale)}</td><td>{rwf(row.amount)}</td><td>{row.status}</td></tr>
            ))}
          </tbody>
        </table>
      )}
      {editing ? (
        <Modal size="md" title={editing.fullName} subtitle={t.edit} onClose={() => setEditing(null)} closeLabel={t.close}
          footer={<><Button variant="ghost" onClick={() => setEditing(null)}>{t.close}</Button><Button variant="primary" onClick={saveRates}>{t.save}</Button></>}>
          <form className="vfc-form" onSubmit={saveRates}>
            {error ? <p className="vfc-error">{error}</p> : null}
            {rates.map((rate, index) => (
              <div className="att-filters" key={`${rate.type}-${index}`}>
                <Field label={t.type}>
                  <SelectInput value={rate.type} onChange={(event) => setRates((current) => current.map((item, itemIndex) => itemIndex === index ? { ...item, type: event.target.value } : item))}>
                    {BONUS_TYPES.map((type) => <option key={type} value={type}>{label(BONUS_LABEL, type, locale)}</option>)}
                  </SelectInput>
                </Field>
                <Field label={t.amount}><TextInput inputMode="numeric" value={rate.amount} onChange={(event) => setRates((current) => current.map((item, itemIndex) => itemIndex === index ? { ...item, amount: event.target.value } : item))} /></Field>
                <Button size="sm" variant="danger" onClick={() => setRates((current) => current.filter((_, itemIndex) => itemIndex !== index))}>{t.remove}</Button>
              </div>
            ))}
            <Button {...(rates.length >= 3 ? { disabled: true } : {})} onClick={() => setRates((current) => [...current, { type: "GOAL", amount: "" }])}><Plus size={16} /> {t.addType}</Button>
          </form>
        </Modal>
      ) : null}
    </section>
  );
}
