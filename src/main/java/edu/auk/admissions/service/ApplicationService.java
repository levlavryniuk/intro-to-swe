package edu.auk.admissions.service;

import edu.auk.admissions.domain.Applicant;
import edu.auk.admissions.domain.Application;
import edu.auk.admissions.domain.ApplicationStatus;
import edu.auk.admissions.domain.BusinessRuleException;
import edu.auk.admissions.domain.Document;
import edu.auk.admissions.domain.DocumentType;
import edu.auk.admissions.domain.EmailMessage;
import edu.auk.admissions.domain.Program;
import edu.auk.admissions.domain.StaffMember;
import edu.auk.admissions.integration.SisGateway;
import edu.auk.admissions.repository.ApplicationRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

/** UC-3 to UC-6 for applicants, UC-8, UC-9 and UC-15 for staff. */
@Service
public class ApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationService.class);

    private final ApplicationRepository applications;
    private final ProgramService programService;
    private final DocumentStorage storage;
    private final EmailService emailService;
    private final SisGateway sisGateway;
    private final Clock clock;

    public ApplicationService(ApplicationRepository applications, ProgramService programService,
                              DocumentStorage storage, EmailService emailService, SisGateway sisGateway,
                              Clock clock) {
        this.applications = applications;
        this.programService = programService;
        this.storage = storage;
        this.emailService = emailService;
        this.sisGateway = sisGateway;
        this.clock = clock;
    }

    /** UC-3 Submit Application. All three documents are required. */
    @Transactional
    public Application submit(Applicant applicant, Long programId, ApplicationDetails details,
                              Map<DocumentType, MultipartFile> files) {
        if (applications.findByApplicantId(applicant.getId()).isPresent()) {
            throw new BusinessRuleException("You already have an application. Only one application is allowed.");
        }
        Program program = programService.get(programId);
        if (!program.isOpen(today())) {
            throw new BusinessRuleException("The program " + program.getName() + " is closed for applications.");
        }
        for (DocumentType type : DocumentType.values()) {
            storage.validate(files.get(type), type);
        }

        Application app = new Application(applicant, program);
        apply(app, details);
        storeDocuments(app, files);
        app.submit(now());
        return applications.save(app);
    }

    /** UC-4 Edit Application. Documents are optional here; a new file replaces the old one. */
    @Transactional
    public Application edit(Applicant applicant, ApplicationDetails details, Map<DocumentType, MultipartFile> files) {
        Application app = mine(applicant);
        if (!app.canBeEdited(today())) {
            throw new BusinessRuleException(app.getStatus() != ApplicationStatus.SUBMITTED
                    ? "The application can only be edited while its status is Submitted."
                    : "The deadline for " + app.getProgram().getName() + " has passed.");
        }
        files.forEach((type, file) -> storage.validate(file, type));
        apply(app, details);
        storeDocuments(app, files);
        return app;
    }

    /** UC-5 Withdraw Application. */
    @Transactional
    public Application withdraw(Applicant applicant) {
        Application app = mine(applicant);
        app.withdraw(applicant, now());
        return app;
    }

    /** UC-6 Track Application Status. */
    @Transactional(readOnly = true)
    public Optional<Application> findMine(Applicant applicant) {
        return applications.findByApplicantId(applicant.getId());
    }

    @Transactional(readOnly = true)
    public List<Application> search(Long programId, ApplicationStatus status) {
        return applications.search(programId, status);
    }

    @Transactional(readOnly = true)
    public Application get(Long id) {
        return applications.findById(id).orElseThrow(() -> new NotFoundException("Application not found."));
    }

    @Transactional(readOnly = true)
    public Document document(Long applicationId, DocumentType type) {
        return get(applicationId).documentOf(type)
                .orElseThrow(() -> new NotFoundException("Document not found."));
    }

    /** UC-9 Record Decision, continuing with UC-15 when the applicant is accepted. */
    @Transactional
    public DecisionOutcome recordDecision(StaffMember staff, Long applicationId,
                                          ApplicationStatus status, String condition) {
        Application app = get(applicationId);
        app.changeStatus(status, condition, staff, now());
        applications.saveAndFlush(app);

        EmailMessage email = emailService.sendDecision(staff, app);

        String sisWarning = null;
        if (status == ApplicationStatus.ACCEPTED && !app.isSisTransferDone()) {
            if (transferToSis(app)) {
                app.markTransferred();
            } else {
                sisWarning = "The SIS could not be reached. The transfer of this applicant is still pending.";
            }
        }
        return new DecisionOutcome(app, email, sisWarning);
    }

    private boolean transferToSis(Application app) {
        try {
            return sisGateway.sendAcceptedApplicant(app);
        } catch (RuntimeException e) {
            log.warn("SIS transfer of application {} failed", app.getId(), e);
            return false;
        }
    }

    private Application mine(Applicant applicant) {
        return applications.findByApplicantId(applicant.getId())
                .orElseThrow(() -> new NotFoundException("You have not submitted an application yet."));
    }

    private static void apply(Application app, ApplicationDetails d) {
        app.setDetails(d.phone().trim(), d.address().trim(), d.lastSchool().trim(), d.graduationYear(), d.gradeAverage());
    }

    /** Saves new files; replaced files are removed after commit, new ones if the transaction rolls back. */
    private void storeDocuments(Application app, Map<DocumentType, MultipartFile> files) {
        List<String> replaced = new ArrayList<>();
        List<String> added = new ArrayList<>();
        files.forEach((type, file) -> {
            app.documentOf(type).ifPresent(old -> replaced.add(old.getStoragePath()));
            Document doc = storage.store(file, type);
            added.add(doc.getStoragePath());
            app.addDocument(doc);
        });
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    List<String> toDelete = status == STATUS_COMMITTED ? replaced : added;
                    toDelete.forEach(storage::deleteQuietly);
                }
            });
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    public record DecisionOutcome(Application application, EmailMessage email, String sisWarning) {
    }
}
