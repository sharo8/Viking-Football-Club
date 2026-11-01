"use client";

import { useEffect, useMemo, useState } from "react";
import { AlertTriangle } from "lucide-react";
import { api, getData } from "@/services/api";
import { useAuth } from "@/components/providers/AuthProvider";
import { Avatar, Button, Field, Modal, SelectInput, TextArea, TextInput } from "@/ui/kit";
import { ConfirmDialog } from "@/crud/kit";

const REASONS = ["RESIGNED", "CONTRACT_ENDED", "MISCONDUCT", "DUPLICATE", "MISTAKE", "OTHER"];

function fill(template, values) {
  return Object.entries(values).reduce((text, [key, value]) => text.replaceAll(`{${key}}`, String(value)), template || "");
}

export function reasonReady(code, text) {
  if (!REASONS.includes(code)) return false;
  const note = (text || "").trim();
  if (note.length > 500) return false;
  if (code === "OTHER") return note.length >= 10;
  return note.length === 0 || note.length >= 10;
}

export function ReasonFields({ t, code, text, onCode, onText, name, typed, onTyped, requireName }) {
  const labels = {
    RESIGNED: t.reasonResigned,
    CONTRACT_ENDED: t.reasonContract,
    MISCONDUCT: t.reasonMisconduct,
    DUPLICATE: t.reasonDuplicate,
    MISTAKE: t.reasonMistake,
    OTHER: t.reasonOther,
  };
  return (
    <>
      <Field label={t.reason} required>
        <SelectInput value={code} onChange={(event) => onCode(event.target.value)}>
          <option value="">{t.chooseReason}</option>
          {REASONS.map((item) => <option key={item} value={item}>{labels[item]}</option>)}
        </SelectInput>
      </Field>
      <Field label={t.reasonText} required={code === "OTHER"}>
        <TextArea value={text} maxLength={500} onChange={(event) => onText(event.target.value)} />
      </Field>
      {requireName ? (
        <Field label={fill(t.typeName, { name })} required>
          <TextInput value={typed} onChange={(event) => onTyped(event.target.value)} />
        </Field>
      ) : null}
    </>
  );
}

export function PersonDeleteDialog({ t, person, onClose, onDone }) {
  const [code, setCode] = useState("");
  const [text, setText] = useState("");
  const [typed, setTyped] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const ready = reasonReady(code, text) && typed.trim().toLowerCase() === String(person.name || "").trim().toLowerCase();
  const submit = async () => {
    setError("");
    setBusy(true);
    try {
      const path = person.kind === "staff" ? `/api/staff/${person.id}/remove` : `/api/users/${person.id}/remove`;
      await api.post(path, { reasonCode: code, reason: text, confirmName: typed });
      onDone();
    } catch (err) {
      setError(err?.code === "SELF" ? t.selfBlocked : err?.code === "LAST_EXECUTIVE" ? t.lastExecutive : err?.code === "REASON_REQUIRED" ? t.reason : t.saveFailed);
    } finally {
      setBusy(false);
    }
  };
  return (
    <ConfirmDialog
      title={fill(t.deletePersonTitle, { name: person.name })}
      body={fill(t.typeName, { name: person.name })}
      confirmLabel={t.delete}
      cancelLabel={t.cancel}
      confirmDisabled={!ready || busy}
      error={error}
      onClose={onClose}
      onConfirm={submit}
    >
      <ReasonFields t={t} code={code} text={text} onCode={setCode} onText={setText} name={person.name} typed={typed} onTyped={setTyped} requireName />
    </ConfirmDialog>
  );
}

