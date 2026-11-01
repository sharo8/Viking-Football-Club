"use client";

import { useState } from "react";
import { api } from "@/services/api";
import { Button, Field, Modal, PhoneInput, SelectInput, Stepper, Switch, TextInput } from "@/ui/kit";
import { ConfirmDialog } from "@/crud/kit";
import { PhotoUpload } from "./PhotoUpload";
import { ReasonFields, reasonReady } from "./RemovalDialogs";
import { phoneProblem } from "@/lib/phone";

const DEPT = {
  "Head Coach": "Technical",
  "Assistant Coach": "Technical",
  "Goalkeeper Coach": "Technical",
  "Fitness Coach": "Technical",
  Doctor: "Medical",
  Physiotherapist: "Medical",
  "Kit Manager": "Support",
  "Team Manager": "Administration",
};
const OTP = new Set(["Head Coach", "Doctor", "Kit Manager"]);
const ACCESS = {
  "Head Coach": "accessHeadCoach",
  "Assistant Coach": "accessAssistantCoach",
  "Goalkeeper Coach": "accessGoalkeeperCoach",
  "Fitness Coach": "accessFitnessCoach",
  Doctor: "accessDoctor",
  Physiotherapist: "accessPhysiotherapist",
  "Kit Manager": "accessKitManager",
  "Team Manager": "accessTeamManager",
};
const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function StaffForm({ t, form, setForm, roles, job, language, onClose, onSubmit, onAccountDone, onAccountError, error }) {
  const [step, setStep] = useState(0);
  const deptName = (value) => (value === "Technical" ? t.technical : value === "Medical" ? t.medical : value === "Support" ? t.support : value === "Administration" ? t.deptAdmin : value);
  const setRole = (roleName) => setForm({ ...form, roleName, department: DEPT[roleName] || "Support" });
  const emailOk = !form.accountEnabled || EMAIL.test((form.loginEmail || "").trim());
  const identityOk = (form.fullName || "").trim().length > 1;
  const phoneMsg = phoneProblem(form.phone, language);
  const next = () => {
    if (step === 0 && !identityOk) return;
    if (step === 1 && (phoneMsg || (form.email && !EMAIL.test(form.email)))) return;
    setStep((value) => Math.min(2, value + 1));
  };

  return (
    <Modal
      title={form.id ? t.editStaff : t.addStaff}
      subtitle={t.staffOnly}
      size="lg"
      onClose={onClose}
      closeLabel={t.close}
      footer={(
        <div className="vfc-foot-right pair-actions">
          <Button onClick={step === 0 ? onClose : () => setStep(step - 1)}>{step === 0 ? t.cancel : t.back}</Button>
          {step < 2 ? <Button variant="primary" disabled={(step === 0 && !identityOk) || (step === 1 && Boolean(phoneMsg))} onClick={next}>{t.next}</Button> : <Button variant="primary" disabled={(form.accountEnabled && !emailOk) || Boolean(phoneMsg)} onClick={onSubmit}>{t.save}</Button>}
        </div>
      )}
    >
      <Stepper steps={[t.stepIdentity, t.stepRole, t.stepAccount]} current={step} onSelect={setStep} label={t.stepAccount} />
      {step === 0 ? (
        <div className="crud-grid">
          <div className="span-2">
            <PhotoUpload name={form.fullName} src={form.photoUrl} hint={t.photoHint} dropLabel={t.photoDrop} onFile={(file) => setForm({ ...form, photoFile: file })} onError={() => setForm({ ...form, photoError: t.photoHint })} />
            {form.photoError ? <p className="crud-error">{form.photoError}</p> : null}
          </div>
          <Field label={t.fullName} required><TextInput value={form.fullName} onChange={(event) => setForm({ ...form, fullName: event.target.value })} /></Field>
          <Field label={t.dateOfBirth}><TextInput type="date" value={form.dateOfBirth || ""} onChange={(event) => setForm({ ...form, dateOfBirth: event.target.value })} /></Field>
        </div>
      ) : null}
      {step === 1 ? (
        <div className="crud-grid">
          <Field label={t.role} required>
            <SelectInput value={form.roleName} onChange={(event) => setRole(event.target.value)}>
              {roles.map((value) => <option key={value} value={value}>{job(value)}</option>)}
            </SelectInput>
          </Field>
          <Field label={t.department}><TextInput value={deptName(form.department)} readOnly /></Field>
          <Field label={t.jobTitle}><TextInput value={form.jobTitle || ""} onChange={(event) => setForm({ ...form, jobTitle: event.target.value })} /></Field>
          <Field label={t.phone} error={phoneMsg} hint={phoneMsg ? undefined : t.use250}><PhoneInput value={form.phone || ""} aria-label={t.phone} onChange={(value) => setForm({ ...form, phone: value })} /></Field>
          <Field label={t.email}><TextInput type="email" value={form.email || ""} onChange={(event) => setForm({ ...form, email: event.target.value, loginEmail: form.loginTouched ? form.loginEmail : event.target.value })} /></Field>
          <Field label={t.contractStart}><TextInput type="date" value={form.contractStart || ""} onChange={(event) => setForm({ ...form, contractStart: event.target.value })} /></Field>
          <Field label={t.contractEnd}><TextInput type="date" value={form.contractEnd || ""} onChange={(event) => setForm({ ...form, contractEnd: event.target.value })} /></Field>
        </div>
      ) : null}
      {step === 2 ? (
        <div className="crud-grid">
          <p className="span-2">{t.noAccountHelp}</p>
          {form.account && form.account !== "NONE" ? (
            <div className="span-2">
              <p>{form.account === "INVITED" ? t.invited : form.account === "SUSPENDED" ? t.suspended : t.active}</p>
              <AccountPanel t={t} row={{ id: form.id, fullName: form.fullName, email: form.loginEmail || form.email, account: form.account }} language={language} onDone={onAccountDone} onError={onAccountError} />
            </div>
          ) : (
            <>
              <div className="crud-switch span-2">
                <span>{t.canSignIn}</span>
                <Switch checked={Boolean(form.accountEnabled)} label={t.canSignIn} onChange={(checked) => setForm({ ...form, accountEnabled: checked, loginEmail: form.loginEmail || form.email || "" })} />
              </div>
              {form.accountEnabled ? (
                <>
                  <Field label={t.loginEmail} required><TextInput type="email" value={form.loginEmail || ""} onChange={(event) => setForm({ ...form, loginEmail: event.target.value, loginTouched: true })} /></Field>
                  <Field label={t.role}><TextInput value={job(form.roleName)} readOnly /></Field>
                  <p className="span-2">{t[ACCESS[form.roleName]]}</p>
                  <div className="crud-switch span-2">
                    <span>{t.sendInvitation}</span>
                    <Switch checked={form.sendInvitation !== false} label={t.sendInvitation} onChange={(checked) => setForm({ ...form, sendInvitation: checked })} />
                  </div>
                  <p className="span-2">{t.inviteOnce}</p>
                  <p className="span-2">{OTP.has(form.roleName) ? t.otpEvery : t.otpOptional}</p>
                </>
              ) : null}
            </>
          )}
        </div>
      ) : null}
      {error ? <p className="crud-error">{error}</p> : null}
    </Modal>
  );
}

