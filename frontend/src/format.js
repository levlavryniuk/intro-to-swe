export const STATUS_LABELS = {
  SUBMITTED: 'Submitted',
  UNDER_REVIEW: 'Under review',
  WAITLISTED: 'Waitlisted',
  ACCEPTED: 'Accepted',
  CONDITIONAL_OFFER: 'Conditional offer',
  REJECTED: 'Rejected',
  WITHDRAWN: 'Withdrawn',
  ENROLLED: 'Enrolled',
};

/** Statuses staff can choose when recording a decision (UC-9). */
export const STAFF_STATUSES = ['UNDER_REVIEW', 'ACCEPTED', 'CONDITIONAL_OFFER', 'WAITLISTED', 'REJECTED', 'ENROLLED'];

export const STAGE_LABELS = { APPLIED: 'Applied', ADMITTED: 'Admitted', ENROLLED: 'Enrolled', CLOSED: 'Closed' };

export const DOCUMENTS = [
  { type: 'TRANSCRIPT', field: 'transcript', label: 'Academic transcript' },
  { type: 'PROOF_OF_IDENTITY', field: 'proofOfIdentity', label: 'Proof of identity' },
  { type: 'MOTIVATION_ESSAY', field: 'motivationEssay', label: 'Motivation essay' },
];

export const DOC_LABELS = Object.fromEntries(DOCUMENTS.map((d) => [d.type, d.label]));

/** FR-03: 15 MB per document. The server checks again. */
export const MAX_FILE_BYTES = 15 * 1024 * 1024;

export function fmtDate(iso) {
  if (!iso) return '';
  const date = new Date(iso.length === 10 ? `${iso}T00:00:00` : iso);
  return date.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
}

export function fmtDateTime(iso) {
  if (!iso) return '';
  return new Date(iso).toLocaleString(undefined, {
    year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
  });
}

export function fmtSize(bytes) {
  return bytes >= 1048576 ? `${(bytes / 1048576).toFixed(1)} MB` : `${Math.max(1, Math.round(bytes / 1024))} KB`;
}

/** Problem with a chosen file, or null when it can be uploaded. */
export function fileProblem(file) {
  if (!file) return null;
  if (!(file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf'))) return 'Choose a PDF file.';
  if (file.size > MAX_FILE_BYTES) return `This file is ${fmtSize(file.size)}. The limit is 15 MB.`;
  return null;
}
