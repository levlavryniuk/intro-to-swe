import { useCallback, useEffect, useState } from 'react';
import { api } from '../../api.js';
import { Field, Message, StatusBadge } from '../../components.jsx';
import { fmtDate, fmtDateTime } from '../../format.js';

/** UC-10 Send Email: to one person (chosen on another tab) or to a group of opted-in leads. */
export default function EmailTab({ programs, events, target, onClearTarget }) {
  const [mode, setMode] = useState(target ? 'one' : 'group');
  const [group, setGroup] = useState({ programId: '', eventId: '' });
  const [count, setCount] = useState(null);
  const [subject, setSubject] = useState('');
  const [body, setBody] = useState('');
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState({});
  const [results, setResults] = useState(null);
  const [recent, setRecent] = useState([]);

  const loadRecent = useCallback(async () => {
    const res = await api.get('/api/staff/emails');
    setRecent(res.ok ? res.data : []);
  }, []);

  useEffect(() => {
    loadRecent();
  }, [loadRecent]);

  // UC-10 step 3: show how many opted-in leads the group email will reach.
  useEffect(() => {
    if (!group.programId && !group.eventId) {
      setCount(null);
      return;
    }
    const query = group.programId ? `programId=${group.programId}` : `eventId=${group.eventId}`;
    api.get(`/api/staff/emails/recipients?${query}`).then((res) => setCount(res.ok ? res.data.count : null));
  }, [group]);

  async function send(e) {
    e.preventDefault();
    let res;
    if (mode === 'one') {
      if (!target) {
        setMessage({ text: 'Choose a person first with an Email button on the Inquiries, Applications or Events tab.', kind: 'err' });
        return;
      }
      const url = target.kind === 'lead' ? `/api/staff/leads/${target.id}/email` : `/api/staff/applications/${target.id}/email`;
      res = await api.post(url, { subject, body });
    } else {
      res = await api.post('/api/staff/emails/group', {
        programId: group.programId ? Number(group.programId) : null,
        eventId: group.eventId ? Number(group.eventId) : null,
        subject,
        body,
      });
    }
    setErrors(res.ok ? {} : res.data.fieldErrors || {});
    if (!res.ok) {
      setMessage({ text: res.data.message, kind: 'err' });
      return;
    }
    if (mode === 'one') {
      setResults(null);
      setMessage(res.data.status === 'SENT'
        ? { text: `Email sent to ${res.data.recipientEmail}.`, kind: 'ok' }
        : { text: `The email failed: ${res.data.failureReason || 'unknown error'}`, kind: 'err' });
    } else {
      setResults(res.data.results);
      setMessage({ text: `${res.data.sent} sent, ${res.data.failed} failed.`, kind: res.data.failed ? 'warn' : 'ok' });
    }
    setSubject('');
    setBody('');
    loadRecent();
  }

  return (
    <>
      <h1>Send email</h1>
      <section className="card">
        <form onSubmit={send} noValidate>
          <fieldset style={{ border: 'none', padding: 0, margin: 0 }}>
            <legend className="hint">Recipients</legend>
            <label className="inline">
              <input type="radio" name="mode" checked={mode === 'one'} onChange={() => setMode('one')} /> One person
            </label>
            <label className="inline">
              <input type="radio" name="mode" checked={mode === 'group'} onChange={() => setMode('group')} /> Group of leads
            </label>
          </fieldset>

          {mode === 'one' && (
            <p>
              To: <strong>{target ? target.label : 'Choose a person with the Email button on the Inquiries, Applications or Events tab.'}</strong>{' '}
              {target && <button type="button" className="link" onClick={onClearTarget}>clear</button>}
            </p>
          )}
          {mode === 'group' && (
            <>
              <div className="grid-2">
                <Field label="Everyone interested in a program" as="select" value={group.programId}
                       onChange={(e) => setGroup({ programId: e.target.value, eventId: '' })}>
                  <option value="">-</option>
                  {programs.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
                </Field>
                <Field label="Or everyone registered for an event" as="select" value={group.eventId}
                       onChange={(e) => setGroup({ programId: '', eventId: e.target.value })}>
                  <option value="">-</option>
                  {events.map((ev) => <option key={ev.id} value={ev.id}>{`${ev.name} (${fmtDate(ev.startsAt)})`}</option>)}
                </Field>
              </div>
              {count === 0 && <Message text="Nobody in this group has opted in to promotional email. Choose another group." kind="warn" />}
              {count > 0 && (
                <Message text={`${count} ${count === 1 ? 'person' : 'people'} will get this email. Leads who did not opt in are left out.`} />
              )}
            </>
          )}

          <Field label="Subject" maxLength={200} value={subject} onChange={(e) => setSubject(e.target.value)} error={errors.subject} />
          <Field label="Message" as="textarea" style={{ minHeight: 160 }} value={body} onChange={(e) => setBody(e.target.value)} error={errors.body} />
          <div className="actions"><button type="submit">Send</button></div>
          <Message {...message} />
        </form>
        {results && (
          <div className="table-wrap">
            <table>
              <thead><tr><th scope="col">To</th><th scope="col">Result</th></tr></thead>
              <tbody>
                {results.map((m) => (
                  <tr key={m.id}>
                    <td>{m.recipientEmail}</td>
                    <td><StatusBadge status={m.status} /> {m.failureReason && <span className="hint">{m.failureReason}</span>}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
      <section className="card">
        <h2>Recently sent</h2>
        {recent.length === 0 && <p className="empty">No emails sent yet.</p>}
        {recent.length > 0 && (
          <div className="table-wrap">
            <table>
              <thead><tr><th scope="col">Sent</th><th scope="col">To</th><th scope="col">Subject</th><th scope="col">Result</th></tr></thead>
              <tbody>
                {recent.map((m) => (
                  <tr key={m.id}>
                    <td>{fmtDateTime(m.sentAt)}</td>
                    <td>{m.recipientEmail}</td>
                    <td>{m.subject} {m.decisionEmail && <span className="hint">(decision)</span>}</td>
                    <td><StatusBadge status={m.status} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </>
  );
}
