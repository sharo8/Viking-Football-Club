"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { motion } from "framer-motion";
import { Clock, Download, Eye, Pencil, Plus, Shield, Users, Wallet } from "lucide-react";
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { api, getData } from "@/services/api";
import { Avatar, Badge, Button, Field, IconButton, Modal, SelectInput, Switch, Tabs, TextArea, TextInput } from "@/ui/kit";
import { paySummary } from "./playerRules";
import { rwf } from "./attendanceCopy";
import { BonusesPanel } from "./BonusesPanel";

const CHARTS = [
  ["income-expenses", "Income and expenses", "Revenus et dépenses", "Amafaranga yinjira n'ayasohoka"],
  ["paid-vs-owed", "Paid and still owed", "Payé et encore dû", "Byishyuwe n'ibisigaye"],
  ["player-ranking", "Player salaries", "Salaires des joueurs", "Imishahara y'abakinnyi"],
  ["staff-by-role", "Staff pay by role", "Paie du staff par rôle", "Umushahara w'abakozi"],
  ["admin-by-person", "Administration pay", "Paie de l'administration", "Umushahara w'ubuyobozi"],
  ["salary-histogram", "Salary distribution", "Répartition des salaires", "Uko imishahara igabanyijemo"],
  ["bonuses-by-period", "Bonuses accrued and paid", "Primes dues et payées", "Ibihembo"],
  ["top-bonus-earners", "Top bonus earners", "Plus grosses primes", "Abahawe ibihembo byinshi"],
  ["expense-by-category", "Expenses by category", "Dépenses par catégorie", "Amafaranga asohoka"],
  ["expense-by-month", "Monthly expenses", "Dépenses mensuelles", "Amafaranga y'ukwezi"],
  ["cost-per-match", "Cost per match", "Coût par match", "Ikiguzi cya match"],
  ["training-spend", "Training ground spend", "Terrain d'entraînement", "Ikiguzi cy'imyitozo"],
  ["fines-by-month", "Fines issued and collected", "Amendes émises et encaissées", "Ibihano"],
  ["cash-trend", "Cash balance trend", "Évolution de la caisse", "Konte y'amafaranga"],
  ["budget-vs-actual", "Budget and actual", "Budget et réel", "Ingengo y'imari n'ukuri"],
  ["social-by-month", "Social fund due and spent", "Fonds social dû et dépensé", "Ikigega"],
];

