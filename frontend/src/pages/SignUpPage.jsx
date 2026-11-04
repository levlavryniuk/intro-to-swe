import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { Field, Message } from '../components.jsx';

const EMPTY = { name: '', email: '', password: '' };

/** Applicants create their own account (local sign-in until the Core Platform provides one). */
export default function SignUpPage() {
  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState(null);
  const [done, setDone] = useState(false);
  const set = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  async function submit(e) {
    e.preventDefault();
    const res = await api.post('/api/public/accounts', form);
    setErrors(res.ok ? {} : res.data.fieldErrors || {});
    setMessage(res.ok ? null : res.data.message);
    if (res.ok) {
      setForm(EMPTY);
      setDone(true);
    }
  }

  return (
    <section className="card narrow">
      <h1>Create an applicant account</h1>
      <p className="lead-text">
        You need an account to submit and follow your application. Later this sign-in will come from the shared
        university platform.
      </p>
      {done ? (
        <div className="msg ok" role="status">
          Your account is ready. <Link to="/login" state={{ signedUp: true }}>Sign in</Link> to start your application.
        </div>
      ) : (
        <form onSubmit={submit} noValidate>
          <Field label="Full name" value={form.name} onChange={set('name')} autoComplete="name" error={errors.name} />
          <Field label="Email address" type="email" value={form.email} onChange={set('email')} autoComplete="email" error={errors.email} />
          <Field label="Password" hint="(at least 8 characters)" type="password" value={form.password} onChange={set('password')}
                 autoComplete="new-password" error={errors.password} />
          <div className="actions"><button type="submit">Create account</button></div>
          <Message text={message} kind="err" />
        </form>
      )}
    </section>
  );
}
