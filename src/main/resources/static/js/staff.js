// Staff page: one tab per group of use cases (UC-7 to UC-14).

const $ = id => document.getElementById(id);
const pageMsg = $('page-msg');
const STAFF_STATUSES = ['UNDER_REVIEW', 'ACCEPTED', 'CONDITIONAL_OFFER', 'WAITLISTED', 'REJECTED', 'ENROLLED'];
let programs = [];
let events = [];
let reviewing = null;     // application open in the review panel
let emailTarget = null;   // { kind: 'lead' | 'application', id, label } for "One person"

// ---------- tabs ----------

const loaders = {};

function openTab(name) {
  document.querySelectorAll('[role=tab]').forEach(t => t.setAttribute('aria-selected', String(t.dataset.tab === name)));
  document.querySelectorAll('[role=tabpanel]').forEach(p => p.classList.toggle('hidden', p.id !== 'tab-' + name));
  showMessage(pageMsg, '');
  if (loaders[name]) loaders[name]();
}

document.querySelectorAll('[role=tab]').forEach(t => t.addEventListener('click', () => openTab(t.dataset.tab)));

async function loadPrograms() {
  const res = await Api.get('/api/staff/programs');
  programs = res.ok ? res.data : [];
  document.querySelectorAll('.program-select').forEach(select => {
    const keep = select.value;
    select.length = 1;
    programs.forEach(p => select.add(new Option(p.name, p.id)));
    select.value = keep;
  });
}

async function loadEventOptions() {
  const res = await Api.get('/api/staff/events');
  events = res.ok ? res.data : [];
  const select = $('em-event');
  const keep = select.value;
  select.length = 1;
  events.forEach(e => select.add(new Option(e.name + ' (' + fmtDate(e.startsAt) + ')', e.id)));
  select.value = keep;
}

// ---------- UC-14 overview ----------

loaders.overview = async () => {
  const res = await Api.get('/api/staff/overview');
  if (!res.ok) return showMessage(pageMsg, res.data.message, 'err');
  const o = res.data;
  $('ov-top').innerHTML =
    '<div class="stat"><div class="num">' + o.inquiries + '</div><div class="lbl">Inquiries</div></div>'
    + '<div class="stat"><div class="num">' + o.eventRegistrations + '</div><div class="lbl">Event registrations</div></div>';
  $('ov-stages').innerHTML = o.stages.map(s =>
    '<div class="stat' + (s.stage === 'CLOSED' ? ' closed' : '') + '">'
    + '<div class="num">' + s.total + '</div><div class="lbl">' + esc(STAGE_LABELS[s.stage]) + '</div>'
    + '<ul>' + Object.entries(s.statuses).map(([st, n]) =>
        '<li><span>' + esc(STATUS_LABELS[st]) + '</span><strong>' + n + '</strong></li>').join('') + '</ul></div>').join('');
};

// ---------- UC-7 inquiries, UC-11 opt-out ----------

loaders.inquiries = async () => {
  await loadPrograms();
  const programId = $('inq-filter').value;
  const res = await Api.get('/api/staff/inquiries' + (programId ? '?programId=' + programId : ''));
  if (!res.ok) return showMessage(pageMsg, res.data.message, 'err');
  $('inq-empty').classList.toggle('hidden', res.data.length > 0);
  $('inq-rows').innerHTML = res.data.map(i =>
    '<tr>'
    + '<td>' + esc(fmtDateTime(i.submittedAt)) + '</td>'
    + '<td>' + esc(i.name) + '<br><span class="hint">' + esc(i.email) + '</span></td>'
    + '<td>' + esc(i.programName) + '</td>'
    + '<td>' + (i.question ? esc(i.question) : '<span class="hint">No question</span>') + '</td>'
    + '<td>' + (i.promotionalOptIn ? 'Yes' : 'No') + '</td>'
    + '<td><label class="sr-only" for="int-' + i.id + '">Interest</label><select id="int-' + i.id + '" data-inquiry="' + i.id + '">'
    + '<option value="">Not rated</option>'
    + ['HIGH', 'MEDIUM', 'LOW'].map(l => '<option value="' + l + '"' + (i.interest === l ? ' selected' : '') + '>'
        + l.charAt(0) + l.slice(1).toLowerCase() + '</option>').join('') + '</select></td>'
    + '<td><button class="small secondary" data-email-lead="' + i.leadId + '" data-label="' + esc(i.name + ' <' + i.email + '>') + '">Email</button> '
    + (i.promotionalOptIn ? '<button class="small secondary" data-optout="' + i.leadId + '">Remove opt-in</button>' : '')
    + '</td></tr>').join('');
};

