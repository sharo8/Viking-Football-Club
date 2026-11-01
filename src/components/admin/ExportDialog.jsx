"use client";

import { useMemo, useState } from "react";
import { FileSpreadsheet, FileText, Download } from "lucide-react";
import { Button, Field, Modal, SelectInput, TextInput } from "@/ui/kit";

const PERIODS = [["today", "Today"], ["week", "This week"], ["month", "This month"], ["year", "This year"], ["all", "All time"], ["custom", "Custom range"]];
const SPLITS = [["none", "One document"], ["day", "One part per day"], ["week", "One part per week"], ["month", "One part per month"], ["player", "One part per player"]];
const LEVELS = [["summary", "Summary"], ["details", "Details"], ["both", "Summary + Details"]];

export function ExportDialog({ open, onClose, askContent, defaultResource = "FINES", defaultFormat = "pdf", players = [], busy, onDownload }) {
  const [resource, setResource] = useState(defaultResource);
  const [format, setFormat] = useState(defaultFormat);
  const [preset, setPreset] = useState("all");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [picked, setPicked] = useState([]);
  const [split, setSplit] = useState("none");
  const [level, setLevel] = useState("details");
  const [query, setQuery] = useState("");
  const shown = useMemo(() => players.filter((player) => `${player.fullName} ${player.jerseyNumber || ""}`.toLowerCase().includes(query.toLowerCase())), [players, query]);
  if (!open) return null;

  function toggle(id) {
    setPicked((current) => current.includes(id) ? current.filter((item) => item !== id) : [...current, id]);
  }

  return (
    <Modal title="Export" subtitle="Choose what goes in the file." size="lg" onClose={onClose} closeLabel="Cancel" footer={
      <>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="primary" icon={<Download size={16} />} loading={busy} onClick={() => onDownload({
          resource, format, scope: { preset, from, to }, people: picked, split, level,
          options: { includeFinesColumn: true },
        })}>Download</Button>
      </>
    }>
      {askContent ? (
        <div className="export-cards">
          {[["BOTH", "Attendance and Fines", "A presence sheet, then every fine."], ["ATTENDANCE", "Attendance only", "The grid, with a Fines column."], ["FINES", "Fines only", "The fine list and the totals."]].map(([id, title, text]) => (
            <Button key={id} variant={resource === id ? "primary" : "secondary"} onClick={() => setResource(id)}>{title}<small>{text}</small></Button>
          ))}
        </div>
      ) : null}
      <div className="export-grid">
        <Field label="Format">
          <div className="export-cards">
            <Button variant={format === "pdf" ? "primary" : "secondary"} icon={<FileText size={16} />} onClick={() => setFormat("pdf")}>PDF</Button>
            <Button variant={format === "xlsx" ? "primary" : "secondary"} icon={<FileSpreadsheet size={16} />} onClick={() => setFormat("xlsx")}>Excel</Button>
            <Button variant={format === "csv" ? "primary" : "secondary"} icon={<Download size={16} />} onClick={() => setFormat("csv")}>CSV</Button>
          </div>
        </Field>
        <Field label="Period">
          <SelectInput aria-label="Period" value={preset} onChange={(event) => setPreset(event.target.value)}>
            {PERIODS.map(([id, label]) => <option key={id} value={id}>{label}</option>)}
          </SelectInput>
        </Field>
        {preset === "custom" ? (
          <>
            <Field label="From"><TextInput type="date" value={from} onChange={(event) => setFrom(event.target.value)} /></Field>
            <Field label="To"><TextInput type="date" value={to} onChange={(event) => setTo(event.target.value)} /></Field>
          </>
        ) : null}
        <Field label="Split">
          <SelectInput aria-label="Split" value={split} onChange={(event) => setSplit(event.target.value)}>
            {SPLITS.map(([id, label]) => <option key={id} value={id}>{label}</option>)}
          </SelectInput>
        </Field>
        <Field label="Level">
          <SelectInput aria-label="Level" value={level} onChange={(event) => setLevel(event.target.value)}>
            {LEVELS.map(([id, label]) => <option key={id} value={id}>{label}</option>)}
          </SelectInput>
        </Field>
      </div>
      <Field label="Players">
        <TextInput aria-label="Search players" placeholder="Search a player or a jersey" value={query} onChange={(event) => setQuery(event.target.value)} />
        <div className="pick-list">
          {shown.map((player) => (
            <Button key={player.id} variant={picked.includes(player.id) ? "primary" : "secondary"} onClick={() => toggle(player.id)}>
              {player.fullName}{player.jerseyNumber ? ` #${player.jerseyNumber}` : ""}
            </Button>
          ))}
        </div>
        {picked.length ? <p className="vfc-muted">{picked.length} selected. Leave empty for everyone.</p> : <p className="vfc-muted">No one selected: the file includes every player.</p>}
      </Field>
    </Modal>
  );
}
