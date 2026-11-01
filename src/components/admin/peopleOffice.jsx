"use client";

import { useEffect, useMemo, useState } from "react";
import { Eye, MoreHorizontal, Pencil, Trash2 } from "lucide-react";
import { api, getData } from "@/services/api";
import { useLocale } from "@/components/providers/LocaleProvider";
import { Avatar, Badge, Button, IconButton, SelectInput, Tabs } from "@/ui/kit";
import en from "@/locales/office/en.json";
import fr from "@/locales/office/fr.json";
import rw from "@/locales/office/rw.json";
import { DataTable, FormModal, Kpis, ViewModal } from "@/crud/kit";
import { TranslatedName, localizedName } from "./TranslatedName";
import { PersonDeleteDialog, RoleDeleteDialog } from "./RemovalDialogs";
import { AccountPanel, StaffForm } from "./StaffForm";
import { useAdminToast } from "./toast";
import { phoneProblem } from "@/lib/phone";

const COPY = { en, fr, rw };
const ADMIN_ROLES = ["PRESIDENT", "GENERAL_MANAGER", "FINANCE_MANAGER", "ASSISTANT_MANAGER", "SOCIAL_MEDIA_MANAGER", "MARKETING_MANAGER"];
const STAFF_ROLES = ["Head Coach", "Assistant Coach", "Goalkeeper Coach", "Fitness Coach", "Doctor", "Physiotherapist", "Kit Manager", "Team Manager"];
const STAFF_KEY = {
  "Head Coach": "headCoach",
  "Assistant Coach": "assistantCoach",
  "Goalkeeper Coach": "goalkeeperCoach",
  "Fitness Coach": "fitnessCoach",
  Doctor: "doctor",
  Physiotherapist: "physiotherapist",
  "Kit Manager": "kitManager",
  "Team Manager": "teamManager",
};

function useCopy() {
  const { locale } = useLocale();
  return { locale, t: COPY[locale] || en };
}

const SYSTEM_ROLE = {
  PRESIDENT: "president",
  GENERAL_MANAGER: "generalManager",
  FINANCE_MANAGER: "financeManager",
  ASSISTANT_MANAGER: "assistantManager",
  SOCIAL_MEDIA_MANAGER: "socialMediaManager",
  MARKETING_MANAGER: "marketingManager",
  HEAD_COACH: "headCoach",
  ASSISTANT_COACH: "assistantCoach",
  GOALKEEPER_COACH: "goalkeeperCoach",
  FITNESS_COACH: "fitnessCoach",
  DOCTOR: "doctor",
  PHYSIOTHERAPIST: "physiotherapist",
  KIT_MANAGER: "kitManager",
  TEAM_MANAGER: "teamManager",
  PLAYER: "player",
};

function roleName(row, locale, t) {
  if (!row) return "";
  if (row.systemRole && t?.[SYSTEM_ROLE[row.code]]) return t[SYSTEM_ROLE[row.code]];
  if (row.label && row.label[locale]) return row.label[locale];
  return localizedName(row, locale) || row.name || row.nameEn || "";
}

function messageOf(err, t) {
  if (err?.code === "ROLE_SEATS_FULL") return t.seatsFull;
  if (err?.code === "ROLE_CATEGORY_MISMATCH") return t.wrongPage;
  if (err?.code === "ROLE_LOCKED") return t.roleProtected;
  if (err?.code === "ROLE_IN_USE") return t.roleInUse;
  if (err?.code === "EMAIL_TAKEN") return t.emailTaken;
  if (err?.code === "EMAIL_INVALID") return t.emailInvalid;
  if (err?.code === "ACCOUNT_EXISTS") return t.accountExists;
  if (err?.code === "SELF") return t.selfBlocked;
  if (err?.code === "REASON_REQUIRED") return t.reason;
  return t.saveFailed;
}

function fill(template, values) {
  return Object.entries(values).reduce((text, [key, value]) => text.replace(`{${key}}`, value), template);
}

