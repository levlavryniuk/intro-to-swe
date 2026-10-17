package edu.auk.admissions.domain;

/** Pipeline stages used by the overview (FR-07). Rejected and withdrawn applications are closed. */
public enum Stage {
    APPLIED,
    ADMITTED,
    ENROLLED,
    CLOSED
}
