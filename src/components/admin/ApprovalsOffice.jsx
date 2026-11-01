"use client";

import { useEffect, useState } from "react";
import { api, getData } from "@/services/api";
import { useLocale } from "@/components/providers/LocaleProvider";
import { useAuth } from "@/components/providers/AuthProvider";
import { useAdminToast } from "./toast";
import { Button, Field, Modal, Tabs, TextArea, TextInput } from "@/ui/kit";
import { APPROVALS } from "./formerCopy";

const ROLES = ["PRESIDENT", "GENERAL_MANAGER", "FINANCE_MANAGER"];

function fill(template, values) {
  return Object.entries(values).reduce((text, [key, value]) => text.replaceAll(`{${key}}`, String(value)), template || "");
}

export function ApprovalsOffice() {
  const { locale } = useLocale();
  const t = APPROVALS[locale] || APPROVALS.en;
  const { user } = useAuth();
  const toast = useAdminToast();
  const [tab, setTab] = useState("pending");
  const [rows, setRows] = useState([]);
  const [error, setError] = useState("");
  const [open, setOpen] = useState(null);
  const [comment, setComment] = useState("");
  const [otp, setOtp] = useState("");
  const [dialogError, setDialogError] = useState("");

  const load = () => {
    getData("/api/deletion-requests").then((list) => {
      setRows((current) => {
        if (current.length && JSON.stringify(current.map((row) => [row.id, row.status, row.approvals])) !== JSON.stringify((list || []).map((row) => [row.id, row.status, row.approvals]))) {
          toast(t.changed);
        }
        return list || [];
      });
    }).catch(() => setError(t.loadFailed));
  };
  useEffect(() => {
    load();
    const timer = setInterval(load, 8000);
    return () => clearInterval(timer);
  }, [t.loadFailed]);

  const shown = rows.filter((row) => tab === "pending" ? row.status === "PENDING" : row.status !== "PENDING");
  const mine = open?.votes?.some((vote) => vote.role === user?.role);
  const closed = open && open.status !== "PENDING";

  const vote = async (path) => {
    setDialogError("");
    try {
      const saved = await api.post(path, { comment, otp });
      setOpen(saved.data?.data || null);
      setComment("");
      load();
    } catch (err) {
      setDialogError(err.code === "OTP_REQUIRED" ? t.code : err.code === "COMMENT_REQUIRED" ? t.comment : err.code === "VOTE_LOCKED" ? t.already : err.code === "REQUEST_CLOSED" ? t.closed : (err.message || t.loadFailed));
    }
  };

  return (
    <section>
      <h1>{t.title}</h1>
      <Tabs label={t.title} value={tab} onChange={setTab} items={[{ id: "pending", label: t.pending }, { id: "decided", label: t.decided }]} />
      {error ? <p className="crud-error">{error}</p> : null}
      <div className="role-people">
        {shown.map((row) => (
          <article key={row.id} className="admin-card">
            <strong>{row.player}</strong>
            <p>{t.reason}: {row.reason}</p>
            <p>{fill(t.progress, { n: row.approvals || 0 })}</p>
            <p>{ROLES.map((role) => {
              const cast = (row.votes || []).find((item) => item.role === role);
              const label = !cast ? t.waiting : cast.decision === "REJECT" ? t.rejected : t.approved;
              return `${role}: ${label}`;
            }).join(" · ")}</p>
            <Button onClick={() => { setOpen(row); setDialogError(""); }}>{t.view}</Button>
          </article>
        ))}
      </div>
      {open ? (
        <Modal title={open.player} size="md" onClose={() => setOpen(null)} footer={(
          <div className="vfc-foot-right pair-actions">
            <Button onClick={() => setOpen(null)}>{t.close}</Button>
            <Button title={mine ? t.already : closed ? t.closed : ""} disabled={mine || closed} onClick={() => vote(`/api/deletion-requests/${open.id}/approve`)}>{t.approve}</Button>
            <Button variant="danger" title={mine ? t.already : closed ? t.closed : ""} disabled={mine || closed || comment.trim().length < 3} onClick={() => vote(`/api/deletion-requests/${open.id}/reject`)}>{t.reject}</Button>
          </div>
        )}>
          <p>{fill(t.progress, { n: open.approvals || 0 })}</p>
          <p>{open.history}</p>
          <p>{t.reason}: {open.reason}</p>
          <ul>
            {(open.votes || []).map((item) => <li key={item.role}>{item.role}: {item.decision === "REJECT" ? t.rejected : t.approved} — {item.comment}</li>)}
          </ul>
          <Field label={t.comment}><TextArea value={comment} onChange={(event) => setComment(event.target.value)} /></Field>
          <Field label={t.code}><TextInput value={otp} onChange={(event) => setOtp(event.target.value)} /></Field>
          <Button onClick={() => api.post("/api/auth/otp/request", { email: user.email, purpose: "STEP_UP" })}>{t.sendCode}</Button>
          {dialogError ? <p className="crud-error">{dialogError}</p> : null}
        </Modal>
      ) : null}
    </section>
  );
}
