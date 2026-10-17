package edu.auk.admissions.web;

import edu.auk.admissions.domain.ApplicationStatus;
import edu.auk.admissions.domain.Document;
import edu.auk.admissions.domain.DocumentType;
import edu.auk.admissions.security.CurrentUser;
import edu.auk.admissions.service.ApplicationService;
import edu.auk.admissions.service.ApplicationService.DecisionOutcome;
import edu.auk.admissions.service.DocumentStorage;
import edu.auk.admissions.service.EmailService;
import edu.auk.admissions.web.dto.Requests.DecisionRequest;
import edu.auk.admissions.web.dto.Requests.EmailRequest;
import edu.auk.admissions.web.dto.Views.ApplicationDetail;
import edu.auk.admissions.web.dto.Views.ApplicationSummary;
import edu.auk.admissions.web.dto.Views.DecisionResult;
import edu.auk.admissions.web.dto.Views.EmailView;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** UC-8 Review Application, UC-9 Record Decision and emailing one applicant (UC-10). */
@RestController
@RequestMapping("/api/staff/applications")
public class StaffApplicationController {

    private final ApplicationService applicationService;
    private final EmailService emailService;
    private final DocumentStorage storage;
    private final CurrentUser currentUser;
    private final Clock clock;

    public StaffApplicationController(ApplicationService applicationService, EmailService emailService,
                                      DocumentStorage storage, CurrentUser currentUser, Clock clock) {
        this.applicationService = applicationService;
        this.emailService = emailService;
        this.storage = storage;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @GetMapping
    List<ApplicationSummary> list(@RequestParam(required = false) Long programId,
                                  @RequestParam(required = false) ApplicationStatus status) {
        return applicationService.search(programId, status).stream().map(ApplicationSummary::of).toList();
    }

    @GetMapping("/{id}")
    ApplicationDetail get(@PathVariable Long id) {
        return ApplicationDetail.of(applicationService.get(id), LocalDate.now(clock));
    }

    /** Served only to signed-in staff; the folder is never publicly reachable. */
    @GetMapping("/{id}/documents/{type}")
    ResponseEntity<Resource> document(@PathVariable Long id, @PathVariable DocumentType type) {
        Document doc = applicationService.document(id, type);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(doc.getFileName()).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(storage.pathOf(doc)));
    }

    @PostMapping("/{id}/status")
    DecisionResult recordDecision(@PathVariable Long id, @Valid @RequestBody DecisionRequest req) {
        DecisionOutcome outcome = applicationService.recordDecision(currentUser.staff(), id, req.status(),
                req.condition());
        return new DecisionResult(ApplicationDetail.of(outcome.application(), LocalDate.now(clock)),
                EmailView.of(outcome.email()), outcome.sisWarning());
    }

    @PostMapping("/{id}/email")
    EmailView emailApplicant(@PathVariable Long id, @Valid @RequestBody EmailRequest req) {
        return EmailView.of(emailService.sendToApplicant(currentUser.staff(), applicationService.get(id),
                req.subject(), req.body()));
    }
}
