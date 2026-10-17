package edu.auk.admissions.web;

import edu.auk.admissions.domain.Applicant;
import edu.auk.admissions.domain.Application;
import edu.auk.admissions.domain.DocumentType;
import edu.auk.admissions.security.CurrentUser;
import edu.auk.admissions.service.ApplicationDetails;
import edu.auk.admissions.service.ApplicationService;
import edu.auk.admissions.service.EmailService;
import edu.auk.admissions.service.NotFoundException;
import edu.auk.admissions.web.dto.Requests.ApplicationForm;
import edu.auk.admissions.web.dto.Views.ApplicantStatusView;
import edu.auk.admissions.web.dto.Views.ApplicationDetail;
import edu.auk.admissions.web.dto.Views.EmailView;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** UC-3 to UC-6. An applicant only ever reaches their own application (NFR-01). */
@RestController
@RequestMapping("/api/applicant/application")
public class ApplicantController {

    private final ApplicationService applicationService;
    private final EmailService emailService;
    private final CurrentUser currentUser;
    private final Clock clock;

    public ApplicantController(ApplicationService applicationService, EmailService emailService,
                               CurrentUser currentUser, Clock clock) {
        this.applicationService = applicationService;
        this.emailService = emailService;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @GetMapping
    ApplicantStatusView myApplication() {
        Application app = applicationService.findMine(currentUser.applicant())
                .orElseThrow(() -> new NotFoundException("You have not submitted an application yet."));
        return statusView(app);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApplicantStatusView submit(@RequestParam(required = false) Long programId,
                               @Valid @ModelAttribute ApplicationForm form,
                               @RequestParam(required = false) MultipartFile transcript,
                               @RequestParam(required = false) MultipartFile proofOfIdentity,
                               @RequestParam(required = false) MultipartFile motivationEssay) {
        if (programId == null) {
            throw new FieldValidationException("programId", "Choose a program.");
        }
        Applicant applicant = currentUser.applicant();
        Application app = applicationService.submit(applicant, programId, details(form),
                files(transcript, proofOfIdentity, motivationEssay));
        return statusView(app);
    }

    @PostMapping("/edit")
    ApplicantStatusView edit(@Valid @ModelAttribute ApplicationForm form,
                             @RequestParam(required = false) MultipartFile transcript,
                             @RequestParam(required = false) MultipartFile proofOfIdentity,
                             @RequestParam(required = false) MultipartFile motivationEssay) {
        Application app = applicationService.edit(currentUser.applicant(), details(form),
                files(transcript, proofOfIdentity, motivationEssay));
        return statusView(app);
    }

    @PostMapping("/withdraw")
    ApplicantStatusView withdraw() {
        return statusView(applicationService.withdraw(currentUser.applicant()));
    }

    private ApplicantStatusView statusView(Application app) {
        return new ApplicantStatusView(ApplicationDetail.of(app, LocalDate.now(clock)),
                emailService.decisionEmailsFor(app).stream().map(EmailView::of).toList());
    }

    private static ApplicationDetails details(ApplicationForm f) {
        return new ApplicationDetails(f.phone(), f.address(), f.lastSchool(), f.graduationYear(), f.gradeAverage());
    }

    /** Leaves out file inputs the applicant did not fill in. */
    private static Map<DocumentType, MultipartFile> files(MultipartFile transcript, MultipartFile identity,
                                                         MultipartFile essay) {
        Map<DocumentType, MultipartFile> files = new EnumMap<>(DocumentType.class);
        putIfPresent(files, DocumentType.TRANSCRIPT, transcript);
        putIfPresent(files, DocumentType.PROOF_OF_IDENTITY, identity);
        putIfPresent(files, DocumentType.MOTIVATION_ESSAY, essay);
        return files;
    }

    private static void putIfPresent(Map<DocumentType, MultipartFile> files, DocumentType type, MultipartFile file) {
        if (file != null && !file.isEmpty()) {
            files.put(type, file);
        }
    }
}
