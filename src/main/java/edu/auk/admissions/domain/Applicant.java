package edu.auk.admissions.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * A visitor who created an account and applies to a program. Owns at most one
 * application (FR-02); the work itself is done by {@link Application}.
 */
@Entity
@DiscriminatorValue("APPLICANT")
public class Applicant extends User {

    protected Applicant() {
    }

    public Applicant(String name, String email) {
        super(name, email);
    }

    @Override
    public boolean canView(Application app) {
        return app != null && getId() != null && getId().equals(app.getApplicant().getId());
    }
}
