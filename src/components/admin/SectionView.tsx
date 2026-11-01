"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import { motion } from "framer-motion";
import { Pencil, Trash2 } from "lucide-react";
import "@/crud/crud.css";
import { Avatar, Button, Field, IconButton, Modal, PhoneInput, SelectInput, TextInput } from "@/ui/kit";
import { api, getData } from "@/services/api";
import { getMediaUrl } from "@/services/media";
import { readSession, TOKEN_KEY, USER_KEY, writeSession } from "@/components/login/session";
import { useAuth } from "@/components/providers/AuthProvider";
import { useLocale } from "@/components/providers/LocaleProvider";
import { useTheme } from "@/components/providers/ThemeProvider";
import { canSee, isExecutive, ROLE_LABEL } from "@/lib/admin/access";
import { adminCopy, type AdminLocale } from "@/lib/admin/copy";
import { toCsv, type Row } from "@/lib/admin/records";
import { useAdminToast } from "./toast";
import { phoneProblem } from "@/lib/phone";
import { PlayersOffice } from "./PlayersOffice";
import { CaptainsOffice } from "./CaptainsOffice";
import { LineupOffice } from "./LineupOffice";
import { MatchesOffice } from "./MatchesOffice";
import { CalendarOffice } from "./CalendarOffice";
import { AdministrationOffice, RolesOffice, StaffOffice } from "./peopleOffice";
import { FormerPlayersOffice } from "./FormerPlayersOffice";
import { ApprovalsOffice } from "./ApprovalsOffice";
import { AuditOffice } from "./AuditOffice";
import { AttendanceOffice } from "./AttendanceOffice";
import { FinesOffice } from "./FinesOffice";
import { FinanceOffice } from "./FinanceOffice";
import { BonusesPanel } from "./BonusesPanel";
import { SettingsOffice } from "./SettingsOffice";
import { EmailLog } from "./EmailLog";

type Field = { key: string; label: string; type?: "text" | "number" | "select"; options?: string[] };

function useCopy() {
  const { locale } = useLocale();
  return adminCopy[(locale as AdminLocale) in adminCopy ? (locale as AdminLocale) : "en"];
}

function statusTone(value: string) {
  const key = value.toLowerCase();
  if (["paid", "fit", "complete", "played", "present", "read"].includes(key)) return "ok";
  if (["unpaid", "overdue", "unavailable", "absent"].includes(key)) return "bad";
  if (["pending", "partial", "upcoming", "limited", "late", "new", "open", "live"].includes(key)) return "wait";
  return "";
}

