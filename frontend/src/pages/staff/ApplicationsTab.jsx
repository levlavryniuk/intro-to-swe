import { useCallback, useEffect, useState } from 'react';
import { api } from '../../api.js';
import { Field, Message, StatusBadge } from '../../components.jsx';
import { DOC_LABELS, STAFF_STATUSES, STATUS_LABELS, fmtDateTime, fmtSize } from '../../format.js';

/** UC-8 Review Application and UC-9 Record Decision (UC-15 runs on the server when an applicant is accepted). */
export default function ApplicationsTab({ programs, onEmail }) {
  const [programId, setProgramId] = useState('');
  const [status, setStatus] = useState('');
  const [list, setList] = useState(null);
  const [selected, setSelected] = useState(null);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    const params = new URLSearchParams();
    if (programId) params.set('programId', programId);
    if (status) params.set('status', status);
    const res = await api.get(`/api/staff/applications?${params}`);
    if (res.ok) setList(res.data);
    else setError(res.data.message);
  }, [programId, status]);

  useEffect(() => {
    load();
  }, [load]);

  async function open(id) {
    const res = await api.get(`/api/staff/applications/${id}`);
    if (res.ok) setSelected(res.data);
    else setError(res.data.message);
  }

  return (
    <>
      <h1>Applications</h1>
      <div className="filters">
        <div>
          <label htmlFor="app-program">Program</label>
          <select id="app-program" value={programId} onChange={(e) => setProgramId(e.target.value)}>
            <option value="">All programs</option>
            {programs.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
          </select>
        </div>
        <div>
          <label htmlFor="app-status">Status</label>
          <select id="app-status" value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">All statuses</option>
            {Object.entries(STATUS_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select>
        </div>
      </div>
      <Message text={error} kind="err" />
      {list === null && <p className="empty">Loading…</p>}
      {list?.length === 0 && <p className="empty">No applications match these filters.</p>}
      {list?.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr><th scope="col">Submitted</th><th scope="col">Applicant</th><th scope="col">Program</th><th scope="col">Status</th></tr>
            </thead>
            <tbody>
              {list.map((a) => (
                <tr key={a.id} tabIndex={0} className={`clickable${selected?.id === a.id ? ' selected' : ''}`}
                    onClick={() => open(a.id)} onKeyDown={(e) => e.key === 'Enter' && open(a.id)}>
                  <td>{fmtDateTime(a.submittedAt)}</td>
                  <td>{a.applicantName}<br /><span className="hint">{a.applicantEmail}</span></td>
                  <td>{a.programName}</td>
                  <td><StatusBadge status={a.status} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {selected && (
        <ReviewPanel key={selected.id} application={selected} onEmail={onEmail}
                     onDecided={(updated) => { setSelected(updated); load(); }} />
      )}
    </>
  );
}

function ReviewPanel({ application: a, onEmail, onDecided }) {
  const [status, setStatus] = useState(STAFF_STATUSES.includes(a.status) ? a.status : 'UNDER_REVIEW');
  const [condition, setCondition] = useState(a.condition || '');
  const [message, setMessage] = useState({});
  const [warning, setWarning] = useState(null);
  const withdrawn = a.status === 'WITHDRAWN';

  async function save(e) {
    e.preventDefault();
    const res = await api.post(`/api/staff/applications/${a.id}/status`, {
      status,
      condition: status === 'CONDITIONAL_OFFER' ? condition : null,
    });
    if (!res.ok) {
      setMessage({ text: res.data.message, kind: 'err' });
      return;
    }
    const email = res.data.email;
    // UC-9 step 6: the status stays saved even when the email fails; staff are told either way.
    setMessage(email.status === 'SENT'
      ? { text: 'Status saved. The applicant was emailed.', kind: 'ok' }
      : { text: `Status saved, but the email to the applicant failed: ${email.failureReason || 'unknown error'}`, kind: 'err' });
    setWarning(res.data.sisWarning);
    onDecided(res.data.application);
  }

  const conditionError = message.kind === 'err' && status === 'CONDITIONAL_OFFER' && /condition/i.test(message.text)
    ? message.text : undefined;

  return (
    <div style={{ marginTop: 24 }}>
      <section className="card">
        <h2>{a.applicantName} <StatusBadge status={a.status} /></h2>
        <dl className="details">
          <dt>Email</dt><dd>{a.applicantEmail}</dd>
          <dt>Program</dt><dd>{a.program.name}</dd>
          {a.condition && <><dt>Condition</dt><dd>{a.condition}</dd></>}
          <dt>Phone</dt><dd>{a.phone}</dd>
          <dt>Address</dt><dd>{a.address}</dd>
          <dt>Last school</dt><dd>{a.lastSchool}, graduated {a.graduationYear}</dd>
          <dt>Grade average</dt><dd>{a.gradeAverage}</dd>
          <dt>Submitted</dt><dd>{fmtDateTime(a.submittedAt)}</dd>
          {(a.status === 'ACCEPTED' || a.status === 'ENROLLED') && (
            <><dt>SIS transfer</dt><dd>{a.sisTransferDone ? 'Done' : 'Pending'}</dd></>
          )}
        </dl>
      </section>
      <div className="grid-2">
        <section className="card">
          <h2>Documents</h2>
          <ul>
            {a.documents.map((d) => (
              <li key={d.type}>
                <a href={`/api/staff/applications/${a.id}/documents/${d.type}`} target="_blank" rel="noopener noreferrer">
                  {DOC_LABELS[d.type]}
                </a>{' '}
                <span className="hint">{d.fileName}, {fmtSize(d.sizeBytes)}</span>
              </li>
            ))}
          </ul>
          <h2 style={{ marginTop: 20 }}>Decision log</h2>
          <div className="table-wrap">
            <table>
              <thead><tr><th scope="col">When</th><th scope="col">From</th><th scope="col">To</th><th scope="col">By</th></tr></thead>
              <tbody>
                {a.decisionLog.map((c, i) => (
                  <tr key={i}>
                    <td>{fmtDateTime(c.changedAt)}</td>
                    <td>{c.oldStatus ? <StatusBadge status={c.oldStatus} /> : '-'}</td>
                    <td><StatusBadge status={c.newStatus} /></td>
                    <td>{c.changedBy}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
        <section className="card">
          <h2>Record decision</h2>
          {withdrawn ? (
            <Message text="The applicant withdrew this application. It is closed." kind="warn" />
          ) : (
            <form onSubmit={save} noValidate>
              <Field label="New status" as="select" value={status} onChange={(e) => setStatus(e.target.value)}>
                {STAFF_STATUSES.map((s) => <option key={s} value={s}>{STATUS_LABELS[s]}</option>)}
              </Field>
              {status === 'CONDITIONAL_OFFER' && (
                <Field label="Condition of the offer" as="textarea" maxLength={1000} value={condition}
                       onChange={(e) => setCondition(e.target.value)} error={conditionError} />
              )}
              <p className="hint">The applicant is emailed automatically.</p>
              <div className="actions"><button type="submit">Save status</button></div>
              <Message {...message} />
              <Message text={warning} kind="warn" />
            </form>
          )}
          <div className="actions">
            <button type="button" className="secondary"
                    onClick={() => onEmail({ kind: 'application', id: a.id, label: `${a.applicantName} <${a.applicantEmail}>` })}>
              Email applicant
            </button>
          </div>
        </section>
      </div>
    </div>
  );
}