async function circleFile(file) {
  if (!file) return null;
  if (file.size > 5 * 1024 * 1024) throw new Error("size");
  const bitmap = await createImageBitmap(file);
  const size = Math.min(bitmap.width, bitmap.height);
  const canvas = document.createElement("canvas");
  canvas.width = 512;
  canvas.height = 512;
  const ctx = canvas.getContext("2d");
  ctx.beginPath();
  ctx.arc(256, 256, 256, 0, Math.PI * 2);
  ctx.closePath();
  ctx.clip();
  ctx.drawImage(bitmap, (bitmap.width - size) / 2, (bitmap.height - size) / 2, size, size, 0, 0, 512, 512);
  const blob = await new Promise((resolve) => canvas.toBlob(resolve, "image/png"));
  return new File([blob], "photo.png", { type: "image/png" });
}

async function sendPhoto(path, file) {
  const cropped = await circleFile(file);
  if (!cropped) return null;
  const body = new FormData();
  body.append("file", cropped);
  return api.post(path, body);
}

async function downloadFile(path, format, locale, query) {
  if (format === "csv") return "csv";
  const response = await api.get(`${path}/export`, { params: { format, lang: locale, q: query || undefined }, responseType: "blob" });
  const url = URL.createObjectURL(response.data);
  const link = document.createElement("a");
  link.href = url;
  link.download = `${path.split("/").pop()}.${format === "pdf" ? "pdf" : "xlsx"}`;
  link.click();
  URL.revokeObjectURL(url);
  return "file";
}

