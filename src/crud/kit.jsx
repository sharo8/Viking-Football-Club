"use client";

import { Avatar, Badge, Button, Field, Modal, PhoneInput, SelectInput, Switch, TextArea, TextInput } from "@/ui/kit";
import "./crud.css";
import { phoneProblem } from "@/lib/phone";

export function DataTable({ columns, rows, actions, empty, actionsLabel = "Actions" }) {
  return (
    <div className="crud-scroll">
      <table className="crud-table">
        <thead>
          <tr>
            {columns.map((column) => <th key={column.key}>{column.label}</th>)}
            {actions ? <th>{actionsLabel}</th> : null}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.id}>
              {columns.map((column) => <td key={column.key}>{column.render ? column.render(row) : (row[column.key] || "—")}</td>)}
              {actions ? <td><div className="crud-actions">{actions(row)}</div></td> : null}
            </tr>
          ))}
        </tbody>
      </table>
      {!rows.length ? <p className="crud-empty">{empty}</p> : null}
    </div>
  );
}

export function FormModal({ title, subtitle, fields, value, onChange, onClose, onSubmit, error, saving, cancelLabel = "Cancel", saveLabel = "Save", before }) {
  const phoneInvalid = fields.some((field) => field.type === "phone" && phoneProblem(value[field.key]));
  return (
    <Modal
      title={title}
      subtitle={subtitle}
      size="lg"
      onClose={onClose}
      closeLabel={cancelLabel}
      footer={(
        <>
          <span />
          <div className="vfc-foot-right">
            <Button onClick={onClose}>{cancelLabel}</Button>
            <Button variant="primary" loading={saving} {...(phoneInvalid ? { disabled: true } : {})} onClick={onSubmit}>{saveLabel}</Button>
          </div>
        </>
      )}
    >
      <div className="crud-grid">
        {before}
        {fields.map((field) => (
          <div key={field.key} className={field.span === 2 ? "span-2" : ""}>
            {field.type === "switch" ? (
              <div className="crud-switch">
                <div>
                  <strong>{field.label}</strong>
                  {field.hint ? <p className="vfc-hint">{field.hint}</p> : null}
                </div>
                <Switch checked={Boolean(value[field.key])} onChange={(next) => onChange({ ...value, [field.key]: next })} label={field.label} />
              </div>
            ) : (
              <Field label={field.label} required={field.required} hint={field.type === "phone" && phoneProblem(value[field.key]) ? undefined : field.hint} error={field.type === "phone" ? phoneProblem(value[field.key]) : undefined}>
                {field.type === "select" ? (
                  <SelectInput value={value[field.key] || ""} onChange={(event) => onChange({ ...value, [field.key]: event.target.value })}>
                    {(field.options || []).map((option) => (
                      <option key={option.value || option} value={option.value || option}>{option.label || option}</option>
                    ))}
                  </SelectInput>
                ) : field.type === "file" ? (
                  <input className="vfc-control" type="file" accept="image/jpeg,image/png,image/webp" onChange={(event) => onChange({ ...value, [field.key]: event.target.files?.[0] || null })} />
                ) : field.type === "textarea" ? (
                  <TextArea value={value[field.key] || ""} onChange={(event) => onChange({ ...value, [field.key]: event.target.value })} />
                ) : field.type === "phone" ? (
                  <PhoneInput value={value[field.key] || ""} aria-label={field.label} onChange={(next) => onChange({ ...value, [field.key]: next })} />
                ) : (
                  <TextInput type={field.type || "text"} value={value[field.key] || ""} onChange={(event) => onChange({ ...value, [field.key]: event.target.value })} />
                )}
              </Field>
            )}
          </div>
        ))}
      </div>
      {error ? <p className="crud-error">{error}</p> : null}
    </Modal>
  );
}

export function ViewModal({ title, subtitle, code, photo, badges, facts, onClose, onEdit, onDelete, labels = {}, deleteDisabled, deleteTitle, children }) {
  const missing = labels.empty || "Not provided";
  return (
    <Modal
      title={title}
      subtitle={subtitle}
      leading={<Avatar name={title} src={photo} size="96" />}
      size="md"
      onClose={onClose}
      closeLabel={labels.close || "Close"}
      footer={(
        <>
          <Button variant="danger" disabled={deleteDisabled} title={deleteTitle} onClick={onDelete}>{labels.delete || "Delete"}</Button>
          <div className="vfc-foot-right">
            <Button onClick={onClose}>{labels.close || "Close"}</Button>
            <Button variant="primary" onClick={onEdit}>{labels.edit || "Edit"}</Button>
          </div>
        </>
      )}
    >
      {code ? <p className="view-code">{code}</p> : null}
      <div className="view-badges">{badges}</div>
      <div className="crud-grid">
        {facts.map((fact) => (
          <article key={fact.label} className="view-card">
            <small>{fact.label}</small>
            <strong className={fact.value ? "" : "view-empty"}>{fact.value || missing}</strong>
          </article>
        ))}
      </div>
      {children}
    </Modal>
  );
}

export function ConfirmDialog({ title, body, onClose, onConfirm, confirmLabel = "Delete", cancelLabel = "Cancel", error, children, confirmDisabled = false }) {
  return (
    <Modal
      title={title}
      size="sm"
      onClose={onClose}
      footer={(
        <>
          <span />
          <div className="vfc-foot-right pair-actions">
            <Button onClick={onClose}>{cancelLabel}</Button>
            <Button variant="danger" disabled={confirmDisabled} onClick={onConfirm}>{confirmLabel}</Button>
          </div>
        </>
      )}
    >
      <p>{body}</p>
      {children}
      {error ? <p className="crud-error">{error}</p> : null}
    </Modal>
  );
}

export function Kpis({ items }) {
  return (
    <div className="crud-kpis">
      {items.map((item) => (
        <div key={item.label} className="crud-kpi">
          <strong>{item.value}</strong>
          <span>{item.label}</span>
        </div>
      ))}
    </div>
  );
}

export { Badge };