const COPY = {
  en: {
    title: "Finance", subtitle: "Players, staff and administration stay in separate groups. Someone with no seasonal pay is not in the payroll.",
    overview: "Overview", payroll: "Payroll", fines: "Fines and deductions", expenses: "Expenses", reports: "Reports",
    players: "Players", staff: "Staff", administration: "Administration", allGroups: "All groups",
    season: "Season", period: "Period", groupBy: "Group by", thisSeason: "This season", thisYear: "This year", thisMonth: "This month",
    day: "Day", week: "Week", month: "Month", year: "Year",
    headcount: "People", payrollTotal: "Season payroll", monthly: "Monthly wage bill", average: "Average pay",
    advances: "Advances", envelopes: "Envelopes", social: "Social fund balance", socialTab: "Social fund", totals: "Totals", stillToPay: "Total still to pay", unpaidBonuses: "Unpaid bonuses", several: "Pay several months", more: "More",
    paid: "Paid to date", net: "Net balance due", cash: "Cash and bank balance", expenseTotal: "Total expenses",
    bonusesPaid: "Bonuses paid", finesDue: "Fines receivable", payable: "Net payable",
    netHint: "Net balance due is the remaining salary after social fund phase 2, minus what the person owes the club. A seasonal pay of zero is excluded.",
    person: "Person", role: "Role", seasonal: "Seasonal pay", status: "Status", actions: "Actions", modified: "Last modified",
    view: "View", empty: "No one in this group.", search: "Search", sort: "Sort", name: "Name", high: "Highest paid", low: "Lowest paid", due: "Highest balance due",
    total: "Group total", issued: "Issued", collected: "Collected", outstanding: "Outstanding", openFines: "Open the fines ledger",
    export: "Export", close: "Close", payNotSet: "Pay not set", addPay: "Add contract and pay",
    paidPct: "paid", viewPayroll: "View payroll", stillOwed: "Still owed",
    summary: "Summary", instalments: "Instalments", payments: "Payments", deductions: "Deductions", bonuses: "Bonuses",
    receipts: "Receipts", history: "History", noRows: "Nothing recorded yet.", record: "Record payment", reverse: "Reverse", editPay: "Edit pay", exempt: "Social fund exempt",
    needSalary: "Enter the seasonal pay", needAdvance: "The advance must stay between zero and the seasonal pay", needReason: "Enter the reason for the change",
    socialShort: "Monthly pay {monthly} gives {three} for 3 months, below the social fund {fund}: raise the salary, mark the person social-fund exempt, or change the social fund in Settings.",
    amount: "Amount", method: "Method", reference: "Reference", date: "Date", reason: "Reason", save: "Save",
    cashMethod: "Cash", mobile: "Mobile money", bank: "Bank",
    category: "Category", expenseName: "Name of the expense", expenseHint: "e.g. Match balls, Bus hire to Huye", description: "Description", vendor: "Vendor", addExpense: "Add expense",
    what: "What", who: "Who", split: "Split", level: "Level", format: "Format", everyone: "Everyone",
    salaries: "Salaries only", bonusesOnly: "Bonuses only", expensesOnly: "Expenses only", finesOnly: "Fines and deductions only",
    socialOnly: "Social fund only", receiptsOnly: "Receipts only", full: "Full report",
    single: "Single document", details: "Details", summaryLevel: "Summary",
    receiptNote: "A signature drawn on screen is an internal proof. Large amounts should also keep a signed paper copy.",
    generate: "Generate receipt", download: "Download", retry: "Retry", emptyChart: "No figures for this chart yet.",
    rule: "Rule", balance: "Balance", paidCol: "Paid",
  },
  fr: {
    title: "Finances", subtitle: "Joueurs, staff et administration restent séparés. Un salaire saisonnier à zéro n'entre pas dans la paie.",
    overview: "Vue d'ensemble", payroll: "Paie", fines: "Amendes et retenues", expenses: "Dépenses", reports: "Rapports",
    players: "Joueurs", staff: "Staff", administration: "Administration", allGroups: "Tous les groupes",
    season: "Saison", period: "Période", groupBy: "Regrouper", thisSeason: "Cette saison", thisYear: "Cette année", thisMonth: "Ce mois",
    day: "Jour", week: "Semaine", month: "Mois", year: "Année",
    headcount: "Personnes", payrollTotal: "Masse saisonnière", monthly: "Masse mensuelle", average: "Salaire moyen",
    advances: "Avances", envelopes: "Enveloppes", social: "Solde du fonds social", socialTab: "Fonds social", totals: "Totaux", stillToPay: "Reste à payer", unpaidBonuses: "Primes non payées", several: "Payer plusieurs mois", more: "Plus",
    paid: "Déjà payé", net: "Solde net dû", cash: "Caisse et banque", expenseTotal: "Dépenses",
    bonusesPaid: "Primes payées", finesDue: "Amendes à recevoir", payable: "Net à payer",
    netHint: "Le solde net dû est le salaire restant après la phase 2 du fonds social, moins ce que la personne doit au club. Un salaire saisonnier nul est exclu.",
    person: "Personne", role: "Rôle", seasonal: "Salaire saison", status: "Statut", actions: "Actions", modified: "Dernière modification",
    view: "Voir", empty: "Personne dans ce groupe.", search: "Rechercher", sort: "Trier", name: "Nom", high: "Mieux payés", low: "Moins payés", due: "Plus gros solde",
    total: "Total du groupe", issued: "Émises", collected: "Encaissées", outstanding: "Dues", openFines: "Ouvrir le registre des amendes",
    export: "Exporter", close: "Fermer", payNotSet: "Salaire non défini", addPay: "Ajouter le contrat et le salaire",
    paidPct: "payé", viewPayroll: "Voir la paie", stillOwed: "Encore dû",
    summary: "Résumé", instalments: "Échéances", payments: "Paiements", deductions: "Retenues", bonuses: "Primes",
    receipts: "Reçus", history: "Historique", noRows: "Rien pour le moment.", record: "Enregistrer un paiement", reverse: "Annuler", editPay: "Modifier la paie", exempt: "Exonéré du fonds social",
    needSalary: "Saisissez le salaire saisonnier", needAdvance: "L'avance doit rester entre zéro et le salaire saisonnier", needReason: "Indiquez la raison du changement",
    socialShort: "Un salaire mensuel de {monthly} donne {three} pour 3 mois, moins que le fonds social {fund} : augmentez le salaire, exonérez la personne, ou changez le fonds social dans Réglages.",
    amount: "Montant", method: "Moyen", reference: "Référence", date: "Date", reason: "Motif", save: "Enregistrer",
    cashMethod: "Espèces", mobile: "Mobile money", bank: "Banque",
    category: "Catégorie", expenseName: "Nom de la dépense", expenseHint: "ex. Ballons, bus vers Huye", description: "Description", vendor: "Fournisseur", addExpense: "Ajouter une dépense",
    what: "Quoi", who: "Qui", split: "Découpage", level: "Niveau", format: "Format", everyone: "Tout le monde",
    salaries: "Salaires seulement", bonusesOnly: "Primes seulement", expensesOnly: "Dépenses seulement", finesOnly: "Amendes seulement",
    socialOnly: "Fonds social seulement", receiptsOnly: "Reçus seulement", full: "Rapport complet",
    single: "Un seul document", details: "Détails", summaryLevel: "Résumé",
    receiptNote: "Une signature à l'écran est une preuve interne. Les gros montants gardent aussi une copie papier signée.",
    generate: "Créer le reçu", download: "Télécharger", retry: "Réessayer", emptyChart: "Pas encore de chiffres pour ce graphique.",
    rule: "Règle", balance: "Solde", paidCol: "Payé",
  },
  rw: {
    title: "Imari", subtitle: "Abakinnyi, abakozi n'ubuyobozi ntibavangwa. Udafite umushahara w'igihembwe ntajya mu mushahara.",
    overview: "Incamake", payroll: "Umushahara", fines: "Ibihano", expenses: "Amafaranga asohoka", reports: "Raporo",
    players: "Abakinnyi", staff: "Abakozi", administration: "Ubuyobozi", allGroups: "Amatsinda yose",
    season: "Igihembwe", period: "Igihe", groupBy: "Gutoranya", thisSeason: "Iki gihembwe", thisYear: "Uyu mwaka", thisMonth: "Uku kwezi",
    day: "Umunsi", week: "Icyumweru", month: "Ukwezi", year: "Umwaka",
    headcount: "Abantu", payrollTotal: "Umushahara w'igihembwe", monthly: "Umushahara w'ukwezi", average: "Impuzandengo",
    advances: "Inyongera", envelopes: "Amabaruwa", social: "Ikigega", socialTab: "Ikigega", totals: "Ibiteranyo", stillToPay: "Bisigaye kwishyura", unpaidBonuses: "Ibihembo bitarishyurwa", several: "Ishyura amezi menshi", more: "Ibindi",
    paid: "Byishyuwe", net: "Asigaye", cash: "Konte", expenseTotal: "Ayasohotse",
    bonusesPaid: "Ibihembo byishyuwe", finesDue: "Ibihano bitarishyurwa", payable: "Asigaye kwishyura",
    netHint: "Asigaye ni umushahara usigaye nyuma y'ikigega cya kabiri, ukuyemo umuntu wese club. Udafite umushahara ntajya muri iki giteranyo.",
    person: "Umuntu", role: "Uruhare", seasonal: "Umushahara", status: "Imimerere", actions: "Ibikorwa", modified: "Yahinduwe",
    view: "Reba", empty: "Nta muntu muri iri tsinda.", search: "Shakisha", sort: "Tondeka", name: "Izina", high: "Bishyurwa cyane", low: "Bishyurwa gake", due: "Ideni rinini",
    total: "Igiteranyo", issued: "Byatanzwe", collected: "Byakusanyijwe", outstanding: "Bitarishyurwa", openFines: "Fungura ibihano",
    export: "Sohora", close: "Funga", payNotSet: "Umushahara utarashyirwaho", addPay: "Ongeraho amasezerano",
    paidPct: "byishyuwe", viewPayroll: "Reba umushahara", stillOwed: "Birasigaye",
    summary: "Incamake", instalments: "Ibyiciro", payments: "Ubwishyu", deductions: "Ibikurwaho", bonuses: "Ibihembo",
    receipts: "Inyemezabwishyu", history: "Amateka", noRows: "Nta kintu.", record: "Andika ubwishyu", reverse: "Kuraho", editPay: "Hindura umushahara", exempt: "Utwikiriwe ikigega",
    needSalary: "Andika umushahara w'igihembwe", needAdvance: "Avansi igomba kuba hagati ya zeru n'umushahara", needReason: "Andika impamvu y'impinduka",
    socialShort: "Umushahara w'ukwezi {monthly} utanga {three} mu mezi 3, munsi y'ikigega {fund}. Wongere umushahara cyangwa uhagarike ikigega.",
    amount: "Amafaranga", method: "Uburyo", reference: "Nimero", date: "Itariki", reason: "Impamvu", save: "Bika",
    cashMethod: "Amafaranga", mobile: "Mobile money", bank: "Banki",
    category: "Icyiciro", expenseName: "Izina ry'ikiguzi", expenseHint: "urugero: umupira, bisi i Huye", description: "Ibisobanuro", vendor: "Uwacuruza", addExpense: "Ongeraho ayasohotse",
    what: "Iki", who: "Nde", split: "Gutandukanya", level: "Urwego", format: "Ubwoko", everyone: "Bose",
    salaries: "Imishahara gusa", bonusesOnly: "Ibihembo gusa", expensesOnly: "Ayasohotse gusa", finesOnly: "Ibihano gusa",
    socialOnly: "Ikigega gusa", receiptsOnly: "Inyemezabwishyu gusa", full: "Raporo yose",
    single: "Inyandiko imwe", details: "Ibisobanuro", summaryLevel: "Incamake",
    receiptNote: "Umukono wo kuri ecran ni icyemezo cy'imbere. Amafaranga menshi agomba kugira kopi y'impapuro.",
    generate: "Kora inyemezabwishyu", download: "Kuramo", retry: "Ongera", emptyChart: "Nta mibare kuri iki gishushanyo.",
    rule: "Itegeko", balance: "Asigaye", paidCol: "Byishyuwe",
  },
};

const KIND = {
  en: { ADVANCE_AT_SIGNING: "Advance at signing", ENVELOPE_M1_M3: "Envelope, months 1 to 3", MONTH_4: "Month 4", MONTH_5: "Month 5", MONTH_6: "Month 6", MONTH_7: "Month 7", MONTH_8: "Month 8" },
  fr: { ADVANCE_AT_SIGNING: "Avance à la signature", ENVELOPE_M1_M3: "Enveloppe, mois 1 à 3", MONTH_4: "Mois 4", MONTH_5: "Mois 5", MONTH_6: "Mois 6", MONTH_7: "Mois 7", MONTH_8: "Mois 8" },
  rw: { ADVANCE_AT_SIGNING: "Inyongera", ENVELOPE_M1_M3: "Ibaruwa, ukwezi 1 kugeza 3", MONTH_4: "Ukwezi 4", MONTH_5: "Ukwezi 5", MONTH_6: "Ukwezi 6", MONTH_7: "Ukwezi 7", MONTH_8: "Ukwezi 8" },
};
const PAY = {
  en: { NOT_PAID: "Pending", PARTIALLY_PAID: "Partial", PAID: "Paid", OVERDUE: "Overdue", OUTSTANDING: "Outstanding", WAIVED: "Waived", PENDING: "Pending", APPROVED: "Approved", CASH: "Cash", MOBILE_MONEY: "Mobile money", BANK: "Bank", DEDUCTION: "Deduction" },
  fr: { NOT_PAID: "En attente", PARTIALLY_PAID: "Partiel", PAID: "Payé", OVERDUE: "En retard", OUTSTANDING: "Dû", WAIVED: "Annulé", PENDING: "En attente", APPROVED: "Approuvé", CASH: "Espèces", MOBILE_MONEY: "Mobile money", BANK: "Banque", DEDUCTION: "Retenue" },
  rw: { NOT_PAID: "Ntirishyurwa", PARTIALLY_PAID: "Byishyuwe bike", PAID: "Byishyuwe", OVERDUE: "Byarengeje", OUTSTANDING: "Bitarishyurwa", WAIVED: "Byakuweho", PENDING: "Bitegerejwe", APPROVED: "Byemejwe", CASH: "Amafaranga", MOBILE_MONEY: "Mobile money", BANK: "Banki", DEDUCTION: "Ibikurwaho" },
};
const CATEGORIES = {
  en: ["Equipment and materials", "Kits and uniforms", "Training ground", "Match costs", "Transport", "Medical and emergency", "Administration and office", "Marketing and media", "Competition and federation fees", "Social fund", "Other"],
  fr: ["Équipement et matériel", "Tenues", "Terrain d'entraînement", "Frais de match", "Transport", "Médical et urgences", "Administration", "Marketing et médias", "Frais de compétition", "Fonds social", "Autre"],
  rw: ["Ibikoresho", "Imyambaro", "Ikibuga cy'imyitozo", "Amafaranga ya match", "Ubwikorezi", "Ubuvuzi", "Ubuyobozi", "Itangazamakuru", "Amarushanwa", "Ikigega", "Ibindi"],
};
const GROUP_ICON = { players: Users, staff: Shield, administration: Wallet };

