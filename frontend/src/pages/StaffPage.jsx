import { useCallback, useEffect, useState } from 'react';
import { api } from '../api.js';
import ApplicationsTab from './staff/ApplicationsTab.jsx';
import EmailTab from './staff/EmailTab.jsx';
import EventsTab from './staff/EventsTab.jsx';
import InquiriesTab from './staff/InquiriesTab.jsx';
import OverviewTab from './staff/OverviewTab.jsx';
import ProgramsTab from './staff/ProgramsTab.jsx';

const TABS = [
  ['overview', 'Overview'],
  ['inquiries', 'Inquiries'],
  ['applications', 'Applications'],
  ['email', 'Email'],
  ['events', 'Events'],
  ['programs', 'Programs'],
];

/** Admissions staff: one tab per group of use cases (UC-7 to UC-14). */
export default function StaffPage() {
  const [tab, setTab] = useState('overview');
  const [programs, setPrograms] = useState([]);
  const [events, setEvents] = useState([]);
  // "One person" email target, chosen with an Email button on another tab (UC-7 step 5, UC-10).
  const [emailTarget, setEmailTarget] = useState(null);

  const loadPrograms = useCallback(async () => {
    const res = await api.get('/api/staff/programs');
    setPrograms(res.ok ? res.data : []);
  }, []);
  const loadEvents = useCallback(async () => {
    const res = await api.get('/api/staff/events');
    setEvents(res.ok ? res.data : []);
  }, []);

  useEffect(() => {
    loadPrograms();
    loadEvents();
  }, [loadPrograms, loadEvents]);

  function emailOne(target) {
    setEmailTarget(target);
    setTab('email');
  }

  return (
    <>
      <div className="tabs" role="tablist" aria-label="Staff sections">
        {TABS.map(([id, label]) => (
          <button key={id} role="tab" type="button" aria-selected={tab === id} onClick={() => setTab(id)}>{label}</button>
        ))}
      </div>
      <div role="tabpanel">
        {tab === 'overview' && <OverviewTab />}
        {tab === 'inquiries' && <InquiriesTab programs={programs} onEmail={emailOne} />}
        {tab === 'applications' && <ApplicationsTab programs={programs} onEmail={emailOne} />}
        {tab === 'email' && (
          <EmailTab programs={programs} events={events} target={emailTarget} onClearTarget={() => setEmailTarget(null)} />
        )}
        {tab === 'events' && <EventsTab events={events} reloadEvents={loadEvents} onEmail={emailOne} />}
        {tab === 'programs' && <ProgramsTab programs={programs} reloadPrograms={loadPrograms} />}
      </div>
    </>
  );
}
