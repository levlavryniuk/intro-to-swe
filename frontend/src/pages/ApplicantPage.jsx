import { useCallback, useEffect, useState } from 'react';
import { api } from '../api.js';
import { Field, Message, StatusBadge } from '../components.jsx';
import { DOC_LABELS, DOCUMENTS, fileProblem, fmtDate, fmtDateTime, fmtSize } from '../format.js';

const TEXT_FIELDS = ['phone', 'address', 'lastSchool', 'graduationYear', 'gradeAverage'];

/** UC-3 Submit, UC-4 Edit, UC-5 Withdraw and UC-6 Track Application Status. */
export default function ApplicantPage() {
  const [view, setView] = useState(undefined); // undefined: loading, null: no application yet
  const [programs, setPrograms] = useState([]);
  const [editing, setEditing] = useState(false);
  const [message, setMessage] = useState({});

  const load = useCallback(async () => {
    const res = await api.get('/api/applicant/application');
    if (res.ok) setView(res.data);
    else if (res.status === 404) setView(null);
    else setMessage({ text: res.data.message, kind: 'err' });
  }, []);

  useEffect(() => {
    api.get('/api/public/programs').then((res) => setPrograms(res.ok ? res.data : []));
    load();
  }, [load]);

  function saved(newView, text) {
    setView(newView);
    setEditing(false);
    setMessage({ text, kind: 'ok' });
  }

  async function withdraw() {
    // UC-5 step 3: ask first; nothing changes on Cancel.
    if (!window.confirm('Withdraw your application? This cannot be undone.')) return;
    const res = await api.post('/api/applicant/application/withdraw');
    if (res.ok) saved(res.data, 'Your application was withdrawn.');
    else setMessage({ text: res.data.message, kind: 'err' });
  }

  if (view === undefined) return <p className="empty">Loading…</p>;

  return (
    <>
      <h1>My application</h1>
      <Message {...message} />
      {view === null && (
        <>
          <Message text="You have not applied yet. Fill in the form below to start your application." />
          <ApplicationForm programs={programs} onSaved={(v) => saved(v, 'Your application was submitted.')} />
        </>
      )}
      {view && editing && (
        <ApplicationForm current={view.application} onSaved={(v) => saved(v, 'Your changes were saved.')}
                         onCancel={() => setEditing(false)} />
      )}
      {view && !editing && <StatusView view={view} onEdit={() => { setMessage({}); setEditing(true); }} onWithdraw={withdraw} />}
    </>
  );
}

/** One form for a new application (UC-3) and for editing one (UC-4, when `current` is given). */
function ApplicationForm({ programs = [], current, onSaved, onCancel }) {
  const editMode = Boolean(current);
  const [fields, setFields] = useState(() =>
    Object.fromEntries(TEXT_FIELDS.map((f) => [f, current ? String(current[f]) : ''])));
  const [programId, setProgramId] = useState('');
  const [files, setFiles] = useState({});
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState(null);
  const [sending, setSending] = useState(false);

  const program = programs.find((p) => String(p.id) === programId);
  const anyOpen = programs.some((p) => p.open);

  function chooseFile(field, file) {
    const problem = fileProblem(file);
    setErrors({ ...errors, [field]: problem });
    setFiles({ ...files, [field]: problem ? undefined : file });
  }

  // UC-3 step 6: Submit stays disabled until everything required is there.
  const complete = TEXT_FIELDS.every((f) => fields[f].trim() !== '')
    && (editMode || (programId !== '' && DOCUMENTS.every((d) => files[d.field])));

  async function submit(e) {
    e.preventDefault();
    const data = new FormData();
    TEXT_FIELDS.forEach((f) => data.append(f, fields[f]));
    if (!editMode) data.append('programId', programId);
    DOCUMENTS.forEach((d) => files[d.field] && data.append(d.field, files[d.field]));
    setSending(true);
    const res = await api.post(editMode ? '/api/applicant/application/edit' : '/api/applicant/application', data);
    setSending(false);
    if (res.ok) {
      onSaved(res.data);
    } else {
      setErrors(res.data.fieldErrors || {});
      setMessage(res.data.message);
    }
  }

  const set = (f) => (e) => setFields({ ...fields, [f]: e.target.value });

  return (
    <section className="card">
      <h2>{editMode ? 'Edit your application' : 'Apply to a program'}</h2>
      {!editMode && (
        <p className="lead-text">
          You can submit one application. Fill in every field and upload the three documents as PDF files of at most
          15 MB each.
        </p>
      )}
      <form onSubmit={submit} noValidate>
        {!editMode && (
          <>
            <Field label="Program" as="select" value={programId} onChange={(e) => setProgramId(e.target.value)} error={errors.programId}>
              <option value="">Choose a program</option>
              {/* Closed programs stay listed so the applicant sees why they cannot apply (UC-3 step 3). */}
              {programs.map((p) => (
                <option key={p.id} value={p.id} disabled={!p.open}>{programLabel(p)}</option>
              ))}
            </Field>
            {program && <div className="hint">Application deadline: {fmtDate(program.deadline)}</div>}
            {!anyOpen && <Message text="No program is accepting applications right now." kind="warn" />}
          </>
        )}

        <h3>Contact information</h3>
        <Field label="Phone number" type="tel" autoComplete="tel" value={fields.phone} onChange={set('phone')} error={errors.phone} />
        <Field label="Address" autoComplete="street-address" value={fields.address} onChange={set('address')} error={errors.address} />

        <h3>Educational background</h3>
        <Field label="Last school attended" value={fields.lastSchool} onChange={set('lastSchool')} error={errors.lastSchool} />
        <div className="grid-2">
          <Field label="Graduation year" type="number" min="1950" max="2100" value={fields.graduationYear}
                 onChange={set('graduationYear')} error={errors.graduationYear} />
          <Field label="Grade average" type="number" step="0.01" min="0" max="100" value={fields.gradeAverage}
                 onChange={set('gradeAverage')} error={errors.gradeAverage} />
        </div>

        <h3>Documents <span className="hint">(PDF, at most 15 MB each)</span></h3>
        {editMode && <p className="hint">Choose a file only if you want to replace the one you uploaded.</p>}
        {DOCUMENTS.map((d) => (
          <Field key={d.field} label={d.label} type="file" accept="application/pdf,.pdf" error={errors[d.field]}
                 onChange={(e) => chooseFile(d.field, e.target.files[0])} />
        ))}

        <div className="actions">
          <button type="submit" disabled={!complete || sending}>{editMode ? 'Save changes' : 'Submit application'}</button>
          {onCancel && <button type="button" className="secondary" onClick={onCancel}>Cancel</button>}
          {!complete && <span className="hint">The button becomes available once everything is filled in.</span>}
        </div>
        <Message text={message} kind="err" />
      </form>
    </section>
  );
}