export function RoleDeleteDialog({ t, role, roles, roleName, onClose, onDone }) {
  const { user } = useAuth();
  const [usage, setUsage] = useState(null);
  const [error, setError] = useState("");
  const [decisions, setDecisions] = useState({});
  const [selected, setSelected] = useState([]);
  const [bulkRole, setBulkRole] = useState("");
  const [summary, setSummary] = useState(false);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    getData(`/api/roles/${role.id}/usage`).then(setUsage).catch((err) => setError(err?.message || t.loadFailed));
  }, [role.id, t.loadFailed]);

  const people = usage?.people || [];
  const targets = useMemo(() => (roles || []).filter((item) => item.id !== role.id && item.active !== false && item.category === role.category), [roles, role]);

  const taken = (targetId, exceptUser) => Object.entries(decisions).filter(([userId, row]) => row.action === "move" && String(row.targetRoleId) === String(targetId) && String(userId) !== String(exceptUser)).length;

  const optionTitle = (item, exceptUser) => {
    if (item.category !== role.category) return t.wrongCategory;
    if (item.active === false) return t.roleProtected;
    if (item.maxSeats != null && Number(item.users || 0) + taken(item.id, exceptUser) >= item.maxSeats) return t.seatsFull;
    const left = item.maxSeats == null ? t.noSeatLimit : fill(t.seatsLeft, { n: item.maxSeats - Number(item.users || 0) - taken(item.id, exceptUser) });
    return left;
  };

  const setDecision = (userId, next) => setDecisions((rows) => ({ ...rows, [userId]: { ...(rows[userId] || {}), ...next } }));
  const handled = people.filter((person) => {
    const row = decisions[person.userId];
    if (!row) return false;
    if (person.email && user?.email && person.email.toLowerCase() === user.email.toLowerCase()) return false;
    if (row.action === "move") return !!row.targetRoleId;
    if (row.action === "delete") return reasonReady(row.reasonCode, row.reason);
    return false;
  }).length;

  const applyBulk = (userIds, targetRoleId) => {
    setDecisions((rows) => {
      const next = { ...rows };
      userIds.forEach((id) => { next[id] = { action: "move", targetRoleId }; });
      return next;
    });
  };

  const submit = async () => {
    setBusy(true);
    setError("");
    try {
      const moves = [];
      const deletions = [];
      people.forEach((person) => {
        const row = decisions[person.userId];
        if (row.action === "move") moves.push({ userId: person.userId, targetRoleId: Number(row.targetRoleId) });
        else deletions.push({ userId: person.userId, reasonCode: row.reasonCode, reason: row.reason || "", confirmName: row.confirmName || "" });
      });
      await api.post(`/api/roles/${role.id}/resolve-and-delete`, { moves, deletions });
      onDone();
    } catch (err) {
      const code = err?.code;
      setError(code === "SELF" ? t.selfBlocked : code === "LAST_EXECUTIVE" ? t.lastExecutive : code === "ROLE_CATEGORY_MISMATCH" ? t.wrongCategory : code === "ROLE_SEATS_FULL" ? t.seatsFull : code === "ROLE_LOCKED" ? t.roleProtected : code === "REASON_REQUIRED" ? t.reason : t.saveFailed);
      setSummary(false);
    } finally {
      setBusy(false);
    }
  };

  if (!usage) {
    return <Modal title={fill(t.deleteRoleTitle, { name: roleName })} onClose={onClose}>{error ? <p className="crud-error">{error}</p> : null}</Modal>;
  }
  if (!people.length) {
    return (
      <ConfirmDialog
        title={fill(t.deleteRoleTitle, { name: roleName })}
        body={t.noOneHasRole}
        confirmLabel={t.delete}
        cancelLabel={t.cancel}
        error={error}
        onClose={onClose}
        onConfirm={async () => {
          try {
            await api.delete(`/api/roles/${role.id}`);
            onDone();
          } catch (err) {
            setError(err?.code === "ROLE_LOCKED" ? t.roleProtected : t.saveFailed);
          }
        }}
      />
    );
  }

  const moved = people.filter((person) => decisions[person.userId]?.action === "move").length;
  const deleted = people.filter((person) => decisions[person.userId]?.action === "delete").length;
  return (
    <Modal
      title={fill(t.roleInUseTitle, { n: people.length, name: roleName })}
      size="lg"
      onClose={onClose}
      leading={<AlertTriangle size={28} />}
      footer={(
        <div className="vfc-foot-right pair-actions">
          <Button onClick={onClose}>{t.cancel}</Button>
          <Button variant="danger" disabled={handled !== people.length || busy} onClick={() => (summary ? submit() : setSummary(true))}>{t.deleteRoleFinal}</Button>
        </div>
      )}
    >
      {summary ? (
        <div>
          <p>{t.confirmStep}</p>
          <p>{fill(t.summaryMoved, { n: moved })}</p>
          <p>{fill(t.summaryDeleted, { n: deleted })}</p>
        </div>
      ) : (
        <>
          <div className="role-shortcuts">
            <span>{t.moveAllTo}</span>
            <SelectInput value={bulkRole} onChange={(event) => setBulkRole(event.target.value)}>
              <option value="">{t.chooseRole}</option>
              {targets.map((item) => <option key={item.id} value={item.id} disabled={optionTitle(item) === t.seatsFull || optionTitle(item) === t.wrongCategory} title={optionTitle(item)}>{roleNameOf(item, roleName, item)} · {optionTitle(item)}</option>)}
            </SelectInput>
            <Button disabled={!bulkRole} onClick={() => applyBulk(people.map((person) => person.userId), bulkRole)}>{t.moveAllTo}</Button>
            <Button disabled={!selected.length || !bulkRole} onClick={() => applyBulk(selected, bulkRole)}>{t.moveSelected}</Button>
          </div>
          <p className="role-progress">{fill(t.handled, { done: handled, total: people.length })}</p>
          <div className={`role-people${people.length > 6 ? " is-scroll" : ""}`}>
            {people.map((person) => {
              const mine = user?.email && person.email && person.email.toLowerCase() === user.email.toLowerCase();
              const row = decisions[person.userId] || {};
              return (
                <div key={person.userId} className="role-person">
                  <input type="checkbox" aria-label={person.name} checked={selected.includes(person.userId)} onChange={(event) => setSelected((ids) => event.target.checked ? [...ids, person.userId] : ids.filter((id) => id !== person.userId))} />
                  <Avatar name={person.name} src={person.photoUrl} />
                  <div>
                    <strong>{person.name}</strong>
                    <div>{person.email}</div>
                    <div>{person.status === "ACTIVE" ? t.active : t.suspended} · {person.type === "admin" ? t.kindAdmin : person.type === "player" ? t.kindPlayer : t.kindStaff}</div>
                    {mine ? <div>{t.selfBlocked}</div> : null}
                  </div>
                  <div>
                    <SelectInput disabled={mine} value={row.action || ""} onChange={(event) => setDecision(person.userId, { action: event.target.value })}>
                      <option value="">{t.chooseRole}</option>
                      <option value="move">{t.moveToRole}</option>
                      <option value="delete">{t.deleteFromSystem}</option>
                    </SelectInput>
                    {row.action === "move" ? (
                      <SelectInput value={row.targetRoleId || ""} onChange={(event) => setDecision(person.userId, { targetRoleId: event.target.value })}>
                        <option value="">{t.chooseRole}</option>
                        {targets.map((item) => {
                          const title = optionTitle(item, person.userId);
                          const blocked = title === t.seatsFull || title === t.wrongCategory;
                          return <option key={item.id} value={item.id} disabled={blocked} title={title}>{item.name || item.nameEn} · {title}</option>;
                        })}
                      </SelectInput>
                    ) : null}
                    {row.action === "delete" ? (
                      <ReasonFields t={t} code={row.reasonCode || ""} text={row.reason || ""} onCode={(value) => setDecision(person.userId, { reasonCode: value })} onText={(value) => setDecision(person.userId, { reason: value })} />
                    ) : null}
                  </div>
                </div>
              );
            })}
          </div>
        </>
      )}
      {error ? <p className="crud-error">{error}</p> : null}
    </Modal>
  );
}

function roleNameOf(item) {
  return item.name || item.nameEn || item.code;
}