function Board({ bucket, title, fields, seed }: { bucket: string; title: string; fields: Field[]; seed: Row[] }) {
  const copy = useCopy();
  const toast = useAdminToast();
  const [rows, setRows] = useState<Row[]>(seed);
  const [q, setQ] = useState("");
  const [sort, setSort] = useState(fields[0]?.key || "id");
  const [dir, setDir] = useState<1 | -1>(1);
  const [page, setPage] = useState(0);
  const [draft, setDraft] = useState<Row | null>(null);
  const [confirmId, setConfirmId] = useState<string | number | null>(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    let cancel = false;
    loadSection(bucket)
      .then((rows) => {
        if (!cancel) {
          setRows(rows);
          setReady(true);
        }
      })
      .catch(() => {
        if (!cancel) {
          setRows([]);
          setReady(true);
        }
      });
    return () => {
      cancel = true;
    };
  }, [bucket]);

  const save = (next: Row[]) => {
    setRows(next);
  };

  const filtered = useMemo(() => {
    const query = q.trim().toLowerCase();
    const list = rows.filter((row) => !query || Object.values(row).some((value) => String(value).toLowerCase().includes(query)));
    return [...list].sort((a, b) => String(a[sort] ?? "").localeCompare(String(b[sort] ?? ""), undefined, { numeric: true }) * dir);
  }, [rows, q, sort, dir]);

  const pageSize = 8;
  const slice = filtered.slice(page * pageSize, page * pageSize + pageSize);

  if (!ready) {
    return (
      <div className="admin-card">
        <p>{copy.loading}…</p>
        <div style={{ height: 12, marginTop: 12, background: "var(--surface-2)", borderRadius: 8 }} />
        <div style={{ height: 12, marginTop: 8, width: "70%", background: "var(--surface-2)", borderRadius: 8 }} />
      </div>
    );
  }

  return (
    <section>
      <div className="admin-toolbar">
        <h1 style={{ margin: 0, fontSize: 28 }}>{title}</h1>
        <input className="admin-search" style={{ maxWidth: 260, marginLeft: "auto" }} value={q} placeholder={copy.searchRecords} onChange={(event) => { setQ(event.target.value); setPage(0); }} />
        <button type="button" className="btn-outline" onClick={() => setDraft({ id: `new-${Date.now()}` })}>{copy.add}</button>
        <button
          type="button"
          className="btn-outline"
          onClick={() => {
            const blob = new Blob([toCsv(filtered)], { type: "text/csv" });
            const url = URL.createObjectURL(blob);
            const link = document.createElement("a");
            link.href = url;
            link.download = `${bucket}.csv`;
            link.click();
            URL.revokeObjectURL(url);
          }}
        >
          {copy.export}
        </button>
      </div>
      {slice.length === 0 ? <p className="admin-card">{copy.empty}</p> : (
        <div className="admin-table-wrap">
          <table className="admin-table">
            <thead>
              <tr>
                {fields.map((field) => (
                  <th
                    key={field.key}
                    className={sort === field.key ? `is-sorted${dir === -1 ? " is-desc" : ""}` : undefined}
                    onClick={() => {
                      setDir(sort === field.key && dir === 1 ? -1 : 1);
                      setSort(field.key);
                    }}
                  >
                    {field.label}
                  </th>
                ))}
                <th>{copy.actions}</th>
              </tr>
            </thead>
            <tbody>
              {slice.map((row, index) => (
                <motion.tr key={String(row.id)} initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: index * 0.03 }}>
                  {fields.map((field) => {
                    const value = String(row[field.key] ?? "");
                    const tone = field.key === "status" ? statusTone(value) : "";
                    return (
                      <td key={field.key}>
                        {tone ? <span className="admin-badge" data-tone={tone}>{value}</span> : value}
                      </td>
                    );
                  })}
                  <td>
                    <span style={{ display: "inline-flex", gap: 6 }}>
                      <IconButton label={copy.edit} onClick={() => setDraft(row)}><Pencil size={16} /></IconButton>
                      <IconButton label={copy.delete} onClick={() => setConfirmId(row.id)}><Trash2 size={16} /></IconButton>
                    </span>
                  </td>
                </motion.tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <div className="admin-toolbar">
        <button type="button" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>←</button>
        <span>{page + 1} / {Math.max(1, Math.ceil(filtered.length / pageSize))}</span>
        <button type="button" disabled={(page + 1) * pageSize >= filtered.length} onClick={() => setPage((p) => p + 1)}>→</button>
      </div>
      {draft ? (
        <Modal
          title={rows.some((row) => row.id === draft.id) ? copy.edit : copy.add}
          onClose={() => setDraft(null)}
          footer={(
            <>
              <span />
              <div className="vfc-foot-right">
                <Button onClick={() => setDraft(null)}>{copy.cancel}</Button>
                <Button
                  variant="primary"
                  onClick={() => {
                    const exists = rows.some((row) => row.id === draft.id);
                    save(exists ? rows.map((row) => (row.id === draft.id ? draft : row)) : [draft, ...rows]);
                    setDraft(null);
                    toast(copy.saved);
                  }}
                >
                  {copy.save}
                </Button>
              </div>
            </>
          )}
        >
          <div className="crud-grid">
            {fields.map((field) => (
              <label key={field.key} className="vfc-field">
                <span className="vfc-label">{field.label}</span>
                {field.type === "select" ? (
                  <SelectInput value={String(draft[field.key] ?? field.options?.[0] ?? "")} onChange={(event) => setDraft({ ...draft, [field.key]: event.target.value })}>
                    {field.options?.map((option) => <option key={option}>{option}</option>)}
                  </SelectInput>
                ) : (
                  <TextInput type={field.type === "number" ? "number" : "text"} value={String(draft[field.key] ?? "")} onChange={(event) => setDraft({ ...draft, [field.key]: field.type === "number" ? Number(event.target.value) : event.target.value })} />
                )}
              </label>
            ))}
          </div>
        </Modal>
      ) : null}
      {confirmId !== null ? (
        <>
          <button type="button" className="drawer-back" onClick={() => setConfirmId(null)} />
          <div className="drawer">
            <p>{copy.confirmDelete}</p>
            <button
              className="btn-primary"
              type="button"
              onClick={() => {
                save(rows.filter((row) => row.id !== confirmId));
                setConfirmId(null);
                toast(copy.delete);
              }}
            >
              {copy.delete}
            </button>
            <button type="button" onClick={() => setConfirmId(null)}>{copy.cancel}</button>
          </div>
        </>
      ) : null}
    </section>
  );
}

async function loadSection(section: string): Promise<Row[]> {
  const text = (value: unknown) => (value == null ? "" : String(value));
  if (section === "players") {
    const rows = await getData("/api/players");
    return rows.map((row) => ({ id: row.id, name: row.knownAs || row.fullName, number: row.jerseyNumber ?? "", position: text(row.position) }));
  }
  if (section === "staff") {
    const rows = await getData("/api/staff");
    return rows.map((row) => ({ id: row.id, name: row.fullName || row.name, role: text(row.role) }));
  }
  if (section === "matches") {
    const rows = await getData("/api/matches");
    return rows.map((row) => ({ id: row.id, opponent: text(row.opponent), date: text(row.kickoffDate || row.kickoff), status: text(row.status) }));
  }
  if (section === "partners" || section === "sponsors") {
    const rows = await getData("/api/partners");
    return rows.map((row) => ({ id: row.id, name: text(row.name), tier: text(row.category || row.status) }));
  }
  if (section === "table") {
    const rows = await getData("/api/standings");
    return (rows || []).map((row, index) => ({ id: row.id || index, team: text(row.team || row.name), pts: row.points ?? row.pts ?? 0, played: row.played ?? 0 }));
  }
  if (section === "users") {
    const rows = await getData("/api/users");
    return rows.map((row) => ({ id: row.id, name: text(row.fullName), email: text(row.email), role: text(row.role) }));
  }
  if (section === "fines") {
    const rows = await getData("/api/fines/ledger");
    const list = Array.isArray(rows) ? rows : rows.content || [];
    return list.map((row) => ({ id: row.id, name: text(row.player || row.playerName), reason: text(row.reason || row.fineType), amount: row.amount ?? 0, status: text(row.status) }));
  }
  if (section === "medical") {
    const body = await getData("/api/medical/dashboard");
    return (body.notCleared || []).map((row) => ({ id: row.id || row.playerId, name: text(row.name), injury: text(row.bodyPart || row.injuryType), status: text(row.status) }));
  }
  if (section === "transfers") {
    const rows = await getData("/api/transfers");
    return rows.map((row) => ({ id: row.id, name: text(row.player || row.name), direction: text(row.direction), status: text(row.status) }));
  }
  if (section === "audit") {
    const body = await getData("/api/audit", { page: 0, size: 20 });
    return (body.content || []).map((row) => ({ id: row.id, actor: text(row.actor), action: text(row.action), reason: text(row.after), when: text(row.at) }));
  }
  if (section === "finance") {
    const rows = await getData("/api/finance/unpaid");
    return rows.map((row) => ({ id: row.id, name: text(row.name), seasonal: row.amountDue ?? 0, paid: row.amountPaid ?? 0, status: text(row.status) }));
  }
  return [];
}

function seeds(section: string): { fields: Field[]; rows: Row[] } {
  const common: Record<string, Field[]> = {
    players: [{ key: "name", label: "Name" }, { key: "number", label: "Jersey" }, { key: "position", label: "Position" }],
    staff: [{ key: "name", label: "Name" }, { key: "role", label: "Role" }],
    matches: [{ key: "opponent", label: "Opponent" }, { key: "date", label: "Date" }, { key: "status", label: "Status" }],
    partners: [{ key: "name", label: "Name" }, { key: "tier", label: "Tier" }],
    sponsors: [{ key: "name", label: "Name" }, { key: "tier", label: "Tier" }],
    table: [{ key: "team", label: "Team" }, { key: "pts", label: "Pts" }, { key: "played", label: "P" }],
    users: [{ key: "name", label: "Name" }, { key: "email", label: "Email" }, { key: "role", label: "Role" }],
    fines: [{ key: "name", label: "Player" }, { key: "reason", label: "Reason" }, { key: "amount", label: "Amount" }, { key: "status", label: "Status" }],
    medical: [{ key: "name", label: "Player" }, { key: "injury", label: "Injury" }, { key: "status", label: "Status" }],
    transfers: [{ key: "name", label: "Name" }, { key: "direction", label: "Direction" }, { key: "status", label: "Status" }],
    finance: [{ key: "name", label: "Name" }, { key: "seasonal", label: "Due" }, { key: "paid", label: "Paid" }, { key: "status", label: "Status" }],
    audit: [{ key: "actor", label: "Actor" }, { key: "action", label: "Action" }, { key: "reason", label: "Reason" }, { key: "when", label: "When" }],
  };
  return { fields: common[section] || [{ key: "name", label: "Name" }], rows: [] };
}

function stillMotion() {
  return typeof window !== "undefined" && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

function keepToken(accessToken: string, email: string) {
  const session = readSession();
  const store = window.localStorage.getItem(TOKEN_KEY) ? window.localStorage : window.sessionStorage;
  store.setItem(TOKEN_KEY, accessToken);
  if (session?.user) store.setItem(USER_KEY, JSON.stringify({ ...session.user, email }));
}

function Profile() {
  const copy = useCopy();
  const toast = useAdminToast();
  const { user, setUser, setPhoto } = useAuth();
  const { locale, setLocale } = useLocale();
  const { theme, setTheme } = useTheme();
  const fileRef = useRef<HTMLInputElement>(null);
  const [email, setEmail] = useState(user?.email || "");
  const [phone, setPhone] = useState(user?.phone || "");
  const [zoom, setZoom] = useState(1);
  const [fileUrl, setFileUrl] = useState<string | null>(null);
  const [over, setOver] = useState(false);
  const [busy, setBusy] = useState(false);
  const [savingEmail, setSavingEmail] = useState(false);
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const phoneError = phoneProblem(phone, locale);
  if (!user) return null;

  const onFile = (file: File | undefined) => {
    if (!file) return;
    if (!["image/jpeg", "image/png", "image/webp"].includes(file.type) || file.size > 5 * 1024 * 1024) {
      toast(copy.photoHint);
      return;
    }
    setZoom(1);
    setFileUrl(URL.createObjectURL(file));
  };

  const saveProfile = async () => {
    const next = email.trim();
    if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(next)) {
      toast(copy.email);
      return;
    }
    if (phoneError) return;
    setSavingEmail(true);
    try {
      const response = await api.patch("/api/auth/me", { email: next, phone });
      const data = response.data?.data || {};
      if (data.accessToken) keepToken(data.accessToken, data.email || next);
      const me = await api.get("/api/auth/me");
      const saved = me.data?.data || {};
      setUser({ ...user, email: saved.email || data.email || next, phone: saved.phone || "" });
      setEmail(saved.email || data.email || next);
      setPhone(saved.phone || "");
      toast(copy.saved);
    } catch (err) {
      const message = (err as { response?: { data?: { message?: string } }; message?: string }).response?.data?.message
        || (err as { message?: string }).message
        || copy.email;
      toast(message);
    } finally {
      setSavingEmail(false);
    }
  };

  const saveCrop = async () => {
    if (!fileUrl) return;
    setBusy(true);
    const image = new Image();
    image.src = fileUrl;
    await image.decode();
    const size = 320;
    const canvas = document.createElement("canvas");
    canvas.width = size;
    canvas.height = size;
    const ctx = canvas.getContext("2d");
    if (!ctx) { setBusy(false); return; }
    ctx.clearRect(0, 0, size, size);
    ctx.save();
    ctx.beginPath();
    ctx.arc(size / 2, size / 2, size / 2, 0, Math.PI * 2);
    ctx.clip();
    const scale = Math.max(size / image.width, size / image.height) * zoom;
    const width = image.width * scale;
    const height = image.height * scale;
    ctx.drawImage(image, (size - width) / 2, (size - height) / 2, width, height);
    ctx.restore();
    const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, "image/jpeg", 0.9));
    if (!blob) {
      setBusy(false);
      return;
    }
    try {
      const body = new FormData();
      body.append("file", new File([blob], "photo.jpg", { type: "image/jpeg" }));
      const response = await api.post("/api/auth/me/photo", body);
      const url = response.data?.data?.photoUrl;
      setPhoto(url ? `${getMediaUrl(url)}?v=${Date.now()}` : null);
      setFileUrl(null);
      toast(copy.photoUpdated);
    } catch (err) {
      toast((err as { response?: { data?: { message?: string } }; message?: string }).response?.data?.message || (err as Error).message);
    } finally {
      setBusy(false);
    }
  };

  const removePhoto = async () => {
    setBusy(true);
    try {
      await api.delete("/api/auth/me/photo");
      setPhoto(null);
      setFileUrl(null);
      toast(copy.saved);
    } catch (err) {
      toast((err as { response?: { data?: { message?: string } }; message?: string }).response?.data?.message || (err as Error).message);
    } finally {
      setBusy(false);
    }
  };

  const savePassword = async () => {
    setBusy(true);
    try {
      const response = await api.post("/api/auth/change-password", { currentPassword, newPassword });
      const data = response.data?.data || {};
      const session = readSession();
      const remembered = Boolean(window.localStorage.getItem(TOKEN_KEY));
      writeSession({
        accessToken: data.accessToken,
        refreshToken: data.refreshToken,
        user: { ...(session?.user || {}), email: user.email, fullName: user.fullName, mustChangePassword: false },
      }, remembered);
      setCurrentPassword("");
      setNewPassword("");
      toast(copy.saved);
    } catch (err) {
      toast((err as { response?: { data?: { message?: string } }; message?: string }).response?.data?.message || (err as Error).message);
    } finally {
      setBusy(false);
    }
  };

  const rise = (index: number) => stillMotion() ? {} : {
    initial: { opacity: 0, y: 14 },
    animate: { opacity: 1, y: 0 },
    transition: { duration: 0.4, delay: index * 0.07, ease: [0.22, 1, 0.36, 1] },
  };

  return (
    <section className="profile-page">
      <motion.header className="profile-id" {...rise(0)}>
        <Avatar name={user.fullName} src={fileUrl || user.photo || undefined} size="xl" />
        <div className="profile-meta">
          <h1>{copy.profile}</h1>
          <strong>{user.fullName}</strong>
          <span>{ROLE_LABEL[user.role] || user.role}</span>
        </div>
      </motion.header>

      <motion.form className="admin-card profile-card" {...rise(1)} onSubmit={(event) => { event.preventDefault(); saveProfile(); }}>
        <p className="profile-note">{copy.emailOnly}</p>
        <Field label={copy.name}><TextInput value={user.fullName} readOnly /></Field>
        <Field label={copy.email} required><TextInput type="email" value={email} autoComplete="email" onChange={(event) => setEmail(event.target.value)} /></Field>
        <Field label={copy.phone} error={phoneError} hint={phoneError ? undefined : "+250 7XX XXX XXX"}><PhoneInput value={phone} aria-label={copy.phone} onChange={setPhone} /></Field>
        <Field label={copy.role}><TextInput value={ROLE_LABEL[user.role] || user.role} readOnly /></Field>
        <div className="profile-pair">
          <Field label={copy.language}>
            <SelectInput aria-label={copy.language} value={locale} onChange={(event) => setLocale(event.target.value as "en" | "fr" | "rw")}>
              <option value="en">EN</option>
              <option value="fr">FR</option>
              <option value="rw">RW</option>
            </SelectInput>
          </Field>
          <Field label={copy.theme}>
            <SelectInput aria-label={copy.theme} value={theme} onChange={(event) => setTheme(event.target.value as "light" | "dark")}>
              <option value="light">{copy.light}</option>
              <option value="dark">{copy.dark}</option>
            </SelectInput>
          </Field>
        </div>
        <Button variant="primary" type="submit" loading={savingEmail} {...(phoneError ? { disabled: true } : {})}>{copy.save}</Button>
      </motion.form>

      <motion.section id="photo" className="admin-card profile-card" {...rise(2)}>
        <h2>{copy.changePhoto}</h2>
        <p className="profile-note">{copy.photoHint}</p>
        <div className="photo-choices">
          <div
            className={`photo-drop${over ? " is-over" : ""}`}
            onDragEnter={(event) => { event.preventDefault(); setOver(true); }}
            onDragOver={(event) => event.preventDefault()}
            onDragLeave={() => setOver(false)}
            onDrop={(event) => {
              event.preventDefault();
              setOver(false);
              onFile(event.dataTransfer.files?.[0]);
            }}
          >
            {copy.drop}
          </div>
          <Button onClick={() => fileRef.current?.click()}>{copy.chooseFile}</Button>
          {user.photo ? <Button loading={busy} onClick={removePhoto}>{copy.removePhoto}</Button> : null}
          <input ref={fileRef} className="photo-file" type="file" accept="image/jpeg,image/png,image/webp" onChange={(event) => { onFile(event.target.files?.[0]); event.target.value = ""; }} />
        </div>
        {fileUrl ? (
          <motion.div className="photo-preview" initial={stillMotion() ? false : { opacity: 0, scale: 0.92 }} animate={{ opacity: 1, scale: 1 }}>
            <img src={fileUrl} alt="" style={{ transform: `scale(${zoom})` }} />
            <Field label={copy.zoom}><TextInput type="range" min={1} max={2.4} step={0.05} value={zoom} onChange={(event) => setZoom(Number(event.target.value))} /></Field>
            <Button variant="primary" loading={busy} onClick={saveCrop}>{copy.save}</Button>
          </motion.div>
        ) : null}
      </motion.section>

      <motion.form id="password" className="admin-card profile-card" {...rise(3)} onSubmit={(event) => { event.preventDefault(); savePassword(); }}>
        <h2>{copy.changePassword}</h2>
        <Field label={copy.currentPassword} required><TextInput type="password" value={currentPassword} autoComplete="current-password" onChange={(event) => setCurrentPassword(event.target.value)} /></Field>
        <Field label={copy.newPassword} required><TextInput type="password" minLength={8} value={newPassword} autoComplete="new-password" onChange={(event) => setNewPassword(event.target.value)} /></Field>
        <Button variant="primary" type="submit" loading={busy} {...(!currentPassword || newPassword.length < 8 ? { disabled: true } : {})}>{copy.save}</Button>
      </motion.form>
    </section>
  );
}