function programLabel(p) {
  if (p.open) return `${p.name} (deadline ${fmtDate(p.deadline)})`;
  return p.deadline ? `${p.name} (closed, deadline was ${fmtDate(p.deadline)})` : `${p.name} (not open for applications yet)`;
}

function StatusView({ view, onEdit, onWithdraw }) {
  const a = view.application;
  // UC-4 step 3: say why editing is no longer possible once the deadline has passed.
  const deadlinePassed = a.status === 'SUBMITTED' && !a.canEdit;
  return (
    <>
      <section className="card">
        <h2>Status</h2>
        <p><StatusBadge status={a.status} /></p>
        {a.condition && <Message text={`Condition of your offer: ${a.condition}`} kind="warn" />}
        <dl className="details">
          <dt>Program</dt><dd>{a.program.name}</dd>
          <dt>Submitted</dt><dd>{fmtDateTime(a.submittedAt)}</dd>
          <dt>Phone</dt><dd>{a.phone}</dd>
          <dt>Address</dt><dd>{a.address}</dd>
          <dt>Last school</dt><dd>{a.lastSchool} ({a.graduationYear})</dd>
          <dt>Grade average</dt><dd>{a.gradeAverage}</dd>
        </dl>
        {deadlinePassed && (
          <p className="hint">The application deadline for {a.program.name} has passed, so the application can no longer be edited.</p>
        )}
        <div className="actions">
          {a.canEdit && <button type="button" className="secondary" onClick={onEdit}>Edit application</button>}
          {a.canWithdraw && <button type="button" className="danger" onClick={onWithdraw}>Withdraw</button>}
        </div>
      </section>
      <div className="grid-2">
        <section className="card">
          <h2>Documents</h2>
          <ul>
            {a.documents.map((d) => (
              <li key={d.type}>{DOC_LABELS[d.type]}: {d.fileName} ({fmtSize(d.sizeBytes)})</li>
            ))}
          </ul>
        </section>
        <section className="card">
          <h2>Decision emails</h2>
          {view.decisionEmails.length === 0 && <p className="empty">No decision emails yet.</p>}
          {view.decisionEmails.map((m) => (
            <div key={m.id}>
              <h3>{m.subject}</h3>
              <p className="hint">{fmtDateTime(m.sentAt)}</p>
              <p style={{ whiteSpace: 'pre-line' }}>{m.body}</p>
            </div>
          ))}
        </section>
      </div>
      <section className="card">
        <h2>History</h2>
        <div className="table-wrap">
          <table>
            <thead><tr><th scope="col">Date</th><th scope="col">Status</th><th scope="col">Changed by</th></tr></thead>
            <tbody>
              {a.decisionLog.map((c, i) => (
                <tr key={i}><td>{fmtDateTime(c.changedAt)}</td><td><StatusBadge status={c.newStatus} /></td><td>{c.changedBy}</td></tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </>
  );
}
