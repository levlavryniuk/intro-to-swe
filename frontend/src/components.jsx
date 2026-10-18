import { useId } from 'react';
import { STATUS_LABELS } from './format.js';

/** A labelled form field with its error message underneath (NFR-04: labels and clear errors). */
export function Field({ label, hint, error, as = 'input', children, ...inputProps }) {
  const id = useId();
  const errorId = `${id}-error`;
  const Control = as;
  const describedBy = error ? errorId : undefined;
  if (as === 'input' && !inputProps.type) {
    inputProps.type = 'text';
  }
  return (
    <div>
      <label htmlFor={id}>
        {label} {hint && <span className="hint">{hint}</span>}
      </label>
      <Control id={id} aria-invalid={error ? 'true' : undefined} aria-describedby={describedBy} {...inputProps}>
        {children}
      </Control>
      {error && <div className="field-error" id={errorId}>{error}</div>}
    </div>
  );
}

export function Checkbox({ label, ...inputProps }) {
  return (
    <label className="inline">
      <input type="checkbox" {...inputProps} /> {label}
    </label>
  );
}

/** A message box. kind is ok, err or warn; nothing is shown without text. */
export function Message({ text, kind = 'ok' }) {
  if (!text) return null;
  return (
    <div className={`msg ${kind}`} role={kind === 'err' ? 'alert' : 'status'}>
      {text}
    </div>
  );
}

export function StatusBadge({ status }) {
  return <span className={`badge ${status}`}>{STATUS_LABELS[status] || status}</span>;
}

/** Turns an API result into the { text, kind } shown by <Message>. */
export function resultMessage(res, okText) {
  return res.ok ? { text: okText ?? res.data.message, kind: 'ok' } : { text: res.data.message, kind: 'err' };
}
