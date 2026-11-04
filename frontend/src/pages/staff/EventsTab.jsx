import { useState } from 'react';
import { api } from '../../api.js';
import { Field, Message, resultMessage } from '../../components.jsx';
import { fmtDateTime } from '../../format.js';

const EMPTY = { name: '', startsAt: '', location: '', onlineLink: '' };

/** UC-12 Manage Events, with UC-11 Remove Lead Opt-in from a registration list. */
export default function EventsTab({ events, reloadEvents, onEmail }) {
  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState({});
  const [selected, setSelected] = useState(null);
  const [registrations, setRegistrations] = useState(null);
  const [listMessage, setListMessage] = useState({});
  const set = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  async function create(e) {
    e.preventDefault();
    const res = await api.post('/api/staff/events', {
      ...form,
      // datetime-local gives "2026-11-01T10:00"; the API expects seconds too.
      startsAt: form.startsAt ? (form.startsAt.length === 16 ? `${form.startsAt}:00` : form.startsAt) : null,
    });
    setErrors(res.ok ? {} : res.data.fieldErrors || {});
    setMessage(resultMessage(res, 'Event created. It now appears on the public events page.'));
    if (res.ok) {
      setForm(EMPTY);
      reloadEvents();
    }
  }

  async function showRegistrations(event) {
    setSelected(event);
    setListMessage({});
    const res = await api.get(`/api/staff/events/${event.id}/registrations`);
    setRegistrations(res.ok ? res.data : []);
  }

  async function removeOptIn(leadId) {
    const res = await api.post(`/api/staff/leads/${leadId}/remove-opt-in`);
    setListMessage({ text: res.data.message, kind: res.ok ? (res.data.changed ? 'ok' : 'warn') : 'err' });
    showRegistrations(selected);
  }

  return (
    <>
      <h1>Events</h1>
      <div className="grid-2">
        <section className="card">
          <h2>Create event</h2>
          <form onSubmit={create} noValidate>
            <Field label="Name" value={form.name} onChange={set('name')} error={errors.name} />
            <Field label="Date and time" type="datetime-local" value={form.startsAt} onChange={set('startsAt')} error={errors.startsAt} />
            <Field label="Location" value={form.location} onChange={set('location')} error={errors.location} />
            <Field label="Or online link" type="url" placeholder="https://" value={form.onlineLink} onChange={set('onlineLink')}
                   error={errors.onlineLink} />
            <div className="actions"><button type="submit">Create event</button></div>
            <Message {...message} />
          </form>
        </section>
        <section className="card">
          <h2>All events</h2>
          {events.length === 0 && <p className="empty">No events yet.</p>}
          {events.length > 0 && (
            <div className="table-wrap">
              <table>
                <thead><tr><th scope="col">When</th><th scope="col">Event</th><th scope="col">Registered</th></tr></thead>
                <tbody>
                  {events.map((ev) => (
                    <tr key={ev.id} tabIndex={0} className={`clickable${selected?.id === ev.id ? ' selected' : ''}`}
                        onClick={() => showRegistrations(ev)} onKeyDown={(e) => e.key === 'Enter' && showRegistrations(ev)}>
                      <td>{fmtDateTime(ev.startsAt)}</td>
                      <td>{ev.name}<br /><span className="hint">{ev.location || ev.onlineLink}</span></td>
                      <td>{ev.registrations}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </div>
      {selected && registrations && (
        <section className="card">
          <h2>Registrations: {selected.name}</h2>
          <Message {...listMessage} />
          {registrations.length === 0 && <p className="empty">Nobody has registered yet.</p>}
          {registrations.length > 0 && (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr><th scope="col">Registered</th><th scope="col">Name</th><th scope="col">Email</th><th scope="col">Promo email</th><th scope="col">Actions</th></tr>
                </thead>
                <tbody>
                  {registrations.map((r) => (
                    <tr key={r.id}>
                      <td>{fmtDateTime(r.registeredAt)}</td>
                      <td>{r.name}</td>
                      <td>{r.email}</td>
                      <td>{r.promotionalOptIn ? 'Yes' : 'No'}</td>
                      <td>
                        <button type="button" className="small secondary"
                                onClick={() => onEmail({ kind: 'lead', id: r.leadId, label: `${r.name} <${r.email}>` })}>Email</button>{' '}
                        {r.promotionalOptIn && (
                          <button type="button" className="small secondary" onClick={() => removeOptIn(r.leadId)}>Remove opt-in</button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      )}
    </>
  );
}