$('inq-filter').addEventListener('change', loaders.inquiries);

$('inq-rows').addEventListener('change', async e => {
  const id = e.target.dataset.inquiry;
  if (!id || !e.target.value) return;
  const res = await Api.put('/api/staff/inquiries/' + id + '/interest', { interest: e.target.value });
  showMessage(pageMsg, res.ok ? 'Interest saved.' : res.data.message, res.ok ? 'ok' : 'err');
});

document.addEventListener('click', async e => {
  const optout = e.target.dataset && e.target.dataset.optout;
  if (optout) {
    const res = await Api.post('/api/staff/leads/' + optout + '/remove-opt-in');
    showMessage(pageMsg, res.data.message, res.ok ? (res.data.changed ? 'ok' : 'warn') : 'err');
    const open = document.querySelector('[role=tab][aria-selected=true]').dataset.tab;
    if (open === 'inquiries') loaders.inquiries();
    if (open === 'events' && $('ev-regs').dataset.eventId) showRegistrations($('ev-regs').dataset.eventId);
  }
  const lead = e.target.dataset && e.target.dataset.emailLead;
  if (lead) emailOne({ kind: 'lead', id: lead, label: e.target.dataset.label });
});

// ---------- UC-8 review, UC-9 decision ----------

$('app-status').innerHTML += Object.keys(STATUS_LABELS).map(s => '<option value="' + s + '">' + esc(STATUS_LABELS[s]) + '</option>').join('');
$('dc-status').innerHTML = STAFF_STATUSES.map(s => '<option value="' + s + '">' + esc(STATUS_LABELS[s]) + '</option>').join('');

loaders.applications = async () => {
  await loadPrograms();
  const params = new URLSearchParams();
  if ($('app-program').value) params.set('programId', $('app-program').value);
  if ($('app-status').value) params.set('status', $('app-status').value);
  const res = await Api.get('/api/staff/applications?' + params);
  if (!res.ok) return showMessage(pageMsg, res.data.message, 'err');
  $('app-empty').classList.toggle('hidden', res.data.length > 0);
  $('app-rows').innerHTML = res.data.map(a =>
    '<tr class="clickable' + (reviewing && reviewing.id === a.id ? ' selected' : '') + '" tabindex="0" data-app="' + a.id + '">'
    + '<td>' + esc(fmtDateTime(a.submittedAt)) + '</td>'
    + '<td>' + esc(a.applicantName) + '<br><span class="hint">' + esc(a.applicantEmail) + '</span></td>'
    + '<td>' + esc(a.programName) + '</td><td>' + statusBadge(a.status) + '</td></tr>').join('');
};

$('app-program').addEventListener('change', loaders.applications);
$('app-status').addEventListener('change', loaders.applications);

function openRowFrom(e) {
  const row = e.target.closest('tr[data-app]');
  if (row) openReview(row.dataset.app);
}
$('app-rows').addEventListener('click', openRowFrom);
$('app-rows').addEventListener('keydown', e => { if (e.key === 'Enter') openRowFrom(e); });

