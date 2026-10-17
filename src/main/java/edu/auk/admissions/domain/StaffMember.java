package edu.auk.admissions.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/** An admissions employee. Staff can open every application (NFR-01). */
@Entity
@DiscriminatorValue("STAFF")
public class StaffMember extends User {

    protected StaffMember() {
    }

    public StaffMember(String name, String email) {
        super(name, email);
    }

    @Override
    public boolean canView(Application app) {
        return app != null;
    }
}
