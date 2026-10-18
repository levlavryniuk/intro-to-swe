import { Link, Route, Routes, useNavigate } from 'react-router-dom';
import { RequireRole, useAuth } from './auth.jsx';
import ApplicantPage from './pages/ApplicantPage.jsx';
import HomePage from './pages/HomePage.jsx';
import SignInPage from './pages/SignInPage.jsx';
import SignUpPage from './pages/SignUpPage.jsx';
import StaffPage from './pages/StaffPage.jsx';

// Keep these paths in sync with SpaController.ROUTES on the server.
export default function App() {
  return (
    <>
      <TopBar />
      <main>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/signup" element={<SignUpPage />} />
          <Route path="/login" element={<SignInPage />} />
          <Route path="/applicant" element={<RequireRole role="APPLICANT"><ApplicantPage /></RequireRole>} />
          <Route path="/staff" element={<RequireRole role="STAFF"><StaffPage /></RequireRole>} />
          <Route path="*" element={<div className="card"><h1>Page not found</h1><Link to="/">Back to the start page</Link></div>} />
        </Routes>
      </main>
    </>
  );
}

function TopBar() {
  const { me, signOut } = useAuth();
  const navigate = useNavigate();

  async function handleSignOut() {
    await signOut();
    navigate('/');
  }

  return (
    <header className="topbar">
      <Link className="brand" to={me?.role === 'STAFF' ? '/staff' : '/'}>
        AUK Admissions{me?.role === 'STAFF' ? ' · Staff' : ''}
      </Link>
      <nav aria-label="Main">
        {me?.authenticated ? (
          <>
            {me.role === 'APPLICANT' && <Link to="/applicant">My application</Link>}
            <span className="who">Signed in as {me.name}</span>
            <button className="link" type="button" onClick={handleSignOut}>Sign out</button>
          </>
        ) : (
          <>
            <Link to="/applicant">My application</Link>
            <Link to="/signup">Create applicant account</Link>
            <Link to="/login">Sign in</Link>
          </>
        )}
      </nav>
    </header>
  );
}
