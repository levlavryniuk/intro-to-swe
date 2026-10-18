// Calls the Spring Boot REST API. Every call resolves to { ok, status, data } and never throws,
// so pages can show the server's message (and its per-field errors) directly.

function csrfToken() {
  const match = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]+)/);
  return match ? decodeURIComponent(match[1]) : '';
}

async function request(method, url, body) {
  const headers = { Accept: 'application/json' };
  const options = { method, headers, credentials: 'same-origin' };
  if (method !== 'GET') {
    headers['X-XSRF-TOKEN'] = csrfToken();
  }
  if (body instanceof FormData || body instanceof URLSearchParams) {
    options.body = body;
  } else if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
    options.body = JSON.stringify(body);
  }

  let response;
  try {
    response = await fetch(url, options);
  } catch {
    return { ok: false, status: 0, data: { message: 'The server cannot be reached. Check your connection and try again.' } };
  }
  const text = await response.text();
  let data = {};
  try {
    data = text ? JSON.parse(text) : {};
  } catch {
    data = { message: text };
  }
  if (!response.ok && !data.message) {
    data.message = {
      401: 'Please sign in first.',
      403: 'You do not have access to this.',
      413: 'Each file must be a PDF of at most 15 MB.',
    }[response.status] || `Something went wrong (${response.status}).`;
  }
  return { ok: response.ok, status: response.status, data };
}

export const api = {
  get: (url) => request('GET', url),
  post: (url, body = {}) => request('POST', url, body),
  put: (url, body) => request('PUT', url, body),
  /** Spring Security's sign-in endpoint takes a form, not JSON. */
  signIn: (email, password) => request('POST', '/login', new URLSearchParams({ email, password })),
  signOut: () => request('POST', '/logout'),
};