async function openReview(id) {
  const res = await Api.get('/api/staff/applications/' + id);
  if (!res.ok) return showMessage(pageMsg, res.data.message, 'err');
  showReview(res.data);
  document.querySelectorAll('#app-rows tr').forEach(r => r.classList.toggle('selected', r.dataset.app === String(id)));
  $('review').scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function showReview(a) {
  reviewing = a;
  $('review').classList.remove('hidden');
  $('rv-title').innerHTML = esc(a.applicantName) + ' ' + statusBadge(a.status);
  $('rv-details').innerHTML =
    '<dt>Email</dt><dd>' + esc(a.applicantEmail) + '</dd>'
    + '<dt>Program</dt><dd>' + esc(a.program.name) + '</dd>'
    + (a.condition ? '<dt>Condition</dt><dd>' + esc(a.condition) + '</dd>' : '')
    + '<dt>Phone</dt><dd>' + esc(a.phone) + '</dd>'
    + '<dt>Address</dt><dd>' + esc(a.address) + '</dd>'
    + '<dt>Last school</dt><dd>' + esc(a.lastSchool) + ', graduated ' + esc(a.graduationYear) + '</dd>'
    + '<dt>Grade average</dt><dd>' + esc(a.gradeAverage) + '</dd>'
    + '<dt>Submitted</dt><dd>' + esc(fmtDateTime(a.submittedAt)) + '</dd>'
    + (a.status === 'ACCEPTED' || a.status === 'ENROLLED'
        ? '<dt>SIS transfer</dt><dd>' + (a.sisTransferDone ? 'Done' : 'Pending') + '</dd>' : '');
  $('rv-docs').innerHTML = a.documents.map(d =>
    '<li><a href="/api/staff/applications/' + a.id + '/documents/' + d.type + '" target="_blank" rel="noopener">'
    + esc(DOC_LABELS[d.type]) + '</a> <span class="hint">' + esc(d.fileName) + ', ' + fmtSize(d.sizeBytes) + '</span></li>').join('');
  $('rv-log').innerHTML = a.decisionLog.map(c =>
    '<tr><td>' + esc(fmtDateTime(c.changedAt)) + '</td><td>' + (c.oldStatus ? statusBadge(c.oldStatus) : '-') + '</td>'
    + '<td>' + statusBadge(c.newStatus) + '</td><td>' + esc(c.changedBy) + '</td></tr>').join('');
  $('dc-status').value = STAFF_STATUSES.includes(a.status) ? a.status : 'UNDER_REVIEW';
  $('dc-condition').value = a.condition || '';
  toggleCondition();
  showMessage($('decision-msg'), '');
  $('decision-warn').classList.add('hidden');
  $('decision-form').querySelector('button').disabled = a.status === 'WITHDRAWN';
  if (a.status === 'WITHDRAWN') showMessage($('decision-msg'), 'The applicant withdrew this application. It is closed.', 'warn');
}

function toggleCondition() {
  $('dc-condition-block').classList.toggle('hidden', $('dc-status').value !== 'CONDITIONAL_OFFER');
}
$('dc-status').addEventListener('change', toggleCondition);

$('decision-form').addEventListener('submit', async e => {
  e.preventDefault();
  const form = e.target;
  const status = $('dc-status').value;
  const res = await Api.post('/api/staff/applications/' + reviewing.id + '/status', {
    status, condition: status === 'CONDITIONAL_OFFER' ? $('dc-condition').value : null,
  });
  if (!res.ok) {
    clearErrors(form);
    if (status === 'CONDITIONAL_OFFER' && /condition/i.test(res.data.message)) {
      showFieldErrors(form, { condition: res.data.message });
    }
    return showMessage($('decision-msg'), res.data.message, 'err');
  }
  showReview(res.data.application);
  const email = res.data.email;
  if (email.status === 'SENT') {
    showMessage($('decision-msg'), 'Status saved. The applicant was emailed.', 'ok');
  } else {
    showMessage($('decision-msg'), 'Status saved, but the email to the applicant failed: ' + (email.failureReason || 'unknown error'), 'err');
  }
  const warn = $('decision-warn');
  warn.classList.toggle('hidden', !res.data.sisWarning);
  warn.textContent = res.data.sisWarning || '';
  loaders.applications();
});

$('rv-email-btn').addEventListener('click', () =>
  emailOne({ kind: 'application', id: reviewing.id, label: reviewing.applicantName + ' <' + reviewing.applicantEmail + '>' }));

// ---------- UC-10 email ----------

function emailMode() {
  return document.querySelector('input[name=mode]:checked').value;
}

function updateEmailMode() {
  const one = emailMode() === 'one';
  $('em-one').classList.toggle('hidden', !one);
  $('em-group').classList.toggle('hidden', one);
}
document.querySelectorAll('input[name=mode]').forEach(r => r.addEventListener('change', updateEmailMode));

function emailOne(target) {
  emailTarget = target;
  openTab('email');
  document.querySelector('input[name=mode][value=one]').checked = true;
  updateEmailMode();
  $('em-one-to').textContent = target.label;
  $('em-subject').focus();
}

async function updateRecipientCount() {
  const programId = $('em-program').value;
  const eventId = $('em-event').value;
  const box = $('em-count');
  if (!programId && !eventId) return box.classList.add('hidden');
  const res = await Api.get('/api/staff/emails/recipients?' + (programId ? 'programId=' + programId : 'eventId=' + eventId));
  box.classList.remove('hidden');
  if (!res.ok) return showMessage(box, res.data.message, 'err');
  const n = res.data.count;
  showMessage(box, n === 0
    ? 'Nobody in this group has opted in to promotional email. Choose another group.'
    : n + (n === 1 ? ' person' : ' people') + ' will get this email. Leads who did not opt in are left out.', n === 0 ? 'warn' : 'ok');
}
$('em-program').addEventListener('change', () => { $('em-event').value = ''; updateRecipientCount(); });
$('em-event').addEventListener('change', () => { $('em-program').value = ''; updateRecipientCount(); });

$('email-form').addEventListener('submit', async e => {
  e.preventDefault();
  const form = e.target;
  const subject = $('em-subject').value;
  const body = $('em-body').value;
  let res;
  if (emailMode() === 'one') {
    if (!emailTarget) return showMessage($('email-msg'), 'Choose a person first with an Email button on another tab.', 'err');
    const url = emailTarget.kind === 'lead'
      ? '/api/staff/leads/' + emailTarget.id + '/email'
      : '/api/staff/applications/' + emailTarget.id + '/email';
    res = await Api.post(url, { subject, body });
    if (!showResult(form, $('email-msg'), res, 'x')) return;
    showMessage($('email-msg'), res.data.status === 'SENT' ? 'Email sent to ' + res.data.recipientEmail + '.'
      : 'The email failed: ' + (res.data.failureReason || 'unknown error'), res.data.status === 'SENT' ? 'ok' : 'err');
    $('em-results').innerHTML = '';
  } else {
    const programId = $('em-program').value ? Number($('em-program').value) : null;
    const eventId = $('em-event').value ? Number($('em-event').value) : null;
    res = await Api.post('/api/staff/emails/group', { programId, eventId, subject, body });
    if (!showResult(form, $('email-msg'), res, 'x')) return;
    const r = res.data;
    showMessage($('email-msg'), r.sent + ' sent, ' + r.failed + ' failed.', r.failed ? 'warn' : 'ok');
    $('em-results').innerHTML = '<div class="table-wrap"><table><thead><tr><th scope="col">To</th><th scope="col">Result</th></tr></thead><tbody>'
      + r.results.map(m => '<tr><td>' + esc(m.recipientEmail) + '</td><td>' + statusBadge(m.status)
        + (m.failureReason ? ' <span class="hint">' + esc(m.failureReason) + '</span>' : '') + '</td></tr>').join('')
      + '</tbody></table></div>';
  }
  form.subject.value = '';
  form.body.value = '';
  loadRecent();
});

async function loadRecent() {
  const res = await Api.get('/api/staff/emails');
  $('em-recent').innerHTML = (res.data || []).map(m =>
    '<tr><td>' + esc(fmtDateTime(m.sentAt)) + '</td><td>' + esc(m.recipientEmail) + '</td><td>' + esc(m.subject)
    + (m.decisionEmail ? ' <span class="hint">(decision)</span>' : '') + '</td><td>' + statusBadge(m.status) + '</td></tr>').join('');
}

loaders.email = async () => {
  await Promise.all([loadPrograms(), loadEventOptions()]);
  updateEmailMode();
  loadRecent();
};

// ---------- UC-12 events ----------

loaders.events = async () => {
  await loadEventOptions();
  $('ev-rows').innerHTML = events.length === 0 ? '<tr><td colspan="3" class="empty">No events yet.</td></tr>'
    : events.map(e => '<tr class="clickable" tabindex="0" data-event="' + e.id + '"><td>' + esc(fmtDateTime(e.startsAt)) + '</td>'
      + '<td>' + esc(e.name) + '<br><span class="hint">' + esc(e.location || e.onlineLink) + '</span></td><td>' + e.registrations + '</td></tr>').join('');
};

function openEventFrom(e) {
  const row = e.target.closest('tr[data-event]');
  if (row) showRegistrations(row.dataset.event);
}
$('ev-rows').addEventListener('click', openEventFrom);
$('ev-rows').addEventListener('keydown', e => { if (e.key === 'Enter') openEventFrom(e); });

async function showRegistrations(eventId) {
  const res = await Api.get('/api/staff/events/' + eventId + '/registrations');
  if (!res.ok) return showMessage(pageMsg, res.data.message, 'err');
  const ev = events.find(x => String(x.id) === String(eventId));
  $('ev-regs').dataset.eventId = eventId;
  $('ev-regs').classList.remove('hidden');
  $('ev-regs-title').textContent = 'Registrations: ' + (ev ? ev.name : '');
  $('ev-regs-empty').classList.toggle('hidden', res.data.length > 0);
  $('ev-reg-rows').innerHTML = res.data.map(r => '<tr><td>' + esc(fmtDateTime(r.registeredAt)) + '</td><td>' + esc(r.name)
    + '</td><td>' + esc(r.email) + '</td><td>' + (r.promotionalOptIn ? 'Yes' : 'No') + '</td>'
    + '<td><button class="small secondary" data-email-lead="' + r.leadId + '" data-label="' + esc(r.name + ' <' + r.email + '>') + '">Email</button> '
    + (r.promotionalOptIn ? '<button class="small secondary" data-optout="' + r.leadId + '">Remove opt-in</button>' : '') + '</td></tr>').join('');
}

$('event-form').addEventListener('submit', async e => {
  e.preventDefault();
  const form = e.target;
  const res = await Api.post('/api/staff/events', {
    name: form.name.value,
    startsAt: form.startsAt.value ? form.startsAt.value + (form.startsAt.value.length === 16 ? ':00' : '') : null,
    location: form.location.value,
    onlineLink: form.onlineLink.value,
  });
  if (showResult(form, $('event-msg'), res, 'Event created. It now appears on the public events page.')) {
    form.reset();
    loaders.events();
  }
});

// ---------- UC-13 programs ----------

loaders.programs = async () => {
  await loadPrograms();
  $('pg-rows').innerHTML = programs.map(p => '<tr><td>' + esc(p.name) + '</td><td>' + (p.deadline ? esc(fmtDate(p.deadline)) : '<span class="hint">Not set</span>') + '</td>'
    + '<td>' + (p.open ? 'Yes' : 'No') + '</td>'
    + '<td><form class="deadline-form" data-program="' + p.id + '" style="display:flex;gap:8px">'
    + '<label class="sr-only" for="dl-' + p.id + '">New deadline</label>'
    + '<input id="dl-' + p.id + '" type="date" name="deadline" value="' + esc(p.deadline || '') + '" style="max-width:170px">'
    + '<button class="small" type="submit">Save</button></form></td></tr>').join('');
};

$('program-form').addEventListener('submit', async e => {
  e.preventDefault();
  const form = e.target;
  const res = await Api.post('/api/staff/programs', { name: form.name.value, deadline: form.deadline.value || null });
  if (showResult(form, $('program-msg'), res, 'Program added.')) {
    form.reset();
    loaders.programs();
  }
});

$('pg-rows').addEventListener('submit', async e => {
  e.preventDefault();
  const form = e.target;
  const res = await Api.put('/api/staff/programs/' + form.dataset.program + '/deadline', { deadline: form.deadline.value || null });
  showMessage($('program-msg'), res.ok ? 'Deadline saved for ' + res.data.name + '.' : res.data.message, res.ok ? 'ok' : 'err');
  if (res.ok) loaders.programs();
});

// ---------- start ----------

showWho($('who'));
loaders.overview();
