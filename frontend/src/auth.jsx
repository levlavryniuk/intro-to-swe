import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { Link, Navigate, useLocation } from 'react-router-dom';
import { api } from './api.js';

const AuthContext = createContext(null);

/** Knows who is signed in (local sign-in until the Core Platform of Team 2 provides one). */
export function AuthProvider({ children }) {
  const [me, setMe] = useState(null);

  const refresh = useCallback(async () => {
    const res = await api.get('/api/public/me');
    const current = res.ok ? res.data : { authenticated: false };
    setMe(current);
    return current;
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const signOut = useCallback(async () => {
    await api.signOut();
    await refresh();
  }, [refresh]);

  return <AuthContext.Provider value={{ me, refresh, signOut }}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}

/** Shows the page only to the given role; others are sent to sign in. The API enforces the same rule. */
export function RequireRole({ role, children }) {
  const { me } = useAuth();
  const location = useLocation();
  if (!me) {
    return <p className="empty">Loading…</p>;
  }
  if (!me.authenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  if (me.role !== role) {
    return (
      <div className="card">
        <h1>No access</h1>
        <p>This page is for {role === 'STAFF' ? 'admissions staff' : 'applicants'} only.</p>
        <Link to={me.role === 'STAFF' ? '/staff' : '/applicant'}>Go to your page</Link>
      </div>
    );
  }
  return children;
}
