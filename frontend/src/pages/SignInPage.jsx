import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { api } from '../api.js';
import { useAuth } from '../auth.jsx';
import { Field, Message } from '../components.jsx';

/** Sign-in for applicants and staff, with the email address they registered with. */
export default function SignInPage() {
  const { refresh } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);

  async function submit(e) {
    e.preventDefault();
    const res = await api.signIn(email, password);
    if (!res.ok) {
      setError(res.data.message);
      return;
    }
    await refresh();
    const home = res.data.role === 'STAFF' ? '/staff' : '/applicant';
    navigate(location.state?.from === home ? location.state.from : home, { replace: true });
  }

  return (
    <section className="card narrow">
      <h1>Sign in</h1>
      <p className="lead-text">For applicants and admissions staff. Use the email address you registered with.</p>
      {location.state?.signedUp && !error && <Message text="Your account is ready. Sign in to start your application." />}
      <form onSubmit={submit}>
        <Field label="Email address" type="email" value={email} onChange={(e) => setEmail(e.target.value)}
               autoComplete="username" required autoFocus />
        <Field label="Password" type="password" value={password} onChange={(e) => setPassword(e.target.value)}
               autoComplete="current-password" required />
        <div className="actions"><button type="submit">Sign in</button></div>
        <Message text={error} kind="err" />
      </form>
      <p>Don't have an account yet? <Link to="/signup">Create an applicant account</Link></p>
    </section>
  );
}
