"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { motion } from "framer-motion";
import { getData } from "@/services/api";
import { useAuth } from "@/components/providers/AuthProvider";
import { useLocale } from "@/components/providers/LocaleProvider";
import { useTheme } from "@/components/providers/ThemeProvider";
import { canSee, isExecutive, ROLE_LABEL } from "@/lib/admin/access";
import { adminCopy, type AdminLocale } from "@/lib/admin/copy";
import { toCsv, type Row } from "@/lib/admin/records";
import { useAdminToast } from "./toast";
import { PlayersOffice } from "./PlayersOffice";
import { CaptainsOffice } from "./CaptainsOffice";
import { LineupOffice } from "./LineupOffice";
import { EmailHealth } from "./EmailHealth";
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
                    <button type="button" onClick={() => setDraft(row)}>{copy.edit}</button>
                    {" · "}
                    <button type="button" onClick={() => setConfirmId(row.id)}>{copy.delete}</button>
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
        <>
          <button type="button" className="drawer-back" aria-label={copy.close} onClick={() => setDraft(null)} />
          <form
            className="drawer"
            onSubmit={(event) => {
              event.preventDefault();
              const form = new FormData(event.currentTarget);
              const nextRow: Row = { ...draft };
              for (const field of fields) {
                const value = String(form.get(field.key) ?? "");
                nextRow[field.key] = field.type === "number" ? Number(value) : value;
              }
              const exists = rows.some((row) => row.id === nextRow.id);
              save(exists ? rows.map((row) => (row.id === nextRow.id ? nextRow : row)) : [nextRow, ...rows]);
              setDraft(null);
              toast(copy.saved);
            }}
          >
            <h2>{rows.some((row) => row.id === draft.id) ? copy.edit : copy.add}</h2>
            {fields.map((field) => (
              <label key={field.key} className="admin-field">
                {field.label}
                {field.type === "select" ? (
                  <select name={field.key} defaultValue={String(draft[field.key] ?? field.options?.[0] ?? "")}>
                    {field.options?.map((option) => <option key={option}>{option}</option>)}
                  </select>
                ) : (
                  <input name={field.key} type={field.type === "number" ? "number" : "text"} defaultValue={String(draft[field.key] ?? "")} required />
                )}
              </label>
            ))}
            <button className="btn-primary" type="submit">{copy.save}</button>
            <button type="button" onClick={() => setDraft(null)}>{copy.cancel}</button>
          </form>
        </>
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
    return (body.content || []).map((row) => ({ id: row.id, actor: text(row.actor), action: text(row.action), when: text(row.at) }));
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
    audit: [{ key: "actor", label: "Actor" }, { key: "action", label: "Action" }, { key: "when", label: "When" }],
  };
  return { fields: common[section] || [{ key: "name", label: "Name" }], rows: [] };
}

