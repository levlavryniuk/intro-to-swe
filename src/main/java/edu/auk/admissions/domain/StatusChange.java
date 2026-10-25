package edu.auk.admissions.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;

/** One line of the decision log (FR-09). Created by {@link Application} and never edited. */
@Entity
public class StatusChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Application application;

    /** Null for the first entry, written when the application is submitted. */
    @Enumerated(EnumType.STRING)
    private ApplicationStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus newStatus;

    @ManyToOne(optional = false)
    private User changedBy;

    @Column(nullable = false)
    private LocalDateTime changedAt;

    protected StatusChange() {
    }

    StatusChange(Application application, ApplicationStatus oldStatus, ApplicationStatus newStatus,
                 User changedBy, LocalDateTime changedAt) {
        this.application = application;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
    }

    public Long getId() {
        return id;
    }

    public Application getApplication() {
        return application;
    }

    public ApplicationStatus getOldStatus() {
        return oldStatus;
    }

    public ApplicationStatus getNewStatus() {
        return newStatus;
    }

    public User getChangedBy() {
        return changedBy;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }
}
