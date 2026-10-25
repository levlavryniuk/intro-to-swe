package edu.auk.admissions.security;

import edu.auk.admissions.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

/**
 * Local sign-in used until the Core Platform (Team 2) provides shared sign-in (FR-10).
 * Kept apart from {@link User} so the domain model does not depend on how people sign in.
 */
@Entity
public class LocalAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    private User user;

    @Column(nullable = false)
    private String passwordHash;

    protected LocalAccount() {
    }

    public LocalAccount(User user, String passwordHash) {
        this.user = user;
        this.passwordHash = passwordHash;
    }

    public User getUser() {
        return user;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
