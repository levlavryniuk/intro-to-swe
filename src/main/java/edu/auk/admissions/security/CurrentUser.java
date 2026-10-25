package edu.auk.admissions.security;

import edu.auk.admissions.domain.Applicant;
import edu.auk.admissions.domain.StaffMember;
import edu.auk.admissions.domain.User;
import edu.auk.admissions.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Looks up the signed-in person as a domain {@link User}. */
@Component
public class CurrentUser {

    private final UserRepository users;

    public CurrentUser(UserRepository users) {
        this.users = users;
    }

    public User get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Sign in first.");
        }
        return users.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("Unknown user."));
    }

    public Applicant applicant() {
        if (get() instanceof Applicant applicant) {
            return applicant;
        }
        throw new AccessDeniedException("Only applicants can do this.");
    }

    public StaffMember staff() {
        if (get() instanceof StaffMember staff) {
            return staff;
        }
        throw new AccessDeniedException("Only admissions staff can do this.");
    }
}
