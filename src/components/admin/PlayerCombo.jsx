"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { ChevronDown } from "lucide-react";
import { Avatar, Button, TextInput } from "@/ui/kit";

const TEXT = {
  en: { all: "All players", one: "1 player selected", many: "{n} players selected", selectAll: "Select all", clear: "Clear", empty: "No matches" },
  fr: { all: "Tous les joueurs", one: "1 joueur sélectionné", many: "{n} joueurs sélectionnés", selectAll: "Tout sélectionner", clear: "Effacer", empty: "Aucun résultat" },
  rw: { all: "Abakinnyi bose", one: "Umukinnyi 1 wahiswemo", many: "Abakinnyi {n} bahiswemo", selectAll: "Hitamo byose", clear: "Siba", empty: "Nta bisubizo" },
};

function meta(person) {
  const jersey = person.jerseyNumber ?? person.jersey;
  const role = person.position || person.roleName || person.role || person.department || "";
  if (jersey !== undefined && jersey !== null && jersey !== "") return `#${jersey}${role ? ` · ${role}` : ""}`;
  return role;
}

function Highlight({ text, query }) {
  const value = text || "";
  const needle = query.trim();
  const index = needle ? value.toLowerCase().indexOf(needle.toLowerCase()) : -1;
  if (index < 0) return value;
  return (
    <>
      {value.slice(0, index)}
      <mark>{value.slice(index, index + needle.length)}</mark>
      {value.slice(index + needle.length)}
    </>
  );
}

export function PlayerCombo({ locale = "en", players = [], selected = [], onChange, placeholder }) {
  const copy = TEXT[locale] || TEXT.en;
  const box = useRef(null);
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [active, setActive] = useState(0);
  const chosen = selected.map(String);
  const shown = useMemo(() => players.filter((player) => {
    const blob = `${player.fullName || ""} ${player.knownAs || ""} ${player.jerseyNumber || ""} ${player.position || ""} ${player.roleName || player.role || ""}`.toLowerCase();
    return blob.includes(query.trim().toLowerCase());
  }), [players, query]);

  useEffect(() => {
    if (!open) return undefined;
    function close(event) {
      if (!box.current?.contains(event.target)) setOpen(false);
    }
    function escape(event) {
      if (event.key === "Escape") setOpen(false);
    }
    document.addEventListener("mousedown", close);
    document.addEventListener("keydown", escape);
    return () => {
      document.removeEventListener("mousedown", close);
      document.removeEventListener("keydown", escape);
    };
  }, [open]);

  useEffect(() => { setActive(0); }, [query, open]);

  function toggle(id) {
    const key = String(id);
    onChange(chosen.includes(key) ? chosen.filter((item) => item !== key) : [...chosen, key]);
  }

  function onSearchKey(event) {
    if (event.key === "ArrowDown") {
      event.preventDefault();
      setActive((index) => Math.min(index + 1, Math.max(shown.length - 1, 0)));
    } else if (event.key === "ArrowUp") {
      event.preventDefault();
      setActive((index) => Math.max(index - 1, 0));
    } else if (event.key === "Enter" && shown[active]) {
      event.preventDefault();
      toggle(shown[active].id);
    }
  }

  const summary = chosen.length === 0 ? (placeholder || copy.all) : chosen.length === 1 ? copy.one : copy.many.replace("{n}", String(chosen.length));
  const picked = players.filter((player) => chosen.includes(String(player.id)));

  return (
    <div className="player-combo vfc-pop" ref={box}>
      <Button icon={<ChevronDown size={16} />} aria-expanded={open} aria-haspopup="listbox" onClick={() => setOpen((value) => !value)}>{summary}</Button>
      {picked.length && picked.length <= 3 ? (
        <div className="player-combo-chips">
          {picked.map((player) => (
            <button key={player.id} type="button" className="player-combo-chip" title={player.fullName} onClick={() => toggle(player.id)}>
              {player.fullName} ×
            </button>
          ))}
        </div>
      ) : null}
      {open ? (
        <div className="vfc-popover player-combo-menu">
          <TextInput autoFocus aria-label={copy.all} placeholder={copy.all} value={query} onChange={(event) => setQuery(event.target.value)} onKeyDown={onSearchKey} />
          <div className="player-combo-actions">
            <Button onClick={() => onChange(players.map((player) => String(player.id)))}>{copy.selectAll}</Button>
            <Button onClick={() => onChange([])}>{copy.clear}</Button>
          </div>
          <div className="player-combo-list" role="listbox" aria-label={copy.all}>
            {shown.length ? shown.map((player, index) => {
              const on = chosen.includes(String(player.id));
              const line = meta(player);
              return (
                <button
                  key={player.id}
                  type="button"
                  role="option"
                  aria-selected={on}
                  className={`player-combo-option${on ? " is-on" : ""}${index === active ? " is-active" : ""}`}
                  onMouseEnter={() => setActive(index)}
                  onClick={() => toggle(player.id)}
                >
                  <Avatar name={player.fullName} src={player.photoUrl} size="sm" />
                  <span className="player-combo-copy">
                    <strong title={player.fullName}><Highlight text={player.fullName} query={query} /></strong>
                    {line ? <small>{line}</small> : null}
                  </span>
                </button>
              );
            }) : <p className="player-combo-empty">{copy.empty}</p>}
          </div>
        </div>
      ) : null}
    </div>
  );
}
