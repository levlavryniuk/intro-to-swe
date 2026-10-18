package edu.auk.admissions.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A prospective student who sent an inquiry or registered for an event. One lead per email address. */
@Entity
@Table(name = "leads") // "lead" is a reserved word in MySQL 8
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private boolean promotionalOptIn;

    protected Lead() {
    }

    public Lead(String name, String email, boolean promotionalOptIn) {
        this.name = name;
        this.email = normalizeEmail(email);
        this.promotionalOptIn = promotionalOptIn;
    }

    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    /** The latest form the lead sent decides the name we use and the opt-in. */
    public void updateFromForm(String name, boolean promotionalOptIn) {
        this.name = name;
        this.promotionalOptIn = promotionalOptIn;
    }

    /** UC-11. Returns false when the lead had already opted out, so there was nothing to change. */
    public boolean removeOptIn() {
        if (!promotionalOptIn) {
            return false;
        }
        promotionalOptIn = false;
        return true;
    }

    public boolean canReceiveGroupEmail() {
        return promotionalOptIn;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public boolean isPromotionalOptIn() {
        return promotionalOptIn;
    }
}