function Attendance() {
  const copy = useCopy();
  const [matrix, setMatrix] = useState<{ sessions: string[]; players: { playerId: number; name: string; cells: Record<string, string> }[] } | null>(null);
  const [error, setError] = useState("");
  useEffect(() => {
    getData("/api/attendance/matrix").then(setMatrix).catch((err) => setError(err.message));
  }, []);
  const sessions = matrix?.sessions || [];
  const people = matrix?.players || [];
  return (
    <section>
      <h1>{copy.attendance}</h1>
      {error ? <button type="button" className="dash-retry" onClick={() => getData("/api/attendance/matrix").then(setMatrix).catch((err) => setError(err.message))}>{copy.retry || "Retry"}: {error}</button> : null}
      {!error && matrix && !people.length ? <p>{copy.empty || "No attendance has been recorded yet."}</p> : null}
      {people.length ? (
        <div className="admin-table-wrap">
          <table className="admin-table">
            <thead><tr><th>Player</th>{sessions.map((day) => <th key={day}>{day}</th>)}</tr></thead>
            <tbody>
              {people.map((player) => (
                <tr key={player.playerId}><td>{player.name}</td>{sessions.map((day) => <td key={day}>{player.cells?.[day] || "—"}</td>)}</tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
    </section>
  );
}


function Profile() {
  const copy = useCopy();
  const toast = useAdminToast();
  const { user, setUser, setPhoto } = useAuth();
  const { locale, setLocale } = useLocale();
  const { theme, setTheme } = useTheme();
  const [zoom, setZoom] = useState(1);
  const [fileUrl, setFileUrl] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  if (!user) return null;

  const onFile = (file: File | undefined) => {
    if (!file) return;
    if (!["image/jpeg", "image/png", "image/webp"].includes(file.type) || file.size > 5 * 1024 * 1024) {
      toast(copy.photoHint);
      return;
    }
    setFileUrl(URL.createObjectURL(file));
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
    if (!ctx) return;
    ctx.clearRect(0, 0, size, size);
    ctx.save();
    ctx.beginPath();
    ctx.arc(size / 2, size / 2, size / 2, 0, Math.PI * 2);
    ctx.clip();
    const scale = Math.max(size / image.width, size / image.height) * zoom;
    const w = image.width * scale;
    const h = image.height * scale;
    ctx.drawImage(image, (size - w) / 2, (size - h) / 2, w, h);
    ctx.restore();
    const data = canvas.toDataURL("image/jpeg", 0.9);
    setPhoto(data);
    setBusy(false);
    setFileUrl(null);
    toast(copy.photoUpdated);
  };

  return (
    <section style={{ maxWidth: 640 }}>
      <h1>{copy.profile}</h1>
      <form
        className="admin-card"
        onSubmit={(event) => {
          event.preventDefault();
          const form = new FormData(event.currentTarget);
          setUser({
            ...user,
            fullName: String(form.get("name") || user.fullName),
            phone: String(form.get("phone") || ""),
          });
          toast(copy.saved);
        }}
      >
        <label className="admin-field">{copy.name}<input name="name" defaultValue={user.fullName} required /></label>
        <label className="admin-field">{copy.email}<input value={user.email} readOnly /></label>
        <label className="admin-field">{copy.phone}<input name="phone" defaultValue={user.phone} /></label>
        <label className="admin-field">{copy.role}<input value={ROLE_LABEL[user.role] || user.role} readOnly /></label>
        <label className="admin-field">
          {copy.language}
          <select value={locale} onChange={(event) => setLocale(event.target.value as "en" | "fr" | "rw")}>
            <option value="en">EN</option>
            <option value="fr">FR</option>
            <option value="rw">RW</option>
          </select>
        </label>
        <label className="admin-field">
          {copy.theme}
          <select value={theme} onChange={(event) => setTheme(event.target.value as "light" | "dark")}>
            <option value="light">{copy.light}</option>
            <option value="dark">{copy.dark}</option>
          </select>
        </label>
        <button className="btn-primary" type="submit">{copy.save}</button>
      </form>

      <form id="photo" className="admin-card" style={{ marginTop: 16 }} onSubmit={(event) => event.preventDefault()}>
        <h2>{copy.changePhoto}</h2>
        <p style={{ color: "var(--text-muted)" }}>{copy.photoHint}</p>
        <div
          onDragOver={(event) => event.preventDefault()}
          onDrop={(event) => {
            event.preventDefault();
            onFile(event.dataTransfer.files?.[0]);
          }}
          style={{ padding: 18, border: "1px dashed var(--border)", borderRadius: 12, textAlign: "center" }}
        >
          {copy.drop}
          <input type="file" accept="image/jpeg,image/png,image/webp" onChange={(event) => onFile(event.target.files?.[0])} />
        </div>
        {fileUrl ? (
          <>
            <img src={fileUrl} alt="" style={{ width: 160, height: 160, objectFit: "cover", borderRadius: "50%", marginTop: 12, transform: `scale(${zoom})` }} />
            <label className="admin-field">{copy.zoom}<input type="range" min={1} max={2.4} step={0.05} value={zoom} onChange={(event) => setZoom(Number(event.target.value))} /></label>
            <button type="button" className="btn-primary" disabled={busy} onClick={saveCrop}>{busy ? copy.loading : copy.save}</button>
          </>
        ) : null}
        {user.photo ? <img src={user.photo} alt="" style={{ width: 72, height: 72, borderRadius: "50%", marginTop: 12 }} /> : null}
      </form>

      <form
        id="password"
        className="admin-card"
        style={{ marginTop: 16 }}
        onSubmit={(event) => {
          event.preventDefault();
          toast(copy.saved);
          event.currentTarget.reset();
        }}
      >
        <h2>{copy.changePassword}</h2>
        <label className="admin-field">{copy.currentPassword}<input type="password" required autoComplete="current-password" /></label>
        <label className="admin-field">{copy.newPassword}<input type="password" required minLength={8} autoComplete="new-password" /></label>
        <button className="btn-primary" type="submit">{copy.save}</button>
      </form>
    </section>
  );
}

function Settings() {
  const copy = useCopy();
  const { user } = useAuth();
  if (!user) return null;
  return (
    <section style={{ maxWidth: 560 }}>
      <h1>{copy.settings}</h1>
      <label className="admin-field">
        {copy.role}
        <input value={ROLE_LABEL[user.role] || user.role} readOnly />
      </label>
      <p style={{ color: "var(--text-muted)" }}>{copy.clubOffice}</p>
      {isExecutive(user.role) ? <EmailHealth /> : null}
    </section>
  );
}

export function SectionView({ section }: { section: string }) {
  const copy = useCopy();
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
  if (section === "captains") return <CaptainsOffice />;
  if (section === "emails") return <EmailLog />;
  if (section === "profile") return <Profile />;
  if (section === "settings") return <Settings />;
  if (section === "attendance") return <Attendance />;
  if (section === "lineup") return <LineupOffice />;
  if (section === "finance" && !isExecutive(user.role)) {
    return (
      <section className="admin-card">
        <h1>{copy.notAllowed}</h1>
      </section>
    );
  }
  const data = seeds(section);
  const title = (copy as Record<string, string>)[section] || section;
  return <Board bucket={section} title={title} fields={data.fields} seed={data.rows} />;
}