export function SectionView({ section }: { section: string }) {
  const copy = useCopy();
  const { locale } = useLocale();
  const { user } = useAuth();
  if (!user || !canSee(user.role, section)) {
    return (
      <section className="admin-card">
        <h1>{copy.notAllowed}</h1>
        <Link href="/admin">{copy.backDashboard}</Link>
      </section>
    );
  }
  if (section === "players") return <PlayersOffice />;
  if (section === "former-players") return <FormerPlayersOffice />;
  if (section === "approvals") return <ApprovalsOffice />;
  if (section === "staff") return <StaffOffice />;
  if (section === "users" || section === "administration") return <AdministrationOffice />;
  if (section === "roles") return <RolesOffice />;
  if (section === "captains") return <CaptainsOffice />;
  if (section === "emails") return <EmailLog />;
  if (section === "audit") return <AuditOffice />;
  if (section === "profile") return <Profile />;
  if (section === "settings") return <SettingsOffice locale={locale} />;
  if (section === "attendance") return <AttendanceOffice locale={locale} />;
  if (section === "fines") return <FinesOffice locale={locale} />;
  if (section === "lineup") return <LineupOffice />;
  if (section === "matches") return <MatchesOffice />;
  if (section === "calendar") return <CalendarOffice />;
  if (section === "finance") {
    if (!isExecutive(user.role)) {
      return (
        <section className="admin-card">
          <h1>{copy.notAllowed}</h1>
        </section>
      );
    }
    return <FinanceOffice locale={locale} />;
  }
  if (section === "bonuses") {
    if (!isExecutive(user.role)) {
      return (
        <section className="admin-card">
          <h1>{copy.notAllowed}</h1>
        </section>
      );
    }
    return <BonusesPanel locale={locale} />;
  }
  const data = seeds(section);
  const title = (copy as Record<string, string>)[section] || section;
  return <Board bucket={section} title={title} fields={data.fields} seed={data.rows} />;
}
