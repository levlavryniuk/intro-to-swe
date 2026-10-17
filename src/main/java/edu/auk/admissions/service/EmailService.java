package edu.auk.admissions.service;

import edu.auk.admissions.domain.Application;
import edu.auk.admissions.domain.ApplicationStatus;
import edu.auk.admissions.domain.BusinessRuleException;
import edu.auk.admissions.domain.DeliveryStatus;
import edu.auk.admissions.domain.EmailMessage;
import edu.auk.admissions.domain.Lead;
import edu.auk.admissions.domain.StaffMember;
import edu.auk.admissions.integration.MailGateway;
import edu.auk.admissions.repository.EmailMessageRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** UC-10 Send Email, plus the automatic decision email of UC-9. */
@Service
public class EmailService {

    private final EmailMessageRepository messages;
    private final MailGateway mailGateway;
    private final LeadService leadService;
    private final ProgramService programService;
    private final EventService eventService;
    private final Clock clock;

    public EmailService(EmailMessageRepository messages, MailGateway mailGateway, LeadService leadService,
                        ProgramService programService, EventService eventService, Clock clock) {
        this.messages = messages;
        this.mailGateway = mailGateway;
        this.leadService = leadService;
        this.programService = programService;
        this.eventService = eventService;
        this.clock = clock;
    }

    /** A personal reply goes out whether or not the lead opted in. */
    @Transactional
    public EmailMessage sendToLead(StaffMember sender, Long leadId, String subject, String body) {
        Lead lead = leadService.get(leadId);
        return deliver(EmailMessage.toLead(sender, lead, subject.trim(), body.trim()));
    }

    @Transactional
    public EmailMessage sendToApplicant(StaffMember sender, Application app, String subject, String body) {
        return deliver(EmailMessage.toApplicant(sender, app, subject.trim(), body.trim()));
    }

    /** Opted-in leads who asked about a program or signed up for an event. Exactly one must be given. */
    @Transactional(readOnly = true)
    public List<Lead> groupRecipients(Long programId, Long eventId) {
        if ((programId == null) == (eventId == null)) {
            throw new BusinessRuleException("Choose either a program or an event.");
        }
        return programId != null
                ? programService.get(programId).optedInLeads()
                : eventService.get(eventId).optedInLeads();
    }

    @Transactional
    public List<EmailMessage> sendGroup(StaffMember sender, Long programId, Long eventId, String subject, String body) {
        List<Lead> recipients = groupRecipients(programId, eventId);
        if (recipients.isEmpty()) {
            throw new BusinessRuleException("Nobody in this group has opted in to promotional email.");
        }
        return recipients.stream()
                .map(lead -> deliver(EmailMessage.toLead(sender, lead, subject.trim(), body.trim())))
                .toList();
    }

    /** UC-9: tells the applicant about the new status. */
    @Transactional
    public EmailMessage sendDecision(StaffMember sender, Application app) {
        String program = app.getProgram().getName();
        StringBuilder body = new StringBuilder()
                .append("Dear ").append(app.getApplicant().getName()).append(",\n\n")
                .append("The status of your application to ").append(program).append(" is now: ")
                .append(label(app.getStatus())).append(".\n");
        if (app.getStatus() == ApplicationStatus.CONDITIONAL_OFFER) {
            body.append("\nCondition of the offer: ").append(app.getCondition()).append("\n");
        }
        body.append("\nAdmissions Office, American University Kyiv");
        return deliver(EmailMessage.decision(sender, app, "Your application to " + program, body.toString()));
    }

    @Transactional(readOnly = true)
    public List<EmailMessage> decisionEmailsFor(Application app) {
        return messages.findByApplicationIdAndDecisionEmailTrueOrderByIdDesc(app.getId());
    }

    @Transactional(readOnly = true)
    public List<EmailMessage> recent() {
        return messages.findTop50ByOrderByIdDesc();
    }

    private EmailMessage deliver(EmailMessage message) {
        try {
            DeliveryStatus result = mailGateway.send(message);
            if (result == DeliveryStatus.SENT) {
                message.markSent(LocalDateTime.now(clock));
            } else {
                message.markFailed("The mail service did not accept the message.");
            }
        } catch (RuntimeException e) {
            message.markFailed(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
        return messages.save(message);
    }

    public static String label(ApplicationStatus status) {
        String words = status.name().replace('_', ' ').toLowerCase();
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }
}
