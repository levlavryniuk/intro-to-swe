import { useEffect, useState } from 'react';
import { api } from '../../api.js';
import { Message } from '../../components.jsx';
import { STAGE_LABELS, STATUS_LABELS } from '../../format.js';

/** UC-14 View Pipeline Overview. */
export default function OverviewTab() {
  const [overview, setOverview] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    api.get('/api/staff/overview').then((res) => (res.ok ? setOverview(res.data) : setError(res.data.message)));
  }, []);

  if (error) return <Message text={error} kind="err" />;
  if (!overview) return <p className="empty">Loading…</p>;

  return (
    <>
      <h1>Pipeline overview</h1>
      <div className="stats">
        <Stat value={overview.inquiries} label="Inquiries" />
        <Stat value={overview.eventRegistrations} label="Event registrations" />
      </div>
      <h2 style={{ marginTop: 24 }}>Applications by stage</h2>
      <div className="stats">
        {overview.stages.map((s) => (
          // Rejected and withdrawn applications are shown as closed, outside the three stages (UC-14 step 4).
          <div key={s.stage} className={`stat${s.stage === 'CLOSED' ? ' closed' : ''}`}>
            <div className="num">{s.total}</div>
            <div className="lbl">{STAGE_LABELS[s.stage]}</div>
            <ul>
              {Object.entries(s.statuses).map(([status, n]) => (
                <li key={status}><span>{STATUS_LABELS[status]}</span><strong>{n}</strong></li>
              ))}
            </ul>
          </div>
        ))}
      </div>
    </>
  );
}

function Stat({ value, label }) {
  return (
    <div className="stat">
      <div className="num">{value}</div>
      <div className="lbl">{label}</div>
    </div>
  );
}
