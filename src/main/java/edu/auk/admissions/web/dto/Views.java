package edu.auk.admissions.web.dto;

import edu.auk.admissions.domain.Application;
import edu.auk.admissions.domain.ApplicationStatus;
import edu.auk.admissions.domain.DeliveryStatus;
import edu.auk.admissions.domain.Document;
import edu.auk.admissions.domain.DocumentType;
import edu.auk.admissions.domain.EmailMessage;
import edu.auk.admissions.domain.Event;
import edu.auk.admissions.domain.EventRegistration;
import edu.auk.admissions.domain.Inquiry;
import edu.auk.admissions.domain.InterestLevel;
import edu.auk.admissions.domain.PipelineReport;
import edu.auk.admissions.domain.Program;
import edu.auk.admissions.domain.Stage;
import edu.auk.admissions.domain.StatusChange;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Response bodies. Entities are never returned directly. */
public final class Views {

    private Views() {
    }

    public record ProgramView(Long id, String name, LocalDate deadline, boolean open) {
        public static ProgramView of(Program p, LocalDate today) {
            return new ProgramView(p.getId(), p.getName(), p.getDeadline(), p.isOpen(today));
        }
    }

    public record EventView(Long id, String name, LocalDateTime startsAt, String location, String onlineLink,
                            int registrations) {
        public static EventView of(Event e) {
            return new EventView(e.getId(), e.getName(), e.getStartsAt(), e.getLocation(), e.getOnlineLink(),
                    e.getRegistrations().size());
        }

        /** Public list: visitors do not need the registration count. */
        public static EventView publicOf(Event e) {
            return new EventView(e.getId(), e.getName(), e.getStartsAt(), e.getLocation(), e.getOnlineLink(), 0);
        }
    }

    public record InquiryView(Long id, Long leadId, String name, String email, Long programId, String programName,
                              String question, boolean promotionalOptIn, InterestLevel interest,
                              LocalDateTime submittedAt) {
        public static InquiryView of(Inquiry i) {
            return new InquiryView(i.getId(), i.getLead().getId(), i.getLead().getName(), i.getLead().getEmail(),
                    i.getProgram().getId(), i.getProgram().getName(), i.getQuestion(),
                    i.getLead().isPromotionalOptIn(), i.getInterest(), i.getSubmittedAt());
        }
    }

    public record RegistrationView(Long id, Long leadId, String name, String email, boolean promotionalOptIn,
                                   LocalDateTime registeredAt) {
        public static RegistrationView of(EventRegistration r) {
            return new RegistrationView(r.getId(), r.getLead().getId(), r.getLead().getName(),
                    r.getLead().getEmail(), r.getLead().isPromotionalOptIn(), r.getRegisteredAt());
        }
    }

    public record ApplicationSummary(Long id, String applicantName, String applicantEmail, String programName,
                                     ApplicationStatus status, LocalDateTime submittedAt) {
        public static ApplicationSummary of(Application a) {
            return new ApplicationSummary(a.getId(), a.getApplicant().getName(), a.getApplicant().getEmail(),
                    a.getProgram().getName(), a.getStatus(), a.getSubmittedAt());
        }
    }

    public record DocumentView(DocumentType type, String fileName, long sizeBytes) {
        public static DocumentView of(Document d) {
            return new DocumentView(d.getType(), d.getFileName(), d.getSizeBytes());
        }
    }

    public record StatusChangeView(ApplicationStatus oldStatus, ApplicationStatus newStatus, String changedBy,
                                   LocalDateTime changedAt) {
        public static StatusChangeView of(StatusChange c) {
            return new StatusChangeView(c.getOldStatus(), c.getNewStatus(), c.getChangedBy().getName(),
                    c.getChangedAt());
        }
    }

    public record ApplicationDetail(Long id, String applicantName, String applicantEmail, ProgramView program,
                                    ApplicationStatus status, Stage stage, String condition, String phone,
                                    String address, String lastSchool, int graduationYear, double gradeAverage,
                                    LocalDateTime submittedAt, boolean sisTransferDone, boolean canEdit,
                                    boolean canWithdraw, List<DocumentView> documents,
                                    List<StatusChangeView> decisionLog) {
        public static ApplicationDetail of(Application a, LocalDate today) {
            ApplicationStatus s = a.getStatus();
            boolean canWithdraw = s == ApplicationStatus.SUBMITTED || s == ApplicationStatus.UNDER_REVIEW;
            return new ApplicationDetail(a.getId(), a.getApplicant().getName(), a.getApplicant().getEmail(),
                    ProgramView.of(a.getProgram(), today), s, s == null ? null : s.stage(), a.getCondition(),
                    a.getPhone(), a.getAddress(), a.getLastSchool(), a.getGraduationYear(), a.getGradeAverage(),
                    a.getSubmittedAt(), a.isSisTransferDone(), a.canBeEdited(today), canWithdraw,
                    a.getDocuments().stream().map(DocumentView::of).toList(),
                    a.getStatusChanges().stream().map(StatusChangeView::of).toList());
        }
    }

    public record EmailView(Long id, String recipientEmail, String subject, String body, DeliveryStatus status,
                            String failureReason, LocalDateTime sentAt, boolean decisionEmail) {
        public static EmailView of(EmailMessage m) {
            return new EmailView(m.getId(), m.getRecipientEmail(), m.getSubject(), m.getBody(), m.getStatus(),
                    m.getFailureReason(), m.getSentAt(), m.isDecisionEmail());
        }
    }

    public record DecisionResult(ApplicationDetail application, EmailView email, String sisWarning) {
    }

    public record GroupEmailResult(int recipients, long sent, long failed, List<EmailView> results) {
        public static GroupEmailResult of(List<EmailMessage> messages) {
            long sent = messages.stream().filter(m -> m.getStatus() == DeliveryStatus.SENT).count();
            return new GroupEmailResult(messages.size(), sent, messages.size() - sent,
                    messages.stream().map(EmailView::of).toList());
        }
    }

    public record RecipientCount(int count) {
    }

    public record StageView(Stage stage, long total, Map<ApplicationStatus, Long> statuses) {
    }

    public record OverviewView(long inquiries, long eventRegistrations, List<StageView> stages,
                               LocalDateTime generatedAt) {
        public static OverviewView of(PipelineReport report) {
            List<StageView> stages = Arrays.stream(Stage.values()).map(stage -> {
                Map<ApplicationStatus, Long> statuses = new LinkedHashMap<>();
                for (ApplicationStatus status : ApplicationStatus.values()) {
                    if (status.stage() == stage) {
                        statuses.put(status, report.countByStatus(status));
                    }
                }
                return new StageView(stage, report.countByStage(stage), statuses);
            }).toList();
            return new OverviewView(report.countInquiries(), report.countEventRegistrations(), stages,
                    report.getGeneratedAt());
        }
    }

    public record MeView(boolean authenticated, String name, String email, String role) {
        public static MeView anonymous() {
            return new MeView(false, null, null, null);
        }
    }

    public record MessageView(String message, boolean changed) {
    }

    public record ErrorView(String message, Map<String, String> fieldErrors) {
        public ErrorView(String message) {
            this(message, Map.of());
        }
    }

    public record ApplicantStatusView(ApplicationDetail application, List<EmailView> decisionEmails) {
    }
}