export function AccountPanel({ t, row, language, onDone, onError }) {
  const [removing, setRemoving] = useState(false);
  const [code, setCode] = useState("");
  const [text, setText] = useState("");
  const [localError, setLocalError] = useState("");
  const run = async (path, body) => {
    try {
      if (path.endsWith("/account") && body?.reasonCode) await api.delete(path, { data: body });
      else await api.post(path, body || {});
      onDone();
    } catch (err) {
      onError(err);
    }
  };
  const none = !row.account || row.account === "NONE";
  return (
    <div className="crud-actions">
      {none ? <Button onClick={() => run(`/api/staff/${row.id}/account`, { email: row.email, sendInvitation: "true", language })}>{t.createAccount}</Button> : null}
      {row.account === "INVITED" ? <Button onClick={() => run(`/api/staff/${row.id}/resend-invitation`)}>{t.resendInvitation}</Button> : null}
      {!none ? <Button onClick={() => run(`/api/staff/${row.id}/reset-password`)}>{t.resetPassword}</Button> : null}
      {row.account === "SUSPENDED" ? <Button onClick={() => run(`/api/staff/${row.id}/activate`)}>{t.activateAccount}</Button> : null}
      {row.account === "ACTIVE" || row.account === "INVITED" ? <Button onClick={() => run(`/api/staff/${row.id}/suspend`)}>{t.suspendAccount}</Button> : null}
      {!none ? <Button variant="danger" onClick={() => { setRemoving(true); setLocalError(""); }}>{t.removeAccount}</Button> : null}
      {removing ? (
        <ConfirmDialog
          title={t.removeAccount}
          body={row.fullName}
          confirmLabel={t.delete}
          cancelLabel={t.cancel}
          confirmDisabled={!reasonReady(code, text)}
          error={localError}
          onClose={() => setRemoving(false)}
          onConfirm={async () => {
            try {
              await api.delete(`/api/staff/${row.id}/account`, { data: { reasonCode: code, reason: text } });
              setRemoving(false);
              onDone();
            } catch (err) {
              setLocalError(err?.code === "REASON_REQUIRED" ? t.reason : t.saveFailed);
            }
          }}
        >
          <ReasonFields t={t} code={code} text={text} onCode={setCode} onText={setText} />
        </ConfirmDialog>
      ) : null}
    </div>
  );
}
