import { useEffect, useState } from 'react';
import { api } from '../api.js';
import { Checkbox, Field, Message, resultMessage } from '../components.jsx';
import { fmtDateTime } from '../format.js';

/** Public start page: UC-1 Submit Inquiry and UC-2 Register for Event. No account needed. */
export default function HomePage() {
  const [programs, setPrograms] = useState([]);
  const [events, setEvents] = useState(null);

  useEffect(() => {
    api.get('/api/public/programs').then((res) => setPrograms(res.ok ? res.data : []));
    api.get('/api/public/events').then((res) => setEvents(res.ok ? res.data : []));
  }, []);

  return (
    <>
      <h1>Study at American University Kyiv</h1>
      <p className="lead-text">Ask the admissions office a question or sign up for an open day or webinar. No account needed.</p>
      <div className="grid-2">
        <section className="card" aria-labelledby="inquiry-title">
          <h2 id="inquiry-title">Send an inquiry</h2>
          <InquiryForm programs={programs} />
        </section>
        <section className="card" aria-labelledby="events-title">
          <h2 id="events-title">Upcoming events</h2>
          {events === null && <p className="empty">Loading events…</p>}
          {events?.length === 0 && <p className="empty">There are no upcoming events right now. Check back soon.</p>}
          {events?.map((event) => <EventItem key={event.id} event={event} />)}
        </section>
      </div>
    </>
  );
}

const EMPTY_INQUIRY = { name: '', email: '', programId: '', question: '', promotionalOptIn: false };

function InquiryForm({ programs }) {
  const [form, setForm] = useState(EMPTY_INQUIRY);
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState({});
  const set = (field) => (e) => setForm({ ...form, [field]: e.target.type === 'checkbox' ? e.target.checked : e.target.value });

  async function submit(e) {
    e.preventDefault();
    const res = await api.post('/api/public/inquiries', {
      ...form,
      programId: form.programId ? Number(form.programId) : null,
    });
    // On an error the typed values stay in the form (UC-1 step 4).
    setErrors(res.ok ? {} : res.data.fieldErrors || {});
    setMessage(resultMessage(res));
    if (res.ok) setForm(EMPTY_INQUIRY);
  }

  return (
    <form onSubmit={submit} noValidate>
      <Field label="Name" value={form.name} onChange={set('name')} autoComplete="name" error={errors.name} />
      <Field label="Email address" type="email" value={form.email} onChange={set('email')} autoComplete="email" error={errors.email} />
      <Field label="Program of interest" as="select" value={form.programId} onChange={set('programId')} error={errors.programId}>
        <option value="">Choose a program</option>
        {programs.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
      </Field>
      <Field label="Your question" hint="(optional)" as="textarea" maxLength={2000} value={form.question} onChange={set('question')} error={errors.question} />
      <Checkbox label="Send me news and promotional email from AUK" checked={form.promotionalOptIn} onChange={set('promotionalOptIn')} />
      <div className="actions"><button type="submit">Send inquiry</button></div>
      <Message {...message} />
    </form>
  );
}

const EMPTY_REGISTRATION = { name: '', email: '', promotionalOptIn: false };

function EventItem({ event }) {
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState(EMPTY_REGISTRATION);
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState({});
  const set = (field) => (e) => setForm({ ...form, [field]: e.target.type === 'checkbox' ? e.target.checked : e.target.value });

  async function submit(e) {
    e.preventDefault();
    const res = await api.post(`/api/public/events/${event.id}/registrations`, form);
    setErrors(res.ok ? {} : res.data.fieldErrors || {});
    setMessage(resultMessage(res));
    if (res.ok) setForm(EMPTY_REGISTRATION);
  }

  return (
    <div className="event">
      <h3>{event.name}</h3>
      <div className="when">{fmtDateTime(event.startsAt)} · {event.location || 'Online'}</div>
      <div className="actions">
        <button className="secondary small" type="button" aria-expanded={open} onClick={() => setOpen(!open)}>
          Register for this event
        </button>
      </div>
      {open && (
        <form onSubmit={submit} noValidate>
          <Field label="Name" value={form.name} onChange={set('name')} autoComplete="name" error={errors.name} />
          <Field label="Email address" type="email" value={form.email} onChange={set('email')} autoComplete="email" error={errors.email} />
          <Checkbox label="Send me news and promotional email from AUK" checked={form.promotionalOptIn} onChange={set('promotionalOptIn')} />
          <div className="actions"><button type="submit">Register</button></div>
          <Message {...message} />
        </form>
      )}
    </div>
  );
}