function csv(name, lines) {
  const blob = new Blob([lines.join("\n")], { type: "text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = name;
  link.click();
  URL.revokeObjectURL(url);
}

function AccountBadge({ t, status }) {
  const item = status === "INVITED"
    ? { label: t.invited, tone: "warn", tip: t.tipInvited }
    : status === "ACTIVE"
      ? { label: t.active, tone: "good", tip: t.tipActive }
      : status === "SUSPENDED"
        ? { label: t.suspended, tone: "bad", tip: t.tipSuspended }
        : { label: t.noAccount, tone: "neutral", tip: t.tipNone };
  return <span title={item.tip}><Badge tone={item.tone}>{item.label}</Badge></span>;
}

function Person({ name, src }) {
  return <span className="crud-person"><Avatar name={name} src={src} /><span>{name}</span></span>;
}

function StaffMenu({ t, row, language, onDone, onError }) {
  const [open, setOpen] = useState(false);
  if (!open) return <IconButton label={t.moreActions} onClick={() => setOpen(true)}><MoreHorizontal size={16} /></IconButton>;
  return (
    <div className="account-menu">
      <IconButton label={t.close} onClick={() => setOpen(false)}><MoreHorizontal size={16} /></IconButton>
      <div className="account-pop">
        <AccountPanel t={t} row={row} language={language} onDone={() => { setOpen(false); onDone(); }} onError={onError} />
      </div>
    </div>
  );
}

function Actions({ t, onView, onEdit, onDelete, editDisabled, deleteDisabled, deleteTitle }) {
  return (
    <>
      <IconButton label={t.view} onClick={onView}><Eye size={16} /></IconButton>
      <IconButton label={t.edit} title={editDisabled ? t.roleProtected : t.edit} disabled={editDisabled} onClick={onEdit}><Pencil size={16} /></IconButton>
      <IconButton label={t.delete} title={deleteTitle || t.delete} disabled={deleteDisabled} onClick={onDelete}><Trash2 size={16} /></IconButton>
    </>
  );
}

function Page({ title, subtitle, kpis, query, setQuery, onAdd, onExport, children, error, onRetry, t, filters }) {
  return (
    <section className="crud-page">
      <header className="crud-head">
        <div>
          <h1>{title} <Badge tone="neutral">{kpis[0]?.value ?? 0}</Badge></h1>
          <p>{subtitle}</p>
        </div>
        <div className="crud-tools">
          <input className="vfc-control" value={query} onChange={(event) => setQuery(event.target.value)} placeholder={t.search} aria-label={t.search} />
          <select className="vfc-control" aria-label={t.export} defaultValue="" onChange={(event) => { if (event.target.value) { onExport(event.target.value); event.target.value = ""; } }}>
            <option value="">{t.export}</option>
            <option value="xlsx">{t.excel}</option>
            <option value="pdf">{t.pdf}</option>
            <option value="csv">{t.csv}</option>
          </select>
          <Button variant="primary" onClick={onAdd}>{t.add}</Button>
        </div>
      </header>
      {filters ? <div className="crud-filters">{filters}</div> : null}
      <Kpis items={kpis} />
      {error ? <p className="crud-error">{error} <button type="button" onClick={onRetry}>{t.retry}</button></p> : null}
      {children}
    </section>
  );
}

const viewLabels = (t) => ({ empty: t.notProvided, delete: t.delete, close: t.close, edit: t.edit });
const formLabels = (t) => ({ cancelLabel: t.cancel, saveLabel: t.save });

export function StaffOffice() {
  const { locale, t } = useCopy();
  const [rows, setRows] = useState([]);
  const [query, setQuery] = useState("");
  const [roleFilter, setRoleFilter] = useState("");
  const [deptFilter, setDeptFilter] = useState("");
  const [accountFilter, setAccountFilter] = useState("");
  const [error, setError] = useState("");
  const [form, setForm] = useState(null);
  const [view, setView] = useState(null);
  const [confirm, setConfirm] = useState(null);
  const [notice, setNotice] = useState("");
  const [viewTab, setViewTab] = useState("overview");

  const load = () => {
    setError("");
    getData("/api/staff").then(setRows).catch(() => setError(t.loadFailed));
  };
  useEffect(load, [t.loadFailed]);

  const shown = rows.filter((row) => {
    const blob = `${row.fullName} ${row.role} ${row.email || ""} ${row.phone || ""}`.toLowerCase();
    if (query && !blob.includes(query.toLowerCase())) return false;
    if (roleFilter && row.role !== roleFilter) return false;
    if (deptFilter && row.department !== deptFilter) return false;
    if (accountFilter && (row.account || "NONE") !== accountFilter) return false;
    return true;
  });
  const count = (department) => rows.filter((row) => row.department === department).length;
  const dept = (value) => (value === "Technical" ? t.technical : value === "Medical" ? t.medical : value === "Support" ? t.support : value === "Administration" ? t.deptAdmin : value);
  const job = (value) => t[STAFF_KEY[value]] || value;

  const save = async () => {
    setNotice("");
    const invalid = phoneProblem(form.phone, locale);
    if (invalid) {
      setNotice(invalid);
      return;
    }
    const body = {
      fullName: form.fullName, roleName: form.roleName, department: form.department, phone: form.phone, email: form.email,
      seasonalPay: 0, advanceAtSigning: 0, sfExempt: false,
      dateOfBirth: form.dateOfBirth || null, contractStart: form.contractStart || null, contractEnd: form.contractEnd || null, jobTitle: form.jobTitle || "",
      account: form.accountEnabled ? { enabled: true, email: form.loginEmail || form.email, sendInvitation: form.sendInvitation !== false, language: locale } : null,
    };
    try {
      const saved = form.id ? await api.put(`/api/staff/${form.id}`, body) : await api.post("/api/staff", body);
      const id = form.id || saved.data?.data?.id;
      if (form.photoFile && id) await sendPhoto(`/api/staff/${id}/photo`, form.photoFile);
      setForm(null);
      load();
    } catch (err) {
      setNotice(err?.message === "size" ? t.photoHint : messageOf(err, t));
    }
  };

  const blank = { fullName: "", roleName: "Assistant Coach", department: "Technical", phone: "", email: "", photoFile: null, jobTitle: "", dateOfBirth: "", contractStart: "", contractEnd: "", accountEnabled: false, loginEmail: "", sendInvitation: true };

  return (
    <Page
      t={t}
      title={t.titleStaff}
      subtitle={t.subStaff}
      query={query}
      setQuery={setQuery}
      filters={(
        <>
          <select className="vfc-control" aria-label={t.role} value={roleFilter} onChange={(event) => setRoleFilter(event.target.value)}>
            <option value="">{t.anyRole}</option>
            {STAFF_ROLES.map((value) => <option key={value} value={value}>{job(value)}</option>)}
          </select>
          <select className="vfc-control" aria-label={t.department} value={deptFilter} onChange={(event) => setDeptFilter(event.target.value)}>
            <option value="">{t.anyDepartment}</option>
            <option value="Technical">{t.technical}</option>
            <option value="Medical">{t.medical}</option>
            <option value="Support">{t.support}</option>
            <option value="Administration">{t.deptAdmin}</option>
          </select>
          <select className="vfc-control" aria-label={t.account} value={accountFilter} onChange={(event) => setAccountFilter(event.target.value)}>
            <option value="">{t.anyAccount}</option>
            <option value="NONE">{t.noAccount}</option>
            <option value="INVITED">{t.invited}</option>
            <option value="ACTIVE">{t.active}</option>
            <option value="SUSPENDED">{t.suspended}</option>
          </select>
        </>
      )}
      onAdd={() => { setNotice(""); setForm(blank); }}
      onExport={(format) => {
        if (format === "csv") csv("staff.csv", ["name,role,department,phone,email", ...shown.map((row) => [row.fullName, row.role, row.department, row.phone || "", row.email || ""].join(","))]);
        else downloadFile("/api/staff", format, locale, query);
      }}
      kpis={[
        { label: t.total, value: rows.length },
        { label: t.technical, value: count("Technical") },
        { label: t.medical, value: count("Medical") },
        { label: t.support, value: count("Support") },
        { label: t.withAccount, value: rows.filter((row) => row.account && row.account !== "NONE").length },
      ]}
      error={error}
      onRetry={load}
    >
      <DataTable
        actionsLabel={t.actions}
        rows={shown}
        empty={t.emptyStaff}
        columns={[
          { key: "name", label: t.name, render: (row) => <Person name={row.fullName} src={row.photoUrl} /> },
          { key: "role", label: t.role, render: (row) => <Badge>{job(row.role)}</Badge> },
          { key: "department", label: t.department, render: (row) => <Badge tone="neutral">{dept(row.department)}</Badge> },
          { key: "phone", label: t.phone, render: (row) => row.phone || t.notProvided },
          { key: "email", label: t.email, render: (row) => row.email || t.notProvided },
          { key: "contract", label: t.contractEnd, render: (row) => row.contractEnd || t.notProvided },
          { key: "status", label: t.status, render: (row) => <Badge tone={row.status === "ACTIVE" ? "good" : "warn"}>{row.status === "ACTIVE" || !row.status ? t.active : row.status}</Badge> },
          { key: "account", label: t.account, render: (row) => <AccountBadge t={t} status={row.account} /> },
        ]}
        actions={(row) => (
          <>
            <Actions
              t={t}
              onView={() => { setViewTab("overview"); setView(row); }}
              onEdit={() => { setNotice(""); setForm({ id: row.id, fullName: row.fullName, roleName: row.role, department: row.department, phone: row.phone || "", email: row.email || "", photoFile: null, photoUrl: row.photoUrl, jobTitle: row.jobTitle || "", dateOfBirth: row.dateOfBirth || "", contractStart: row.contractStart || "", contractEnd: row.contractEnd || "", account: row.account, accountEnabled: row.account && row.account !== "NONE", loginEmail: row.email || "", sendInvitation: true }); }}
              onDelete={() => setConfirm(row)}
            />
            <StaffMenu t={t} row={row} language={locale} onDone={load} onError={(err) => setNotice(messageOf(err, t))} />
          </>
        )}
      />
      {notice && !form ? <p className="crud-error">{notice}</p> : null}
      {form ? (
        <StaffForm
          t={t}
          form={form}
          setForm={setForm}
          roles={STAFF_ROLES}
          job={job}
          language={locale}
          onClose={() => setForm(null)}
          onSubmit={save}
          onAccountDone={() => { setForm(null); load(); }}
          onAccountError={(err) => setNotice(messageOf(err, t))}
          error={notice}
        />
      ) : null}
      {view ? (
        <ViewModal
          labels={viewLabels(t)}
          title={view.fullName}
          subtitle={job(view.role)}
          photo={view.photoUrl}
          badges={<><Badge>{dept(view.department)}</Badge><AccountBadge t={t} status={view.account} /></>}
          facts={viewTab === "overview" ? [
            { label: t.phone, value: view.phone },
            { label: t.email, value: view.email },
            { label: t.jobTitle, value: view.jobTitle },
            { label: t.dateOfBirth, value: view.dateOfBirth },
            { label: t.contractStart, value: view.contractStart },
            { label: t.contractEnd, value: view.contractEnd },
            { label: t.status, value: view.status === "ACTIVE" || !view.status ? t.active : view.status },
          ] : []}
          onClose={() => setView(null)}
          onEdit={() => { setForm({ id: view.id, fullName: view.fullName, roleName: view.role, department: view.department, phone: view.phone || "", email: view.email || "", photoFile: null, photoUrl: view.photoUrl, jobTitle: view.jobTitle || "", dateOfBirth: view.dateOfBirth || "", contractStart: view.contractStart || "", contractEnd: view.contractEnd || "", account: view.account, accountEnabled: view.account && view.account !== "NONE", loginEmail: view.email || "", sendInvitation: true }); setView(null); }}
          onDelete={() => { setConfirm(view); setView(null); }}
        >
          <Tabs label={t.account} value={viewTab} onChange={setViewTab} items={[{ id: "overview", label: t.overviewTab }, { id: "account", label: t.accountTab }]} />
          {viewTab === "account" ? <AccountPanel t={t} row={view} language={locale} onDone={() => { setView(null); load(); }} onError={(err) => setNotice(messageOf(err, t))} /> : null}
        </ViewModal>
      ) : null}
      {confirm ? (
        <PersonDeleteDialog t={t} person={{ id: confirm.id, name: confirm.fullName, kind: "staff" }} onClose={() => setConfirm(null)} onDone={() => { setConfirm(null); load(); }} />
      ) : null}
    </Page>
  );
}

export function AdministrationOffice() {
  const { locale, t } = useCopy();
  const [rows, setRows] = useState([]);
  const [roles, setRoles] = useState([]);
  const [query, setQuery] = useState("");
  const [roleFilter, setRoleFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [otpFilter, setOtpFilter] = useState("");
  const [error, setError] = useState("");
  const [form, setForm] = useState(null);
  const [view, setView] = useState(null);
  const [notice, setNotice] = useState("");
  const [removing, setRemoving] = useState(null);

  const load = () => {
    setError("");
    Promise.all([getData("/api/users"), getData("/api/roles")])
      .then(([users, roleRows]) => {
        setRows((users || []).filter((user) => ADMIN_ROLES.includes(user.role)));
        setRoles(roleRows || []);
      })
      .catch(() => setError(t.loadFailed));
  };
  useEffect(load, [t.loadFailed]);

  const shown = useMemo(() => rows.filter((row) => {
    if (query && !`${row.fullName} ${row.email}`.toLowerCase().includes(query.toLowerCase())) return false;
    if (roleFilter && row.role !== roleFilter) return false;
    if (statusFilter === "ACTIVE" && !row.active) return false;
    if (statusFilter === "SUSPENDED" && row.active) return false;
    if (otpFilter === "REQUIRED" && !["PRESIDENT", "GENERAL_MANAGER", "FINANCE_MANAGER"].includes(row.role)) return false;
    if (otpFilter === "OPTIONAL" && ["PRESIDENT", "GENERAL_MANAGER", "FINANCE_MANAGER"].includes(row.role)) return false;
    return true;
  }), [rows, query, roleFilter, statusFilter, otpFilter]);
  const named = (code) => roleName(roles.find((role) => role.code === code), locale, t) || code;
  const seats = roles.find((role) => role.code === form?.role);
  const otp = (code) => ["PRESIDENT", "GENERAL_MANAGER", "FINANCE_MANAGER"].includes(code);

  const save = async () => {
    setNotice("");
    const invalid = phoneProblem(form.phone, locale);
    if (invalid) {
      setNotice(invalid);
      return;
    }
    try {
      const saved = form.id
        ? await api.put(`/api/users/${form.id}`, { fullName: form.fullName, role: form.role, phone: form.phone, active: true })
        : await api.post("/api/users", { fullName: form.fullName, email: form.email, phone: form.phone, role: form.role });
      const id = form.id || saved.data?.data?.id;
      if (form.photoFile && id) await sendPhoto(`/api/users/${id}/photo`, form.photoFile);
      setForm(null);
      load();
    } catch (err) {
      setNotice(err?.message === "size" ? t.photoHint : messageOf(err, t));
    }
  };

  return (
    <Page
      t={t}
      title={t.titleAdmin}
      subtitle={t.subAdmin}
      query={query}
      setQuery={setQuery}
      filters={(
        <>
          <select className="vfc-control" aria-label={t.role} value={roleFilter} onChange={(event) => setRoleFilter(event.target.value)}>
            <option value="">{t.anyRole}</option>
            {ADMIN_ROLES.map((code) => <option key={code} value={code}>{named(code)}</option>)}
          </select>
          <select className="vfc-control" aria-label={t.status} value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
            <option value="">{t.anyStatus}</option>
            <option value="ACTIVE">{t.active}</option>
            <option value="SUSPENDED">{t.suspended}</option>
          </select>
          <select className="vfc-control" aria-label={t.otp} value={otpFilter} onChange={(event) => setOtpFilter(event.target.value)}>
            <option value="">{t.anyOtp}</option>
            <option value="REQUIRED">{t.required}</option>
            <option value="OPTIONAL">{t.optional}</option>
          </select>
        </>
      )}
      onAdd={() => { setNotice(""); setForm({ fullName: "", email: "", phone: "", role: "SOCIAL_MEDIA_MANAGER", photoFile: null }); }}
      onExport={(format) => {
        if (format === "csv") csv("administration.csv", ["name,email,role,status", ...shown.map((row) => `${row.fullName},${row.email},${row.role},${row.active ? "Active" : "Suspended"}`)]);
        else downloadFile("/api/users", format, locale, query);
      }}
      kpis={[
        { label: t.accounts, value: rows.length },
        { label: t.active, value: rows.filter((row) => row.active).length },
        { label: t.invited, value: rows.filter((row) => row.mustChangePassword).length },
        { label: t.suspended, value: rows.filter((row) => !row.active).length },
      ]}
      error={error}
      onRetry={load}
    >
      <DataTable
        actionsLabel={t.actions}
        rows={shown}
        empty={t.emptyAdmin}
        columns={[
          { key: "name", label: t.name, render: (row) => <Person name={row.fullName} src={row.photoUrl} /> },
          { key: "email", label: t.email },
          { key: "role", label: t.role, render: (row) => <Badge>{named(row.role)}</Badge> },
          { key: "status", label: t.status, render: (row) => <Badge tone={row.active ? "good" : "bad"}>{row.active ? t.active : t.suspended}</Badge> },
          { key: "otp", label: t.otp, render: (row) => <Badge>{otp(row.role) ? t.required : t.optional}</Badge> },
          { key: "login", label: t.lastLogin, render: (row) => row.lastLoginAt ? String(row.lastLoginAt).replace("T", " ").slice(0, 16) : t.notProvided },
        ]}
        actions={(row) => (
          <>
            <Actions
              t={t}
              onView={() => setView(row)}
              onEdit={() => { setNotice(""); setForm({ id: row.id, fullName: row.fullName, email: row.email, phone: row.phone || "", role: row.role, photoFile: null }); }}
              onDelete={() => setRemoving(row)}
            />
            <Button onClick={async () => {
              try {
                await api.post(`/api/users/${row.id}/${row.active ? "disable" : "enable"}`);
                load();
              } catch (err) {
                setNotice(err?.message || t.saveFailed);
              }
            }}>{row.active ? t.suspendAccount : t.activateAccount}</Button>
          </>
        )}
      />
      {notice && !form ? <p className="crud-error">{notice}</p> : null}
      {form ? (
        <FormModal
          {...formLabels(t)}
          title={form.id ? t.editMember : t.addMember}
          subtitle={seats?.maxSeats ? fill(t.seatsUsed, { used: seats.users || 0, max: seats.maxSeats }) : t.adminOnly}
          fields={[
            { key: "fullName", label: t.fullName, required: true },
            { key: "email", label: t.email, type: "email", required: !form.id },
            { key: "phone", label: t.phone, type: "phone", hint: t.use250 },
            { key: "role", label: t.role, type: "select", options: ADMIN_ROLES.map((code) => ({ value: code, label: named(code) })) },
            { key: "photoFile", label: t.photo, type: "file", hint: t.photoHint, span: 2 },
          ]}
          value={form}
          onChange={setForm}
          onClose={() => setForm(null)}
          onSubmit={save}
          error={notice}
        />
      ) : null}
      {view ? (
        <ViewModal
          labels={viewLabels(t)}
          title={view.fullName}
          subtitle={view.email}
          photo={view.photoUrl}
          badges={<><Badge>{named(view.role)}</Badge><Badge>{otp(view.role) ? t.required : t.optional}</Badge></>}
          facts={[
            { label: t.phone, value: view.phone },
            { label: t.status, value: view.active ? t.active : t.suspended },
            { label: t.account, value: view.mustChangePassword ? t.passwordPending : t.completed },
            { label: t.lastLogin, value: view.lastLoginAt ? String(view.lastLoginAt).replace("T", " ").slice(0, 16) : "" },
          ]}
          onClose={() => setView(null)}
          onEdit={() => { setForm({ id: view.id, fullName: view.fullName, email: view.email, phone: view.phone || "", role: view.role, photoFile: null }); setView(null); }}
          onDelete={() => { setRemoving(view); setView(null); }}
        />
      ) : null}
      {removing ? (
        <PersonDeleteDialog t={t} person={{ id: removing.id, name: removing.fullName, kind: "user" }} onClose={() => setRemoving(null)} onDone={() => { setRemoving(null); load(); }} />
      ) : null}
    </Page>
  );
}

export function RolesOffice() {
  const { locale, t } = useCopy();
  const toast = useAdminToast();
  const [rows, setRows] = useState([]);
  const [query, setQuery] = useState("");
  const [categoryFilter, setCategoryFilter] = useState("");
  const [kindFilter, setKindFilter] = useState("");
  const [otpFilter, setOtpFilter] = useState("");
  const [error, setError] = useState("");
  const [form, setForm] = useState(null);
  const [view, setView] = useState(null);
  const [notice, setNotice] = useState("");
  const [pending, setPending] = useState(null);

  const load = () => {
    setError("");
    getData("/api/roles").then(setRows).catch(() => setError(t.loadFailed));
  };
  useEffect(load, [t.loadFailed]);

  const shown = rows.filter((row) => {
    const blob = `${row.nameEn || ""} ${row.nameFr || ""} ${row.nameRw || ""} ${row.name || ""} ${row.code || ""}`.toLowerCase();
    if (query && !blob.includes(query.toLowerCase())) return false;
    if (categoryFilter && row.category !== categoryFilter) return false;
    if (kindFilter === "PROTECTED" && !row.locked) return false;
    if (kindFilter === "STANDARD" && (row.locked || !row.systemRole)) return false;
    if (kindFilter === "CUSTOM" && (row.locked || row.systemRole)) return false;
    if (otpFilter === "REQUIRED" && !row.otpRequired) return false;
    if (otpFilter === "OPTIONAL" && row.otpRequired) return false;
    return true;
  });
  const named = (row) => roleName(row, locale, t);
  const kind = (row) => (row.locked ? t.protected : row.systemRole ? t.standard : t.custom);
  const category = (value) => (value === "ADMINISTRATION" ? t.administration : value === "PLAYER" ? t.player : t.staff);

  const save = async () => {
    setNotice("");
    if (!form.systemRole && !(form.name || "").trim()) {
      setNotice(t.nameRequired);
      return;
    }
    try {
      const sourceLang = form.sourceLang || locale;
      const body = {
        ...form,
        name: form.name,
        sourceLang,
        nameEn: sourceLang === "en" ? form.name : (form.nameEn || ""),
        nameFr: form.nameFr || "",
        nameRw: form.nameRw || "",
        otpRequired: Boolean(form.otpRequired),
        active: form.active !== false,
        maxSeats: form.maxSeats === "" || form.maxSeats == null ? null : Number(form.maxSeats),
      };
      if (form.id) await api.put(`/api/roles/${form.id}`, body);
      else await api.post("/api/roles", body);
      setForm(null);
      load();
    } catch (err) {
      setNotice(messageOf(err, t));
    }
  };

  const askDelete = (row) => {
    if (row.locked) return;
    setPending({ row, target: "" });
    setView(null);
  };

  return (
    <Page
      t={t}
      title={t.titleRoles}
      subtitle={t.subRoles}
      query={query}
      setQuery={setQuery}
      filters={(
        <>
          <select className="vfc-control" aria-label={t.category} value={categoryFilter} onChange={(event) => setCategoryFilter(event.target.value)}>
            <option value="">{t.anyCategory}</option>
            <option value="ADMINISTRATION">{t.administration}</option>
            <option value="STAFF">{t.staff}</option>
            <option value="PLAYER">{t.player}</option>
          </select>
          <select className="vfc-control" aria-label={t.roles} value={kindFilter} onChange={(event) => setKindFilter(event.target.value)}>
            <option value="">{t.anyKind}</option>
            <option value="PROTECTED">{t.protected}</option>
            <option value="STANDARD">{t.standard}</option>
            <option value="CUSTOM">{t.custom}</option>
          </select>
          <select className="vfc-control" aria-label={t.otp} value={otpFilter} onChange={(event) => setOtpFilter(event.target.value)}>
            <option value="">{t.anyOtp}</option>
            <option value="REQUIRED">{t.required}</option>
            <option value="OPTIONAL">{t.optional}</option>
          </select>
        </>
      )}
      onAdd={() => { setNotice(""); setForm({ name: "", nameFr: "", nameRw: "", sourceLang: locale, category: "STAFF", description: "", color: "#a3283a", otpRequired: false, maxSeats: "", active: true }); }}
      onExport={(format) => {
        if (format === "csv") csv("roles.csv", ["name,category,otp,users", ...shown.map((row) => `${named(row)},${row.category},${row.otpRequired},${row.users}`)]);
        else downloadFile("/api/roles", format, locale, query);
      }}
      kpis={[
        { label: t.roles, value: rows.length },
        { label: t.protected, value: rows.filter((row) => row.locked).length },
        { label: t.custom, value: rows.filter((row) => !row.systemRole).length },
        { label: t.otpRequired, value: rows.filter((row) => row.otpRequired).length },
      ]}
      error={error}
      onRetry={load}
    >
      <DataTable
        actionsLabel={t.actions}
        rows={shown}
        empty={t.emptyRoles}
        columns={[
          { key: "name", label: t.role, render: (row) => <Badge>{named(row)}</Badge> },
          { key: "category", label: t.category, render: (row) => <Badge tone="neutral">{category(row.category)}</Badge> },
          { key: "description", label: t.description, render: (row) => row.description || t.notProvided },
          { key: "users", label: t.users },
          { key: "otp", label: t.otp, render: (row) => <Badge>{row.otpRequired ? t.required : t.optional}</Badge> },
          { key: "type", label: t.type, render: (row) => kind(row) },
          { key: "seats", label: t.maxUsers, render: (row) => row.maxSeats ?? t.notProvided },
        ]}
        actions={(row) => (
          <Actions
            t={t}
            editDisabled={row.locked}
            deleteDisabled={row.locked}
            deleteTitle={row.locked ? t.roleProtected : t.delete}
            onView={() => setView(row)}
            onEdit={() => { if (!row.locked) { setNotice(""); setForm({ ...row, name: row.name || "" }); } }}
            onDelete={() => askDelete(row)}
          />
        )}
      />
      {form ? (
        <FormModal
          {...formLabels(t)}
          title={form.id ? t.editRole : t.addRole}
          subtitle={t.codeHint}
          before={form.systemRole ? null : <TranslatedName value={form} onChange={setForm} onError={toast} />}
          fields={[
            { key: "category", label: t.category, type: "select", options: [{ value: "ADMINISTRATION", label: t.administration }, { value: "STAFF", label: t.staff }, { value: "PLAYER", label: t.player }] },
            { key: "description", label: t.description, type: "textarea", span: 2 },
            { key: "color", label: t.color },
            { key: "maxSeats", label: t.maxUsers },
            { key: "otpRequired", label: t.requireCode, type: "switch" },
            { key: "active", label: t.roleActive, type: "switch" },
          ]}
          value={form}
          onChange={setForm}
          onClose={() => setForm(null)}
          onSubmit={save}
          error={notice}
        />
      ) : null}
      {view ? (
        <ViewModal
          labels={viewLabels(t)}
          title={named(view)}
          subtitle={category(view.category)}
          code={view.code}
          deleteDisabled={view.locked}
          deleteTitle={view.locked ? t.roleProtected : t.delete}
          badges={<><Badge>{category(view.category)}</Badge><Badge>{view.otpRequired ? t.required : t.optional}</Badge><Badge>{kind(view)}</Badge></>}
          facts={[
            { label: t.nameFr, value: view.nameFr },
            { label: t.nameRw, value: view.nameRw },
            { label: t.description, value: view.description },
            { label: t.users, value: view.users },
          ]}
          onClose={() => setView(null)}
          onEdit={() => { if (!view.locked) { setForm({ ...view, name: view.name || "" }); setView(null); } }}
          onDelete={() => askDelete(view)}
        />
      ) : null}
      {pending ? (
        <RoleDeleteDialog t={t} role={pending.row} roles={rows} roleName={named(pending.row)} onClose={() => setPending(null)} onDone={() => { setPending(null); setView(null); load(); }} />
      ) : null}
    </Page>
  );
}
