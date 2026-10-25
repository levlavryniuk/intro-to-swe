package edu.auk.admissions.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/**
 * The center of the module: one applicant's application to one program.
 * Every status change goes through this class, so a decision log entry is always written (FR-09).
 */
@Entity
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(unique = true)
    private Applicant applicant;

    @ManyToOne(optional = false)
    private Program program;

    /** Null until the application is submitted. */
    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    /** Condition of a conditional offer. "condition" is a reserved word in MySQL. */
    @Column(name = "offer_condition", length = 1000)
    private String condition;

    private String phone;
    private String address;
    private String lastSchool;
    private int graduationYear;
    private double gradeAverage;

    private LocalDateTime submittedAt;

    private boolean sisTransferDone;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Document> documents = new ArrayList<>();

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("changedAt ASC, id ASC")
    private List<StatusChange> statusChanges = new ArrayList<>();

    protected Application() {
    }

    public Application(Applicant applicant, Program program) {
        this.applicant = applicant;
        this.program = program;
    }

    public void setDetails(String phone, String address, String lastSchool, int graduationYear, double gradeAverage) {
        this.phone = phone;
        this.address = address;
        this.lastSchool = lastSchool;
        this.graduationYear = graduationYear;
        this.gradeAverage = gradeAverage;
    }

    /** Adds a document, replacing an earlier one of the same type. */
    public void addDocument(Document doc) {
        documents.removeIf(d -> d.getType() == doc.getType());
        doc.attachTo(this);
        documents.add(doc);
    }

    public Optional<Document> documentOf(DocumentType type) {
        return documents.stream().filter(d -> d.getType() == type).findFirst();
    }

    /** All required fields filled in and all three documents uploaded (UC-3). */
    public boolean isComplete() {
        boolean allDocuments = EnumSet.allOf(DocumentType.class).stream()
                .allMatch(type -> documentOf(type).filter(Document::isValid).isPresent());
        return program != null
                && notBlank(phone) && notBlank(address) && notBlank(lastSchool)
                && graduationYear > 0
                && allDocuments;
    }

    /** UC-3. Saves the application with status Submitted and writes the first log entry. */
    public StatusChange submit(LocalDateTime now) {
        if (status != null) {
            throw new BusinessRuleException("This application has already been submitted.");
        }
        if (!program.isOpen(now.toLocalDate())) {
            throw new BusinessRuleException("The program " + program.getName() + " is closed for applications.");
        }
        if (!isComplete()) {
            throw new BusinessRuleException("Fill in every required field and upload all three documents.");
        }
        submittedAt = now;
        return recordChange(ApplicationStatus.SUBMITTED, applicant, now);
    }

    /** UC-4. Only while nobody has started reviewing it and the deadline has not passed. */
    public boolean canBeEdited(LocalDate today) {
        return status == ApplicationStatus.SUBMITTED && program.isOpen(today);
    }

    /** UC-9. Sets a new status chosen by staff, or changes an earlier decision. */
    public StatusChange changeStatus(ApplicationStatus newStatus, String newCondition, User by, LocalDateTime now) {
        if (!(by instanceof StaffMember)) {
            throw new BusinessRuleException("Only admissions staff can record a decision.");
        }
        if (status == null) {
            throw new BusinessRuleException("The application has not been submitted yet.");
        }
        if (!ApplicationStatus.STAFF_SETTABLE.contains(newStatus)) {
            throw new BusinessRuleException("Staff cannot set the status " + newStatus + ".");
        }
        if (status == ApplicationStatus.WITHDRAWN) {
            throw new BusinessRuleException("The application was withdrawn and is closed.");
        }
        if (newStatus == ApplicationStatus.CONDITIONAL_OFFER && !notBlank(newCondition)) {
            throw new BusinessRuleException("Enter the condition of the conditional offer.");
        }
        if (newStatus == ApplicationStatus.ENROLLED && status.stage() != Stage.ADMITTED) {
            throw new BusinessRuleException("Only an admitted applicant can be marked as enrolled.");
        }
        String conditionToKeep = newStatus == ApplicationStatus.CONDITIONAL_OFFER ? newCondition.trim() : null;
        if (newStatus == status && equalsNullable(conditionToKeep, condition)) {
            throw new BusinessRuleException("The application already has this status.");
        }
        condition = conditionToKeep;
        return recordChange(newStatus, by, now);
    }

    /** UC-5. Allowed at any time before a decision. */
    public StatusChange withdraw(Applicant by, LocalDateTime now) {
        if (!by.canView(this)) {
            throw new BusinessRuleException("Only the applicant can withdraw this application.");
        }
        if (status == null || status.isDecision() || status == ApplicationStatus.WITHDRAWN) {
            throw new BusinessRuleException("The application can no longer be withdrawn.");
        }
        condition = null;
        return recordChange(ApplicationStatus.WITHDRAWN, by, now);
    }

    /** UC-15. The SIS confirmed it created a student record. */
    public void markTransferred() {
        sisTransferDone = true;
    }

    private StatusChange recordChange(ApplicationStatus newStatus, User by, LocalDateTime now) {
        StatusChange change = new StatusChange(this, status, newStatus, by, now);
        status = newStatus;
        statusChanges.add(change);
        return change;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static boolean equalsNullable(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    public Long getId() {
        return id;
    }

    public Applicant getApplicant() {
        return applicant;
    }

    public Program getProgram() {
        return program;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public String getCondition() {
        return condition;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public String getLastSchool() {
        return lastSchool;
    }

    public int getGraduationYear() {
        return graduationYear;
    }

    public double getGradeAverage() {
        return gradeAverage;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public boolean isSisTransferDone() {
        return sisTransferDone;
    }

    public List<Document> getDocuments() {
        return documents;
    }

    public List<StatusChange> getStatusChanges() {
        return statusChanges;
    }
}
