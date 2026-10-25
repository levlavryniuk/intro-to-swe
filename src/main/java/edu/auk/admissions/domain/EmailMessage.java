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

/** An email the module sends: a personal reply, a group email or an automatic decision email (FR-05). */
@Entity
public class EmailMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private StaffMember sender;

    @ManyToOne
    private Lead lead;

    @ManyToOne
    private Application application;

    @Column(nullable = false)
    private String recipientEmail;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, length = 10000)
    private String body;

    /** Decision emails are listed on the applicant's status page (UC-6). */
    private boolean decisionEmail;

    private LocalDateTime sentAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status = DeliveryStatus.PENDING;

    private String failureReason;

    protected EmailMessage() {
    }

    private EmailMessage(StaffMember sender, String recipientEmail, Lead lead, Application application,
                         String subject, String body, boolean decisionEmail) {
        this.sender = sender;
        this.recipientEmail = recipientEmail;
        this.lead = lead;
        this.application = application;
        this.subject = subject;
        this.body = body;
        this.decisionEmail = decisionEmail;
    }

    public static EmailMessage toLead(StaffMember sender, Lead lead, String subject, String body) {
        return new EmailMessage(sender, lead.getEmail(), lead, null, subject, body, false);
    }

    public static EmailMessage toApplicant(StaffMember sender, Application app, String subject, String body) {
        return new EmailMessage(sender, app.getApplicant().getEmail(), null, app, subject, body, false);
    }

    public static EmailMessage decision(StaffMember sender, Application app, String subject, String body) {
        return new EmailMessage(sender, app.getApplicant().getEmail(), null, app, subject, body, true);
    }

    public void markSent(LocalDateTime now) {
        status = DeliveryStatus.SENT;
        sentAt = now;
        failureReason = null;
    }

    public void markFailed(String reason) {
        status = DeliveryStatus.FAILED;
        failureReason = reason;
    }

    public Long getId() {
        return id;
    }

    public StaffMember getSender() {
        return sender;
    }

    public Lead getLead() {
        return lead;
    }

    public Application getApplication() {
        return application;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public boolean isDecisionEmail() {
        return decisionEmail;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }
}
