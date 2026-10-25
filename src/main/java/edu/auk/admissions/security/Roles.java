package edu.auk.admissions.security;

import edu.auk.admissions.domain.StaffMember;
import edu.auk.admissions.domain.User;

public final class Roles {

    public static final String STAFF = "STAFF";
    public static final String APPLICANT = "APPLICANT";

    private Roles() {
    }

    public static String of(User user) {
        return user instanceof StaffMember ? STAFF : APPLICANT;
    }
}
