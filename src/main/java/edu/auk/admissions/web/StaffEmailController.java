package edu.auk.admissions.web;

import edu.auk.admissions.security.CurrentUser;
import edu.auk.admissions.service.EmailService;
import edu.auk.admissions.web.dto.Requests.GroupEmailRequest;
import edu.auk.admissions.web.dto.Views.EmailView;
import edu.auk.admissions.web.dto.Views.GroupEmailResult;
import edu.auk.admissions.web.dto.Views.RecipientCount;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Group part of UC-10 Send Email, and the list of sent emails. */
@RestController
@RequestMapping("/api/staff/emails")
public class StaffEmailController {

    private final EmailService emailService;
    private final CurrentUser currentUser;

    public StaffEmailController(EmailService emailService, CurrentUser currentUser) {
        this.emailService = emailService;
        this.currentUser = currentUser;
    }

    /** How many opted-in leads a group email would reach. */
    @GetMapping("/recipients")
    RecipientCount recipients(@RequestParam(required = false) Long programId,
                              @RequestParam(required = false) Long eventId) {
        return new RecipientCount(emailService.groupRecipients(programId, eventId).size());
    }

    @PostMapping("/group")
    GroupEmailResult sendGroup(@Valid @RequestBody GroupEmailRequest req) {
        return GroupEmailResult.of(emailService.sendGroup(currentUser.staff(), req.programId(), req.eventId(),
                req.subject(), req.body()));
    }

    @GetMapping
    List<EmailView> recent() {
        return emailService.recent().stream().map(EmailView::of).toList();
    }
}
