package edu.auk.admissions.domain;

import java.util.EnumSet;
import java.util.Set;

/** The eight statuses an application can have (FR-04). */
public enum ApplicationStatus {
    SUBMITTED(Stage.APPLIED),
    UNDER_REVIEW(Stage.APPLIED),
    WAITLISTED(Stage.APPLIED),
    ACCEPTED(Stage.ADMITTED),
    CONDITIONAL_OFFER(Stage.ADMITTED),
    REJECTED(Stage.CLOSED),
    WITHDRAWN(Stage.CLOSED),
    ENROLLED(Stage.ENROLLED);

    /** Statuses staff can choose in Record Decision (UC-9). */
    public static final Set<ApplicationStatus> STAFF_SETTABLE =
            EnumSet.of(UNDER_REVIEW, ACCEPTED, CONDITIONAL_OFFER, WAITLISTED, REJECTED, ENROLLED);

    private final Stage stage;

    ApplicationStatus(Stage stage) {
        this.stage = stage;
    }

    public Stage stage() {
        return stage;
    }

    /** True once staff have recorded a decision; the applicant can then no longer withdraw (UC-5). */
    public boolean isDecision() {
        return this != SUBMITTED && this != UNDER_REVIEW && this != WITHDRAWN;
    }
}
