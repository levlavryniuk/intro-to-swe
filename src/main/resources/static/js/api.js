// Small helpers shared by all pages: calling the API, showing errors, formatting.

const Api = {
  csrfToken() {
    const match = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]+)/);
    return match ? decodeURIComponent(match[1]) : '';
  },

  async request(method, url, body) {
    const options = { method, headers: { Accept: 'application/json' }, credentials: 'same-origin' };
    if (method !== 'GET') {
      options.headers['X-XSRF-TOKEN'] = this.csrfToken();
    }
    if (body instanceof FormData) {
      options.body = body;
    } else if (body !== undefined) {
      options.headers['Content-Type'] = 'application/json';
      options.body = JSON.stringify(body);
    }
    let response;
    try {
      response = await fetch(url, options);
    } catch (e) {
      return { ok: false, status: 0, data: { message: 'The server cannot be reached. Check your connection and try again.' } };
    }
    if (response.status === 401) {
      window.location.href = '/login';
      return { ok: false, status: 401, data: {} };
    }
    const text = await response.text();
    let data = {};
    try { data = text ? JSON.parse(text) : {}; } catch (e) { data = { message: text }; }
    if (!response.ok && !data.message) {
      data.message = response.status === 403 ? 'You do not have access to this.' : 'Something went wrong (' + response.status + ').';
    }
    return { ok: response.ok, status: response.status, data };
  },

  get(url) { return this.request('GET', url); },
  post(url, body) { return this.request('POST', url, body === undefined ? {} : body); },
  put(url, body) { return this.request('PUT', url, body); },
};

/** Escapes text before it goes into innerHTML. Visitors type much of what staff see. */
function esc(value) {
  if (value === null || value === undefined) return '';
  return String(value).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

const STATUS_LABELS = {
  SUBMITTED: 'Submitted', UNDER_REVIEW: 'Under review', WAITLISTED: 'Waitlisted', ACCEPTED: 'Accepted',
  CONDITIONAL_OFFER: 'Conditional offer', REJECTED: 'Rejected', WITHDRAWN: 'Withdrawn', ENROLLED: 'Enrolled',
};
const STAGE_LABELS = { APPLIED: 'Applied', ADMITTED: 'Admitted', ENROLLED: 'Enrolled', CLOSED: 'Closed' };
const DOC_LABELS = { TRANSCRIPT: 'Academic transcript', PROOF_OF_IDENTITY: 'Proof of identity', MOTIVATION_ESSAY: 'Motivation essay' };

function statusBadge(status) {
  return '<span class="badge ' + esc(status) + '">' + esc(STATUS_LABELS[status] || status) + '</span>';
}

function fmtDate(iso) {
  if (!iso) return '';
  const d = new Date(iso.length === 10 ? iso + 'T00:00:00' : iso);
  return d.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
}

function fmtDateTime(iso) {
  if (!iso) return '';
  return new Date(iso).toLocaleString(undefined, { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
}

function fmtSize(bytes) {
  return bytes >= 1048576 ? (bytes / 1048576).toFixed(1) + ' MB' : Math.max(1, Math.round(bytes / 1024)) + ' KB';
}

/** Shows a message in a .msg box. kind is ok, err or warn. */
function showMessage(el, text, kind) {
  el.className = 'msg ' + (kind || 'ok');
  el.textContent = text || '';
  if (text) el.setAttribute('role', kind === 'err' ? 'alert' : 'status');
}

/** Clears earlier field errors in a form. */
function clearErrors(form) {
  form.querySelectorAll('.field-error').forEach(e => e.remove());
  form.querySelectorAll('[aria-invalid]').forEach(e => e.removeAttribute('aria-invalid'));
}

/** Puts each server field error under its input; typed values stay as they are (UC-1 step 4). */
function showFieldErrors(form, fieldErrors) {
  Object.entries(fieldErrors || {}).forEach(([name, message]) => {
    const input = form.querySelector('[name="' + name + '"]');
    if (!input) return;
    input.setAttribute('aria-invalid', 'true');
    const err = document.createElement('div');
    err.className = 'field-error';
    err.id = 'err-' + form.id + '-' + name;
    err.textContent = message;
    input.setAttribute('aria-describedby', err.id);
    input.insertAdjacentElement('afterend', err);
  });
  const first = form.querySelector('[aria-invalid=true]');
  if (first) first.focus();
}

/** Shows the result of an API call on a form: field errors, or a general message. */
function showResult(form, msgEl, result, okText) {
  clearErrors(form);
  if (result.ok) {
    showMessage(msgEl, okText || (result.data && result.data.message) || 'Saved.', 'ok');
    return true;
  }
  showFieldErrors(form, result.data.fieldErrors);
  showMessage(msgEl, result.data.message, 'err');
  return false;
}

async function logout() {
  await Api.post('/logout');
  window.location.href = '/';
}

/** Fills the "signed in as" part of the top bar. */
async function showWho(el) {
  const me = await Api.get('/api/public/me');
  if (me.ok && me.data.authenticated) {
    el.textContent = 'Signed in as ' + me.data.name;
  }
  return me.data;
}