function still() {
  return typeof window !== "undefined" && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}
function compact(value) {
  const amount = Number(value) || 0;
  const abs = Math.abs(amount);
  const sign = amount < 0 ? "-" : "";
  if (abs >= 1000000) return `${sign}RWF ${(abs / 1000000).toFixed(2)}M`;
  return rwf(amount);
}
function labelOf(map, locale, code) {
  if (!code) return "";
  return (map[locale] || map.en)[code] || String(code).replaceAll("_", " ").toLowerCase();
}
function ago(value) {
  if (!value) return "";
  const minutes = Math.max(0, Math.round((Date.now() - new Date(value).getTime()) / 60000));
  if (minutes < 60) return `${minutes} min`;
  const hours = Math.round(minutes / 60);
  if (hours < 48) return `${hours} h`;
  return `${Math.round(hours / 24)} d`;
}
function Spark({ points }) {
  const values = (points || []).map((point) => Number(point.amount) || 0);
  if (values.length < 2) return null;
  const max = Math.max(...values);
  const min = Math.min(...values);
  const path = values.map((value, index) => {
    const x = (index / (values.length - 1)) * 120;
    const y = 32 - ((value - min) / (max - min || 1)) * 28;
    return `${index ? "L" : "M"}${x.toFixed(1)},${y.toFixed(1)}`;
  }).join(" ");
  return <svg className="finance-spark" viewBox="0 0 120 32" width="120" height="32" aria-hidden="true"><path d={path} /></svg>;
}
function Kpis({ items }) {
  return (
    <div className="crud-kpis finance-kpis">
      {items.map((item) => (
        <article key={item.label} className="crud-kpi" title={item.title || ""}>
          <strong>{item.value}</strong>
          <span>{item.label}</span>
        </article>
      ))}
    </div>
  );
}
async function postData(path, body) {
  const response = await api.post(path, body);
  if (!response.data || response.data.success === false) throw new Error(response.data?.message || "Request failed");
  return response.data.data;
}
function moneyText(value) {
  if (value == null || value === "") return "–";
  const text = String(value);
  const number = text.match(/-?\d+(?:\.\d+)?/);
  if (!number) return text;
  return text.replace(number[0], rwf(number[0]));
}

function SocialCard({ person, payroll, t }) {
  const phase1 = Number(payroll?.socialFundPhase1 || 0);
  const phase2 = Number(payroll?.socialFundPhase2 || 0);
  const due = Number(payroll?.totalSocialFund ?? person.social ?? 0);
  const exempt = Boolean(person.sfExempt) || due === 0;
  return (
    <div>
      <Kpis items={[
        { label: t.socialTab, value: rwf(due), title: rwf(due) },
        { label: t.paid, value: exempt ? rwf(0) : rwf(phase1), title: rwf(phase1) },
        { label: t.stillToPay, value: exempt ? rwf(0) : rwf(phase2), title: rwf(phase2) },
      ]} />
      <div className="crud-scroll">
        <table className="crud-table">
          <thead><tr><th>{t.socialTab}</th><th>{t.amount}</th><th>{t.status}</th></tr></thead>
          <tbody>
            <tr><td>{t.socialTab} 1</td><td>{rwf(phase1)}</td><td><Badge>{exempt ? t.exempt : t.paid}</Badge></td></tr>
            <tr><td>{t.socialTab} 2</td><td>{rwf(phase2)}</td><td><Badge>{exempt ? t.exempt : t.stillToPay}</Badge></td></tr>
          </tbody>
        </table>
      </div>
    </div>
  );
}

function payProblems(form, socialFund, t) {
  const pay = paySummary({ seasonalPay: form.seasonalPay, advance: form.advance, socialFund, sfExempt: form.sfExempt });
  const rows = [];
  if (!(Number(form.seasonalPay) > 0)) rows.push(t.needSalary);
  if (form.advance === "" || Number(form.advance) < 0 || Number(form.advance) > Number(form.seasonalPay || 0)) rows.push(t.needAdvance);
  if (!String(form.reason || "").trim()) rows.push(t.needReason);
  if (pay.shortfall) rows.push(t.socialShort.replace("{monthly}", rwf(pay.monthly)).replace("{three}", rwf(pay.three)).replace("{fund}", rwf(pay.social1)));
  return rows;
}

function TotalsBoard({ overview, t, onOpen }) {
  const [bonuses, setBonuses] = useState([]);
  useEffect(() => {
    getData("/api/bonuses/by-player").then((rows) => setBonuses(Array.isArray(rows) ? rows : [])).catch(() => setBonuses([]));
  }, []);
  const groups = ["players", "staff", "administration"].map((key) => {
    const people = overview?.groups?.[key]?.people || [];
    const rows = people.map((person) => {
      const bonus = key === "players" ? bonuses.find((row) => String(row.id) === String(person.id)) : null;
      const unpaid = Number(bonus?.accrued || 0) + Number(bonus?.approved || 0);
      const still = Number(person.netDue || 0) + unpaid - Number(person.fines || 0);
      return { ...person, unpaid, still, bonusPaid: Number(bonus?.paid || 0), group: key };
    });
    return {
      key,
      rows,
      paid: rows.reduce((sum, row) => sum + Number(row.paid || 0) + row.bonusPaid, 0),
      still: rows.reduce((sum, row) => sum + row.still, 0),
      bonusPaid: rows.reduce((sum, row) => sum + row.bonusPaid, 0),
      unpaid: rows.reduce((sum, row) => sum + row.unpaid, 0),
    };
  });
  const grandPaid = groups.reduce((sum, group) => sum + group.paid, 0);
  const grandStill = groups.reduce((sum, group) => sum + group.still, 0);
  return (
    <div>
      <Kpis items={[
        { label: t.paid, value: compact(grandPaid), title: rwf(grandPaid) },
        { label: t.stillToPay, value: compact(grandStill), title: rwf(grandStill) },
        { label: t.unpaidBonuses, value: compact(groups.reduce((sum, group) => sum + group.unpaid, 0)), title: rwf(groups.reduce((sum, group) => sum + group.unpaid, 0)) },
      ]} />
      {groups.map((group) => (
        <section key={group.key} className="crud-card">
          <h2>{t[group.key]} · {t.paid} {rwf(group.paid)} · {t.stillToPay} {rwf(group.still)}</h2>
          <div className="crud-scroll">
            <table className="crud-table finance-table">
              <thead><tr><th>{t.person}</th><th>{t.seasonal}</th><th>{t.paid}</th><th>{t.net}</th><th>{t.unpaidBonuses}</th><th>{t.finesDue}</th><th>{t.stillToPay}</th><th>{t.actions}</th></tr></thead>
              <tbody>
                {group.rows.map((row) => (
                  <tr key={`${group.key}-${row.id}`}>
                    <td><span className="finance-person"><Avatar name={row.fullName} src={row.photoUrl} size="sm" />{row.fullName}</span></td>
                    <td>{rwf(row.seasonal)}</td><td>{rwf(row.paid)}</td><td>{rwf(row.netDue)}</td><td>{rwf(row.unpaid)}</td><td>{rwf(row.fines)}</td><td>{rwf(row.still)}</td>
                    <td className="finance-actions"><IconButton label={t.view} onClick={() => onOpen(row, "summary")}><Eye size={16} /></IconButton></td>
                  </tr>
                ))}
              </tbody>
              <tfoot><tr className="finance-total"><th>{t.total}</th><th /><th>{rwf(group.paid)}</th><th /><th>{rwf(group.unpaid)}</th><th /><th>{rwf(group.still)}</th><th /></tr></tfoot>
            </table>
          </div>
        </section>
      ))}
    </div>
  );
}

