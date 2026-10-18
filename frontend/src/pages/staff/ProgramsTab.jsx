import { useState } from 'react';
import { api } from '../../api.js';
import { Field, Message, resultMessage } from '../../components.jsx';
import { fmtDate } from '../../format.js';

/** UC-13 Manage Programs and Deadlines. */
export default function ProgramsTab({ programs, reloadPrograms }) {
  const [name, setName] = useState('');
  const [deadline, setDeadline] = useState('');
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState({});

  async function add(e) {
    e.preventDefault();
    const res = await api.post('/api/staff/programs', { name, deadline: deadline || null });
    setErrors(res.ok ? {} : res.data.fieldErrors || {});
    setMessage(resultMessage(res, 'Program added.'));
    if (res.ok) {
      setName('');
      setDeadline('');
      reloadPrograms();
    }
  }

  async function saveDeadline(program, date) {
    const res = await api.put(`/api/staff/programs/${program.id}/deadline`, { deadline: date || null });
    setMessage(resultMessage(res, `Deadline saved for ${program.name}.`));
    if (res.ok) reloadPrograms();
  }

  return (
    <>
      <h1>Programs and deadlines</h1>
      <section className="card">
        <form onSubmit={add} noValidate className="filters">
          <Field label="Program name" value={name} onChange={(e) => setName(e.target.value)} error={errors.name} />
          <Field label="Deadline" hint="(optional)" type="date" value={deadline} onChange={(e) => setDeadline(e.target.value)} />
          <div><button type="submit">Add program</button></div>
        </form>
        <Message {...message} />
      </section>
      <div className="table-wrap">
        <table>
          <thead>
            <tr><th scope="col">Program</th><th scope="col">Deadline</th><th scope="col">Accepting applications</th><th scope="col">Change deadline</th></tr>
          </thead>
          <tbody>
            {programs.map((p) => (
              <tr key={p.id}>
                <td>{p.name}</td>
                <td>{p.deadline ? fmtDate(p.deadline) : <span className="hint">Not set</span>}</td>
                <td>{p.open ? 'Yes' : 'No'}</td>
                <td><DeadlineEditor program={p} onSave={saveDeadline} /></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}

function DeadlineEditor({ program, onSave }) {
  const [date, setDate] = useState(program.deadline || '');
  return (
    <form style={{ display: 'flex', gap: 8 }} onSubmit={(e) => { e.preventDefault(); onSave(program, date); }}>
      <label className="sr-only" htmlFor={`deadline-${program.id}`}>New deadline for {program.name}</label>
      <input id={`deadline-${program.id}`} type="date" value={date} onChange={(e) => setDate(e.target.value)} style={{ maxWidth: 170 }} />
      <button className="small" type="submit">Save</button>
    </form>
  );
}
