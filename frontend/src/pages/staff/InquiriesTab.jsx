import { useCallback, useEffect, useState } from 'react';
import { api } from '../../api.js';
import { Message, resultMessage } from '../../components.jsx';
import { fmtDateTime } from '../../format.js';

/** UC-7 Manage Inquiries and UC-11 Remove Lead Opt-in. */
export default function InquiriesTab({ programs, onEmail }) {
  const [programId, setProgramId] = useState('');
  const [inquiries, setInquiries] = useState(null);
  const [message, setMessage] = useState({});

  const load = useCallback(async () => {
    const res = await api.get(`/api/staff/inquiries${programId ? `?programId=${programId}` : ''}`);
    if (res.ok) setInquiries(res.data);
    else setMessage({ text: res.data.message, kind: 'err' });
  }, [programId]);

  useEffect(() => {
    load();
  }, [load]);

  async function rate(id, interest) {
    if (!interest) return;
    const res = await api.put(`/api/staff/inquiries/${id}/interest`, { interest });
    setMessage(resultMessage(res, 'Interest saved.'));
    if (res.ok) load();
  }

  async function removeOptIn(leadId) {
    const res = await api.post(`/api/staff/leads/${leadId}/remove-opt-in`);
    setMessage({ text: res.data.message, kind: res.ok ? (res.data.changed ? 'ok' : 'warn') : 'err' });
    load();
  }

  return (
    <>
      <h1>Inquiries</h1>
      <div className="filters">
        <div>
          <label htmlFor="inq-program">Program</label>
          <select id="inq-program" value={programId} onChange={(e) => setProgramId(e.target.value)}>
            <option value="">All programs</option>
            {programs.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
          </select>
        </div>
      </div>
      <Message {...message} />
      {inquiries === null && <p className="empty">Loading…</p>}
      {inquiries?.length === 0 && <p className="empty">No inquiries match this filter.</p>}
      {inquiries?.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th scope="col">Received</th><th scope="col">Lead</th><th scope="col">Program</th><th scope="col">Question</th>
                <th scope="col">Promo email</th><th scope="col">Interest</th><th scope="col">Actions</th>
              </tr>
            </thead>
            <tbody>
              {inquiries.map((i) => (
                <tr key={i.id}>
                  <td>{fmtDateTime(i.submittedAt)}</td>
                  <td>{i.name}<br /><span className="hint">{i.email}</span></td>
                  <td>{i.programName}</td>
                  <td>{i.question || <span className="hint">No question</span>}</td>
                  <td>{i.promotionalOptIn ? 'Yes' : 'No'}</td>
                  <td>
                    <label className="sr-only" htmlFor={`interest-${i.id}`}>Interest of {i.name}</label>
                    <select id={`interest-${i.id}`} value={i.interest || ''} onChange={(e) => rate(i.id, e.target.value)}>
                      <option value="">Not rated</option>
                      <option value="HIGH">High</option>
                      <option value="MEDIUM">Medium</option>
                      <option value="LOW">Low</option>
                    </select>
                  </td>
                  <td>
                    <button type="button" className="small secondary"
                            onClick={() => onEmail({ kind: 'lead', id: i.leadId, label: `${i.name} <${i.email}>` })}>Email</button>{' '}
                    {i.promotionalOptIn && (
                      <button type="button" className="small secondary" onClick={() => removeOptIn(i.leadId)}>Remove opt-in</button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}