function PayCard({ form, socialFund, t }) {
  const pay = paySummary({ seasonalPay: form.seasonalPay, advance: form.advance, socialFund, sfExempt: form.sfExempt });
  return (
    <div className="finance-facts">
      <p><span>{t.monthly}</span><strong>{rwf(pay.monthly)}</strong></p>
      <p><span>3</span><strong>{rwf(pay.three)}</strong></p>
      <p><span>{t.social}</span><strong>{rwf(pay.social1)}</strong></p>
      <p><span>{t.net}</span><strong>{rwf(pay.balance)}</strong></p>
      <p><span>{t.envelopes}</span><strong>{rwf(pay.envelope)}</strong></p>
    </div>
  );
}

function downloadBlob(data, filename) {
  const url = URL.createObjectURL(data);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}

export function FinanceOffice({ locale = "en" }) {
  const t = COPY[locale] || COPY.en;
  const charts = CHARTS.map((row) => ({ id: row[0], title: row[locale === "fr" ? 2 : locale === "rw" ? 3 : 1] }));
  const [tab, setTab] = useState("overview");
  const [overviewGroup, setOverviewGroup] = useState("all");
  const [period, setPeriod] = useState("season");
  const [groupBy, setGroupBy] = useState("month");
  const [group, setGroup] = useState("players");
  const [socialFund, setSocialFund] = useState(0);
  const [overview, setOverview] = useState(null);
  const [fines, setFines] = useState(null);
  const [ledger, setLedger] = useState([]);
  const [expenses, setExpenses] = useState([]);
  const [reports, setReports] = useState([]);
  const [chartData, setChartData] = useState({});
  const [error, setError] = useState("");
  const [query, setQuery] = useState("");
  const [sort, setSort] = useState("name");
  const [person, setPerson] = useState(null);
  const [dossier, setDossier] = useState(null);
  const [personTab, setPersonTab] = useState("summary");
  const [exportOpen, setExportOpen] = useState(false);
  const [expenseOpen, setExpenseOpen] = useState(false);
  const [contractFor, setContractFor] = useState(null);
  const [form, setForm] = useState({});

  async function load() {
    const [board, fineSummary, rows, costs, history] = await Promise.all([
      getData("/api/finance/overview"),
      getData("/api/fines/summary"),
      getData("/api/fines/ledger"),
      getData("/api/finance/expenses"),
      getData("/api/finance/reports"),
    ]);
    setOverview(board);
    setFines(fineSummary);
    setLedger(Array.isArray(rows) ? rows : []);
    setExpenses(Array.isArray(costs) ? costs : []);
    setReports(Array.isArray(history) ? history : []);
    setError("");
  }

  async function loadCharts(nextGroupBy = groupBy) {
    const entries = await Promise.all(charts.map(async (chart) => {
      try {
        return [chart.id, await getData(`/api/finance/analytics/${chart.id}`, { groupBy: nextGroupBy })];
      } catch (err) {
        return [chart.id, { empty: true, points: [], error: err.message }];
      }
    }));
    setChartData(Object.fromEntries(entries));
  }

  useEffect(() => {
    const next = new URLSearchParams(window.location.search).get("group");
    if (["players", "staff", "administration", "bonuses", "totals"].includes(next)) setGroup(next);
    getData("/api/settings").then((view) => setSocialFund(Number(view?.playerSocialFund || 0))).catch(() => {});
    load().catch((err) => setError(err.message || "Finance"));
    loadCharts(groupBy);
  }, [locale]);

  function chooseGroup(id) {
    setGroup(id);
    const url = new URL(window.location.href);
    url.searchParams.set("group", id);
    window.history.replaceState(null, "", url);
  }

  const block = overview?.groups?.[group] || { people: [], headcount: 0 };
  const people = block.people || [];
  const shown = useMemo(() => {
    const needle = query.trim().toLowerCase();
    const rows = people.filter((row) => `${row.fullName} ${row.role} ${row.line || ""}`.toLowerCase().includes(needle));
    rows.sort((a, b) => {
      if (sort === "name") return String(a.fullName).localeCompare(String(b.fullName));
      if (sort === "low") return Number(a.seasonal) - Number(b.seasonal);
      if (sort === "due") return Number(b.netDue) - Number(a.netDue);
      return Number(b.seasonal) - Number(a.seasonal);
    });
    return rows;
  }, [people, query, sort]);

  async function openPerson(row) {
    setPerson(row);
    setPersonTab("summary");
    setDossier(null);
    try {
      setDossier(await getData(`/api/finance/people/${row.personType}/${row.id}`));
    } catch (err) {
      setError(err.message || "");
    }
  }

  async function savePayment(event) {
    event.preventDefault();
    const chosen = String(form.months || form.instalmentId || "").split(",").filter(Boolean);
    const open = (dossier?.instalments || []).filter((row) => chosen.includes(String(row.id)));
    let left = Number(form.amount);
    const plan = open.map((row) => {
      const applied = Math.min(Number(row.cashRemaining), Math.max(0, left));
      left -= applied;
      return { id: row.id, applied };
    }).filter((row) => row.applied > 0);
    for (const line of plan) {
      await postData(`/api/finance/instalments/${line.id}/payments`, {
        amount: line.applied,
        method: form.method || "CASH",
        reference: form.reference || "",
        paidOn: form.paidOn || new Date().toISOString().slice(0, 10),
      });
    }
    setForm({});
    await load();
    if (person) setDossier(await getData(`/api/finance/people/${person.personType}/${person.id}`));
  }

  async function reversePayment(payment) {
    const reason = window.prompt(t.reason);
    if (!reason) return;
    await postData(`/api/finance/payments/${payment.id}/reverse`, { reason });
    await load();
    if (person) setDossier(await getData(`/api/finance/people/${person.personType}/${person.id}`));
  }

  async function saveContract(event) {
    event?.preventDefault();
    const pay = paySummary({ seasonalPay: form.seasonalPay, advance: form.advance, socialFund, sfExempt: form.sfExempt });
    if (!(Number(form.seasonalPay) > 0) || Number(form.advance) < 0 || Number(form.advance) > Number(form.seasonalPay) || !String(form.reason || "").trim() || pay.shortfall) return;
    await postData(`/api/finance/people/${contractFor.personType}/${contractFor.id}/contract`, {
      seasonalPay: Number(form.seasonalPay),
      advance: Number(form.advance || 0),
      sfExempt: Boolean(form.sfExempt),
      reason: form.reason,
    });
    setContractFor(null);
    setForm({});
    await load();
  }

  async function saveExpense(event) {
    event.preventDefault();
    await postData("/api/finance/expenses", {
      category: form.category,
      description: form.customLabel || form.description,
      customLabel: form.customLabel || "",
      amount: Number(form.amount),
      expenseDate: form.expenseDate || new Date().toISOString().slice(0, 10),
      vendorName: form.vendor || "",
      method: form.method || "CASH",
      status: "PAID",
      note: form.note || "",
      linkedLabel: form.linked || "",
    });
    setExpenseOpen(false);
    setForm({});
    await load();
  }

  async function runExport(event) {
    event.preventDefault();
    const response = await api.post("/api/finance/exports", {
      what: form.what || "FULL",
      who: form.who || "ALL",
      period: period.toUpperCase(),
      split: form.split || "SINGLE",
      level: form.level || "DETAILS",
      format: form.format || "XLSX",
      person: form.person || "",
    }, { responseType: "blob" });
    const named = /filename="([^"]+)"/.exec(response.headers?.["content-disposition"] || "");
    const extension = (form.format || "XLSX").toLowerCase() === "pdf" ? "pdf" : (form.format || "XLSX").toLowerCase() === "csv" ? "csv" : "xlsx";
    downloadBlob(response.data, named?.[1] || `VIKING-FC_${form.what || "Full"}_${form.who || "All"}.${extension}`);
    setExportOpen(false);
    setReports(await getData("/api/finance/reports"));
  }

  async function makeReceipt() {
    const amount = Number(person.paid) || Number(form.amount);
    if (!amount) {
      setError(t.amount);
      return;
    }
    const saved = await postData("/api/finance/receipts", {
      personType: person.personType,
      personId: person.id,
      personName: person.fullName,
      amount,
    });
    const response = await api.get(`/api/finance/receipts/${saved.id}/pdf`, { responseType: "blob" });
    downloadBlob(response.data, `${saved.number}.pdf`);
    if (person) setDossier(await getData(`/api/finance/people/${person.personType}/${person.id}`));
  }

  const rise = still() ? {} : { initial: { opacity: 0, y: 12 }, animate: { opacity: 1, y: 0 } };
  const names = ["players", "staff", "administration"].flatMap((key) => overview?.groups?.[key]?.people || []);

  return (
    <section className="finance-page">
      <motion.header className="att-head" {...rise}>
        <div>
          <h1>{t.title} {overview?.season ? <span className="finance-season">{overview.season}</span> : null}</h1>
          <p>{t.subtitle}</p>
        </div>
        <Button variant="primary" icon={<Download size={16} />} onClick={() => { setForm({ what: "FULL", who: "ALL", format: "XLSX", level: "DETAILS", split: "SINGLE" }); setExportOpen(true); }}>{t.export}</Button>
      </motion.header>
      {error ? <p className="crud-empty">{error} <Button onClick={() => load().catch((err) => setError(err.message))}>{t.retry}</Button></p> : null}
      <Tabs
        label={t.title}
        value={tab}
        onChange={setTab}
        items={[
          { id: "overview", label: t.overview },
          { id: "payroll", label: t.payroll },
          { id: "fines", label: t.fines },
          { id: "expenses", label: t.expenses },
          { id: "reports", label: t.reports },
        ]}
      />

      {tab === "overview" && overview ? (
        <motion.div {...rise}>
          <div className="att-filters">
            <Field label={t.period}>
              <SelectInput aria-label={t.period} value={period} onChange={(event) => setPeriod(event.target.value)}>
                <option value="season">{t.thisSeason}</option>
                <option value="year">{t.thisYear}</option>
                <option value="month">{t.thisMonth}</option>
              </SelectInput>
            </Field>
            <Field label={t.groupBy}>
              <SelectInput aria-label={t.groupBy} value={groupBy} onChange={(event) => { setGroupBy(event.target.value); loadCharts(event.target.value); }}>
                <option value="day">{t.day}</option>
                <option value="week">{t.week}</option>
                <option value="month">{t.month}</option>
                <option value="year">{t.year}</option>
              </SelectInput>
            </Field>
            <Field label={t.season}>
              <SelectInput aria-label={t.season} value={overview.season || ""} onChange={() => {}}>
                <option value={overview.season || ""}>{overview.season}</option>
              </SelectInput>
            </Field>
            <Field label={t.allGroups}>
              <SelectInput aria-label={t.allGroups} value={overviewGroup} onChange={(event) => setOverviewGroup(event.target.value)}>
                <option value="all">{t.allGroups}</option>
                <option value="players">{t.players}</option>
                <option value="staff">{t.staff}</option>
                <option value="administration">{t.administration}</option>
              </SelectInput>
            </Field>
          </div>
          <Kpis items={[
            ["cashBalance", t.cash, "cashBalance"],
            ["seasonPayroll", t.payrollTotal, "seasonPayroll"],
            ["paid", t.paid, "paid"],
            ["netDue", t.net, "netDue"],
            ["expenses", t.expenseTotal, "expenses"],
            ["bonusesPaid", t.bonusesPaid, "bonusesPaid"],
            ["finesReceivable", t.finesDue, "fines"],
            ["socialBalance", t.social, "social"],
          ].map(([field, label, groupField]) => {
            const amount = overviewGroup === "all" || !["seasonPayroll", "paid", "netDue"].includes(field)
              ? Number(overview.kpis?.[field] || 0)
              : Number(overview.groups?.[overviewGroup]?.[groupField] || 0);
            return { label, value: compact(amount), title: field === "netDue" ? `${t.netHint} ${rwf(amount)}` : rwf(amount) };
          })} />
          <div className="finance-split">
            {(overviewGroup === "all" ? ["players", "staff", "administration"] : [overviewGroup]).map((key) => {
              const Icon = GROUP_ICON[key];
              const card = overview.groups[key];
              const faces = card.people || [];
              return (
                <article key={key} className="finance-card">
                  <header>
                    <span className="finance-card-icon"><Icon size={18} /></span>
                    <div>
                      <h2>{t[key]}</h2>
                      <small>{card.headcount} {t.headcount.toLowerCase()}</small>
                    </div>
                    <div className="finance-ring" style={{ "--pct": Math.min(100, Number(card.percent) || 0) }} title={`${card.percent}% ${t.paidPct}`}>
                      <span>{card.percent}%</span>
                    </div>
                  </header>
                  <strong title={rwf(card.seasonPayroll)}>{compact(card.seasonPayroll)}</strong>
                  <Spark points={card.sparkline} />
                  <ul>
                    <li><span>{t.paid}</span><b>{rwf(card.paid)}</b></li>
                    <li><span>{t.stillOwed}</span><b title={t.netHint}>{rwf(card.netDue)}</b></li>
                    <li><span>{t.average}</span><b>{rwf(card.average)}</b></li>
                  </ul>
                  <div className="finance-faces">
                    {faces.slice(0, 5).map((row) => <Avatar key={row.id} name={row.fullName} src={row.photoUrl} size="sm" />)}
                    {faces.length > 5 ? <em>+{faces.length - 5}</em> : null}
                  </div>
                  <Button onClick={() => { setTab("payroll"); setGroup(key); }}>{t.viewPayroll}</Button>
                </article>
              );
            })}
          </div>
          {(overview.payNotSet || []).length ? (
            <article className="admin-card">
              <h2>{t.payNotSet}</h2>
              <div className="crud-scroll">
                <table className="crud-table">
                  <thead><tr><th>{t.person}</th><th>{t.role}</th><th>{t.actions}</th></tr></thead>
                  <tbody>
                    {overview.payNotSet.map((row) => (
                      <tr key={`${row.personType}-${row.id}`}>
                        <td><span className="finance-person"><Avatar name={row.fullName} src={row.photoUrl} size="sm" />{row.fullName}{row.jerseyNumber ? ` #${row.jerseyNumber}` : ""}</span></td>
                        <td>{row.role}</td>
                        <td><Button icon={<Plus size={16} />} onClick={() => { setContractFor(row); setForm({ seasonalPay: "", advance: "0" }); }}>{t.addPay}</Button></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </article>
          ) : null}
          <div className="finance-charts">
            {charts.map((chart) => {
              const data = chartData[chart.id];
              const points = data?.points || [];
              const series = (data?.series || []).filter((key) => points.some((point) => point[key] != null));
              return (
                <article key={chart.id} className="admin-card">
                  <h2>{chart.title}</h2>
                  <Field label={t.allGroups}>
                    <SelectInput aria-label={chart.title} value={overviewGroup} onChange={(event) => getData(`/api/finance/analytics/${chart.id}`, { group: event.target.value, groupBy }).then((next) => setChartData((current) => ({ ...current, [chart.id]: next }))).catch(() => {})}>
                      <option value="all">{t.allGroups}</option>
                      <option value="players">{t.players}</option>
                      <option value="staff">{t.staff}</option>
                      <option value="administration">{t.administration}</option>
                    </SelectInput>
                  </Field>
                  {data?.empty || !points.length ? <p className="crud-empty">{t.emptyChart}</p> : (
                    <div className={`finance-chart${["player-ranking", "admin-by-person", "staff-by-role", "top-bonus-earners"].includes(chart.id) ? " finance-chart--rank" : ""}`}>
                      <ResponsiveContainer width="100%" height={["player-ranking", "admin-by-person", "staff-by-role", "top-bonus-earners"].includes(chart.id) ? Math.max(440, points.length * 36) : 380}>
                        <BarChart layout={["player-ranking", "admin-by-person", "staff-by-role", "top-bonus-earners"].includes(chart.id) ? "vertical" : "horizontal"} data={points.map((point) => ({ ...point, name: point.label }))}>
                          <CartesianGrid strokeDasharray="3 3" vertical={false} />
                          {["player-ranking", "admin-by-person", "staff-by-role", "top-bonus-earners"].includes(chart.id) ? (
                            <>
                              <XAxis type="number" tickFormatter={(value) => compact(value)} />
                              <YAxis type="category" dataKey="name" width={180} tick={{ fontSize: 12 }} />
                            </>
                          ) : (
                            <>
                              <XAxis dataKey="name" interval={0} tick={{ fontSize: 12 }} />
                              <YAxis tickFormatter={(value) => series.includes("players") ? String(value) : compact(value)} width={72} />
                            </>
                          )}
                          <Tooltip formatter={(value) => rwf(value)} />
                          {(series.length ? series : ["pay", "amount", "income"]).slice(0, 3).map((key, index) => (
                            <Bar key={key} dataKey={key} fill={index ? "#1f2933" : "#a3283a"} radius={[6, 6, 0, 0]} />
                          ))}
                        </BarChart>
                      </ResponsiveContainer>
                    </div>
                  )}
                </article>
              );
            })}
          </div>
        </motion.div>
      ) : null}

      {tab === "payroll" ? (
        <motion.div {...rise}>
          <div className="finance-sticky">
            <Tabs label={t.payroll} value={group} onChange={chooseGroup} id="finance-group" items={[{ id: "players", label: t.players }, { id: "staff", label: t.staff }, { id: "administration", label: t.administration }, { id: "bonuses", label: t.bonuses }, { id: "totals", label: t.totals }]} />
          </div>
          {group === "bonuses" ? <BonusesPanel locale={locale} /> : group === "totals" ? <TotalsBoard locale={locale} overview={overview} t={t} onOpen={(row, nextTab) => { openPerson(row); setPersonTab(nextTab); }} /> : overview ? (
          <>
          <Kpis items={[
            { label: t.payrollTotal, value: compact(block.seasonPayroll), title: rwf(block.seasonPayroll) },
            { label: t.monthly, value: compact(block.monthly), title: rwf(block.monthly) },
            { label: t.advances, value: compact(block.advances), title: rwf(block.advances) },
            { label: t.envelopes, value: compact(block.envelopes), title: rwf(block.envelopes) },
            { label: t.paid, value: compact(block.paid), title: rwf(block.paid) },
            { label: t.net, value: compact(block.netDue), title: `${t.netHint} ${rwf(block.netDue)}` },
          ]} />
          <div className="att-filters">
            <Field label={t.search}><TextInput value={query} onChange={(event) => setQuery(event.target.value)} /></Field>
            <Field label={t.sort}>
              <SelectInput aria-label={t.sort} value={sort} onChange={(event) => setSort(event.target.value)}>
                <option value="name">{t.name}</option>
                <option value="high">{t.high}</option>
                <option value="low">{t.low}</option>
                <option value="due">{t.due}</option>
              </SelectInput>
            </Field>
          </div>
          {!shown.length ? <p className="crud-empty">{t.empty}</p> : (
            <div className="crud-scroll">
              <table className="crud-table finance-table">
                <thead>
                  <tr>
                    <th>{t.person}</th><th>{t.role}</th><th>{t.seasonal}</th><th>{t.paid}</th><th>{t.net}</th><th>{t.finesDue}</th><th>{t.payable}</th><th>{t.modified}</th><th>{t.actions}</th>
                  </tr>
                </thead>
                <tbody>
                  {shown.map((row) => (
                    <tr key={row.id}>
                      <td><span className="finance-person"><Avatar name={row.fullName} src={row.photoUrl} size="sm" />{row.fullName}{row.jerseyNumber ? ` #${row.jerseyNumber}` : ""}</span></td>
                      <td>{row.role}</td>
                      <td>{rwf(row.seasonal)}</td>
                      <td>{rwf(row.paid)}</td>
                      <td title={t.netHint}>{rwf(row.netDue)}</td>
                      <td>{rwf(row.fines)}</td>
                      <td>{rwf(row.netPayable)}</td>
                      <td>
                        <span className="finance-modified">
                          <Avatar name={row.updatedBy || row.fullName} size="xs" />
                          <span>{row.updatedBy}<small>{ago(row.updatedAt)}</small></span>
                        </span>
                      </td>
                      <td className="finance-actions">
                        <IconButton label={t.view} onClick={() => openPerson(row)}><Eye size={16} /></IconButton>
                        <IconButton label={t.record} onClick={() => { openPerson(row); setPersonTab("payments"); }}><Plus size={16} /></IconButton>
                        <IconButton label={t.editPay} onClick={() => { setContractFor(row); setForm({ seasonalPay: row.seasonal, advance: row.signingAdvance || 0, sfExempt: Boolean(row.sfExempt), reason: "" }); }}><Pencil size={16} /></IconButton>
                        <IconButton label={t.history} onClick={() => { openPerson(row); setPersonTab("history"); }}><Clock size={16} /></IconButton>
                        <details className="finance-more">
                          <summary aria-label={t.more}>⋯</summary>
                          <button type="button" onClick={() => { openPerson(row); setPersonTab("payments"); }}>{t.several}</button>
                          <button type="button" onClick={() => { openPerson(row); setPersonTab("receipts"); }}>{t.receipts}</button>
                          <button type="button" onClick={() => { openPerson(row); setPersonTab("bonuses"); }}>{t.bonuses}</button>
                          <button type="button" onClick={() => { openPerson(row); setPersonTab("social"); }}>{t.socialTab}</button>
                          <button type="button" onClick={() => { openPerson(row); setPersonTab("deductions"); }}>{t.fines}</button>
                          <button type="button" onClick={() => { setForm({ what: "SALARIES", who: "ALL", period: "SEASON", split: "SINGLE", level: "DETAILS", format: "PDF", person: row.fullName }); setExportOpen(true); }}>{t.download}</button>
                        </details>
                      </td>
                    </tr>
                  ))}
                </tbody>
                <tfoot>
                  <tr className="finance-total">
                    <th>{t.total}</th><th>{block.headcount}</th><th>{rwf(block.seasonPayroll)}</th><th>{rwf(block.paid)}</th><th>{rwf(block.netDue)}</th><th>{rwf(block.fines)}</th><th>{rwf(block.netPayable)}</th><th /><th />
                  </tr>
                </tfoot>
              </table>
            </div>
          )}
          </>
          ) : null}
        </motion.div>
      ) : null}

      {tab === "fines" ? (
        <motion.div {...rise}>
          <Kpis items={[
            { label: t.issued, value: rwf(fines?.totalFinesDue), title: rwf(fines?.totalFinesDue) },
            { label: t.collected, value: rwf(fines?.collected), title: rwf(fines?.collected) },
            { label: t.outstanding, value: rwf(fines?.outstanding), title: rwf(fines?.outstanding) },
          ]} />
          {!ledger.length ? <p className="crud-empty">{t.noRows}</p> : (
            <div className="crud-scroll">
              <table className="crud-table">
                <thead><tr><th>{t.person}</th><th>{t.rule}</th><th>{t.date}</th><th>{t.amount}</th><th>{t.paidCol}</th><th>{t.balance}</th><th>{t.status}</th><th>{t.modified}</th></tr></thead>
                <tbody>
                  {ledger.map((row) => (
                    <tr key={row.id}>
                      <td><span className="finance-person"><Avatar name={row.player} src={row.photo} size="sm" />{row.player}</span></td>
                      <td>{row.rule || labelOf(PAY, locale, row.type)}</td>
                      <td>{row.date}</td>
                      <td>{rwf(row.amount)}</td>
                      <td>{rwf(row.paid)}</td>
                      <td>{rwf(row.balance)}</td>
                      <td><Badge>{labelOf(PAY, locale, row.status)}</Badge></td>
                      <td><span className="finance-modified"><Avatar name={row.updatedBy || row.player} size="xs" /><span>{row.updatedBy}<small>{ago(row.updatedAt)}</small></span></span></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          <p><Link href="/admin/fines">{t.openFines}</Link></p>
        </motion.div>
      ) : null}

      {tab === "expenses" ? (
        <motion.div {...rise}>
          <Button variant="primary" icon={<Plus size={16} />} onClick={() => { setForm({ category: (CATEGORIES[locale] || CATEGORIES.en)[0], method: "CASH" }); setExpenseOpen(true); }}>{t.addExpense}</Button>
          {!expenses.length ? <p className="crud-empty">{t.noRows}</p> : (
            <div className="crud-scroll">
              <table className="crud-table">
                <thead><tr><th>{t.date}</th><th>{t.category}</th><th>{t.description}</th><th>{t.vendor}</th><th>{t.amount}</th><th>{t.status}</th><th>{t.modified}</th><th>{t.actions}</th></tr></thead>
                <tbody>
                  {expenses.map((row) => (
                    <tr key={row.id}>
                      <td>{row.date}</td>
                      <td>{row.category}</td>
                      <td>{row.description}</td>
                      <td>{row.vendor}</td>
                      <td>{rwf(row.amount)}</td>
                      <td><Badge>{labelOf(PAY, locale, row.status)}</Badge></td>
                      <td><span className="finance-modified"><Avatar name={row.updatedBy || "Finance"} size="xs" /><span>{row.updatedBy}<small>{ago(row.updatedAt)}</small></span></span></td>
                      <td>{row.reversed ? null : <Button onClick={() => { const reason = window.prompt(t.reason); if (reason) postData(`/api/finance/expenses/${row.id}/reverse`, { reason }).then(load); }}>{t.reverse}</Button>}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </motion.div>
      ) : null}

      {tab === "reports" ? (
        <motion.div {...rise}>
          <div className="finance-split">
            {["SALARIES", "BONUSES", "EXPENSES", "FINES", "SOCIAL", "RECEIPTS", "FULL"].map((what) => (
              <article key={what} className="finance-card">
                <h2>{{ SALARIES: t.salaries, BONUSES: t.bonusesOnly, EXPENSES: t.expensesOnly, FINES: t.finesOnly, SOCIAL: t.socialOnly, RECEIPTS: t.receiptsOnly, FULL: t.full }[what]}</h2>
                <Button onClick={() => { setForm({ what, who: "ALL", format: "PDF", level: "DETAILS", split: "SINGLE" }); setExportOpen(true); }}>{t.export}</Button>
              </article>
            ))}
          </div>
          {!reports.length ? <p className="crud-empty">{t.noRows}</p> : (
            <div className="crud-scroll">
              <table className="crud-table">
                <thead><tr><th>{t.what}</th><th>{t.who}</th><th>{t.period}</th><th>{t.format}</th><th>{t.modified}</th></tr></thead>
                <tbody>
                  {reports.map((row) => (
                    <tr key={row.id}><td>{row.what}</td><td>{row.who}</td><td>{row.period}</td><td>{row.format}</td><td>{row.by} · {ago(row.at)}</td></tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </motion.div>
      ) : null}

      {person ? (
        <Modal title={person.fullName} subtitle={person.role} leading={<Avatar name={person.fullName} src={person.photoUrl} size="lg" />} size="xl" onClose={() => setPerson(null)} closeLabel={t.close}
          footer={<div className="vfc-foot-right"><Button onClick={makeReceipt}>{t.generate}</Button><Button variant="primary" onClick={() => setPerson(null)}>{t.close}</Button></div>}>
          <Kpis items={[
            { label: t.seasonal, value: rwf(person.seasonal), title: rwf(person.seasonal) },
            { label: t.paid, value: rwf(person.paid), title: rwf(person.paid) },
            { label: t.net, value: rwf(person.netDue), title: `${t.netHint} ${rwf(person.netDue)}` },
            { label: t.finesDue, value: rwf(person.fines), title: rwf(person.fines) },
            { label: t.payable, value: rwf(person.netPayable), title: rwf(person.netPayable) },
          ]} />
          <Tabs label={person.fullName} value={personTab} onChange={setPersonTab} id="finance-person" items={[
            { id: "summary", label: t.summary }, { id: "instalments", label: t.instalments }, { id: "payments", label: t.payments },
            { id: "deductions", label: t.deductions }, { id: "bonuses", label: t.bonuses }, { id: "social", label: t.socialTab },
            { id: "receipts", label: t.receipts }, { id: "history", label: t.history },
          ]} />
          {personTab === "summary" ? (
            <div className="finance-facts">
              <p><span>{t.monthly}</span><b>{rwf(dossier?.statement?.payroll?.monthlyPay ?? person.monthly)}</b></p>
              <p><span>{t.advances}</span><b>{rwf(person.advance)}</b></p>
              <p><span>{t.envelopes}</span><b>{rwf(dossier?.statement?.payroll?.envelopeAmount ?? person.envelope)}</b></p>
              <p><span>{t.socialTab}</span><b>{rwf(dossier?.statement?.payroll?.totalSocialFund ?? person.social)}</b></p>
              <p><span>{t.stillToPay}</span><b>{rwf(Number(person.netDue || 0) - Number(person.fines || 0))}</b></p>
            </div>
          ) : null}
          {personTab === "instalments" ? (
            dossier?.instalments?.length ? (
              <div className="crud-scroll"><table className="crud-table"><thead><tr><th>{t.instalments}</th><th>{t.date}</th><th>{t.amount}</th><th>{t.paidCol}</th><th>{t.balance}</th><th>{t.status}</th></tr></thead><tbody>
                {dossier.instalments.map((row) => <tr key={row.id}><td>{labelOf(KIND, locale, row.kind)}</td><td>{row.dueDate}</td><td>{rwf(row.amountDue)}</td><td>{rwf(row.amountPaid)}</td><td>{rwf(row.cashRemaining)}</td><td><Badge>{labelOf(PAY, locale, row.status)}</Badge></td></tr>)}
              </tbody></table></div>
            ) : <p className="crud-empty">{t.noRows}</p>
          ) : null}
          {personTab === "payments" ? (
            <div>
              <form className="att-filters" onSubmit={savePayment}>
                <div className="finance-months">{(dossier?.instalments || []).filter((row) => Number(row.cashRemaining) > 0).map((row) => {
                const picked = String(form.months || "").split(",").filter(Boolean);
                const on = picked.includes(String(row.id));
                return <Button key={row.id} variant={on ? "primary" : "secondary"} onClick={() => {
                  const next = on ? picked.filter((id) => id !== String(row.id)) : [...picked, String(row.id)];
                  const amount = (dossier?.instalments || []).filter((item) => next.includes(String(item.id))).reduce((sum, item) => sum + Number(item.cashRemaining), 0);
                  setForm({ ...form, months: next.join(","), amount });
                }}>{labelOf(KIND, locale, row.kind)} · {rwf(row.cashRemaining)}</Button>;
              })}</div>
                <Field label={t.amount}><TextInput required type="number" min="1" value={form.amount || ""} onChange={(event) => setForm({ ...form, amount: event.target.value })} /></Field>
                <Field label={t.method}><SelectInput value={form.method || "CASH"} onChange={(event) => setForm({ ...form, method: event.target.value })}><option value="CASH">{t.cashMethod}</option><option value="MOBILE_MONEY">{t.mobile}</option><option value="BANK">{t.bank}</option></SelectInput></Field>
                <Field label={t.reference}><TextInput value={form.reference || ""} onChange={(event) => setForm({ ...form, reference: event.target.value })} /></Field>
                <Field label={t.date}><TextInput type="date" value={form.paidOn || ""} onChange={(event) => setForm({ ...form, paidOn: event.target.value })} /></Field>
                <Button variant="primary" type="submit">{t.record}</Button>
              </form>
              {dossier?.payments?.length ? <div className="crud-scroll"><table className="crud-table"><thead><tr><th>{t.date}</th><th>{t.method}</th><th>{t.amount}</th><th>{t.reference}</th><th>{t.modified}</th><th>{t.actions}</th></tr></thead><tbody>
                {dossier.payments.map((row) => <tr key={row.id}><td>{row.paidAt}</td><td>{labelOf(PAY, locale, row.method)}</td><td>{rwf(row.amount)}</td><td>{row.reference}</td><td>{row.recordedBy}</td><td>{row.reversed ? null : <Button onClick={() => reversePayment(row)}>{t.reverse}</Button>}</td></tr>)}
              </tbody></table></div> : <p className="crud-empty">{t.noRows}</p>}
            </div>
          ) : null}
          {personTab === "deductions" ? (
            ledger.filter((row) => String(row.playerId) === String(person.id) || row.player === person.fullName).length
              ? <div className="crud-scroll"><table className="crud-table"><thead><tr><th>{t.rule}</th><th>{t.date}</th><th>{t.amount}</th><th>{t.balance}</th><th>{t.status}</th></tr></thead><tbody>
                {ledger.filter((row) => String(row.playerId) === String(person.id) || row.player === person.fullName).map((row) => <tr key={row.id}><td>{row.rule}</td><td>{row.date}</td><td>{rwf(row.amount)}</td><td>{rwf(row.balance)}</td><td><Badge>{labelOf(PAY, locale, row.status)}</Badge></td></tr>)}
              </tbody></table></div> : <p className="crud-empty">{t.noRows}</p>
          ) : null}
          {personTab === "bonuses" ? (dossier?.bonuses?.length ? <div className="crud-scroll"><table className="crud-table"><thead><tr><th>{t.bonuses}</th><th>{t.amount}</th><th>{t.status}</th><th>{t.date}</th></tr></thead><tbody>{dossier.bonuses.map((row) => <tr key={row.id}><td>{labelOf(PAY, locale, row.type)}</td><td>{rwf(row.amount)}</td><td><Badge>{labelOf(PAY, locale, row.status)}</Badge></td><td>{row.earnedOn}</td></tr>)}</tbody></table></div> : <p className="crud-empty">{t.noRows}</p>) : null}
          {personTab === "social" ? <SocialCard person={person} payroll={dossier?.statement?.payroll} t={t} /> : null}
          {personTab === "receipts" ? (
            <div>
              <p>{t.receiptNote}</p>
              {dossier?.receipts?.length ? dossier.receipts.map((row) => <p key={row.id}>{row.number} · {rwf(row.amount)} · {row.financeSigner} · {row.recipientSigner} <Button onClick={() => api.get(`/api/finance/receipts/${row.id}/pdf`, { responseType: "blob" }).then((response) => downloadBlob(response.data, `${row.number}.pdf`))}>{t.download}</Button></p>) : <p className="crud-empty">{t.noRows}</p>}
            </div>
          ) : null}
          {personTab === "history" ? (dossier?.history?.length ? <ol className="finance-timeline">{dossier.history.map((row, index) => <li key={`${row.at}-${index}`}><Avatar name={row.actor} size="sm" /><div><b>{row.actor}</b> <Badge>{labelOf(PAY, locale, row.action)}</Badge><small title={String(row.at || "")}>{ago(row.at)}</small><p>{row.field || t.seasonal}: <s>{moneyText(row.before)}</s> → <strong>{moneyText(row.after)}</strong></p>{row.reason ? <em>{row.reason}</em> : null}</div></li>)}</ol> : <p className="crud-empty">{t.noRows}</p>) : null}
        </Modal>
      ) : null}

      {exportOpen ? (
        <Modal title={t.export} size="md" onClose={() => setExportOpen(false)} closeLabel={t.close} footer={<div className="vfc-foot-right"><Button onClick={() => setExportOpen(false)}>{t.close}</Button><Button variant="primary" onClick={runExport}>{t.download}</Button></div>}>
          <div className="att-filters">
            <Field label={t.what}><SelectInput value={form.what || "FULL"} onChange={(event) => setForm({ ...form, what: event.target.value })}><option value="SALARIES">{t.salaries}</option><option value="BONUSES">{t.bonusesOnly}</option><option value="EXPENSES">{t.expensesOnly}</option><option value="FINES">{t.finesOnly}</option><option value="SOCIAL">{t.socialOnly}</option><option value="RECEIPTS">{t.receiptsOnly}</option><option value="FULL">{t.full}</option></SelectInput></Field>
            <Field label={t.who}><SelectInput value={form.who || "ALL"} onChange={(event) => setForm({ ...form, who: event.target.value })}><option value="ALL">{t.everyone}</option><option value="PLAYERS">{t.players}</option><option value="STAFF">{t.staff}</option><option value="ADMINISTRATION">{t.administration}</option></SelectInput></Field>
            <Field label={t.person}><SelectInput value={form.person || ""} onChange={(event) => setForm({ ...form, person: event.target.value })}><option value="">{t.everyone}</option>{names.map((row) => <option key={`${row.personType}-${row.id}`} value={row.fullName}>{row.fullName}</option>)}</SelectInput></Field>
            <Field label={t.period}><SelectInput value={period} onChange={(event) => setPeriod(event.target.value)}><option value="season">{t.thisSeason}</option><option value="year">{t.thisYear}</option><option value="month">{t.thisMonth}</option></SelectInput></Field>
            <Field label={t.split}><SelectInput value={form.split || "SINGLE"} onChange={(event) => setForm({ ...form, split: event.target.value })}><option value="SINGLE">{t.single}</option><option value="MONTH">{t.month}</option><option value="PERSON">{t.person}</option></SelectInput></Field>
            <Field label={t.level}><SelectInput value={form.level || "DETAILS"} onChange={(event) => setForm({ ...form, level: event.target.value })}><option value="SUMMARY">{t.summaryLevel}</option><option value="DETAILS">{t.details}</option></SelectInput></Field>
            <Field label={t.format}><SelectInput value={form.format || "XLSX"} onChange={(event) => setForm({ ...form, format: event.target.value })}><option value="PDF">PDF</option><option value="XLSX">Excel</option><option value="CSV">CSV</option></SelectInput></Field>
          </div>
        </Modal>
      ) : null}

      {expenseOpen ? (
        <Modal title={t.addExpense} size="lg" onClose={() => setExpenseOpen(false)} closeLabel={t.close} footer={<div className="vfc-foot-right"><Button onClick={() => setExpenseOpen(false)}>{t.close}</Button><Button variant="primary" onClick={saveExpense}>{t.save}</Button></div>}>
          <div className="att-filters">
            <Field label={t.category}><SelectInput value={form.category || ""} onChange={(event) => setForm({ ...form, category: event.target.value })}>{(CATEGORIES[locale] || CATEGORIES.en).map((item) => <option key={item}>{item}</option>)}</SelectInput></Field>
            {/other|autre|ibindi/i.test(form.category || "") ? <Field label={t.expenseName || "Name of the expense"}><TextInput required minLength={3} maxLength={80} placeholder={t.expenseHint || "e.g. Match balls"} value={form.customLabel || ""} onChange={(event) => setForm({ ...form, customLabel: event.target.value })} /></Field> : null}
            <Field label={t.description}><TextInput {...(/other|autre|ibindi/i.test(form.category || "") ? {} : { required: true })} value={form.description || ""} onChange={(event) => setForm({ ...form, description: event.target.value })} /></Field>
            <Field label={t.amount}><TextInput required type="number" min="1" value={form.amount || ""} onChange={(event) => setForm({ ...form, amount: event.target.value })} /></Field>
            <Field label={t.date}><TextInput required type="date" value={form.expenseDate || ""} onChange={(event) => setForm({ ...form, expenseDate: event.target.value })} /></Field>
            <Field label={t.vendor}><TextInput value={form.vendor || ""} onChange={(event) => setForm({ ...form, vendor: event.target.value })} /></Field>
            <Field label={t.method}><SelectInput value={form.method || "CASH"} onChange={(event) => setForm({ ...form, method: event.target.value })}><option value="CASH">{t.cashMethod}</option><option value="MOBILE_MONEY">{t.mobile}</option><option value="BANK">{t.bank}</option></SelectInput></Field>
            <Field label={t.reason}><TextArea value={form.note || ""} onChange={(event) => setForm({ ...form, note: event.target.value })} /></Field>
          </div>
        </Modal>
      ) : null}

      {contractFor ? (
        <Modal title={contractFor.fullName} subtitle={t.editPay || "Edit pay"} leading={<Avatar name={contractFor.fullName} src={contractFor.photoUrl} size="lg" />} size="md" onClose={() => setContractFor(null)} closeLabel={t.close}
          footer={<div className="vfc-foot-right">{payProblems(form, socialFund, t).length ? <span className="vfc-error">{payProblems(form, socialFund, t).join(" · ")}</span> : null}<Button onClick={() => setContractFor(null)}>{t.close}</Button><Button variant="primary" {...(payProblems(form, socialFund, t).length ? { disabled: true } : {})} onClick={saveContract}>{t.save}</Button></div>}>
          <Field label={t.seasonal}><TextInput required inputMode="numeric" value={form.seasonalPay || ""} onChange={(event) => setForm({ ...form, seasonalPay: event.target.value })} /></Field>
          <Field label={t.advances}><TextInput inputMode="numeric" value={form.advance ?? ""} onChange={(event) => setForm({ ...form, advance: event.target.value })} /></Field>
          <Switch checked={Boolean(form.sfExempt)} label={t.exempt || "Social fund exempt"} onChange={(checked) => setForm({ ...form, sfExempt: checked })} />
          <Field label={t.reason}><TextArea required value={form.reason || ""} onChange={(event) => setForm({ ...form, reason: event.target.value })} /></Field>
          <PayCard form={form} socialFund={socialFund} t={t} />
        </Modal>
      ) : null}
    </section>
  );
}
