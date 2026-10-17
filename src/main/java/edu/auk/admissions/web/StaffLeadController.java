package edu.auk.admissions.web;

import edu.auk.admissions.security.CurrentUser;
import edu.auk.admissions.service.EmailService;
import edu.auk.admissions.service.InquiryService;
import edu.auk.admissions.service.LeadService;
import edu.auk.admissions.web.dto.Requests.EmailRequest;
import edu.auk.admissions.web.dto.Requests.InterestRequest;
import edu.auk.admissions.web.dto.Views.EmailView;
import edu.auk.admissions.web.dto.Views.InquiryView;
import edu.auk.admissions.web.dto.Views.MessageView;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** UC-7 Manage Inquiries, UC-11 Remove Lead Opt-in and the one-person part of UC-10. */
@RestController
@RequestMapping("/api/staff")
public class StaffLeadController {

    private final InquiryService inquiryService;
    private final LeadService leadService;
    private final EmailService emailService;
    private final CurrentUser currentUser;

    public StaffLeadController(InquiryService inquiryService, LeadService leadService, EmailService emailService,
                               CurrentUser currentUser) {
        this.inquiryService = inquiryService;
        this.leadService = leadService;
        this.emailService = emailService;
        this.currentUser = currentUser;
    }

    @GetMapping("/inquiries")
    List<InquiryView> inquiries(@RequestParam(required = false) Long programId) {
        return inquiryService.list(programId).stream().map(InquiryView::of).toList();
    }

    @PutMapping("/inquiries/{id}/interest")
    InquiryView rate(@PathVariable Long id, @Valid @RequestBody InterestRequest req) {
        return InquiryView.of(inquiryService.rate(id, req.interest()));
    }

    @PostMapping("/leads/{id}/remove-opt-in")
    MessageView removeOptIn(@PathVariable Long id) {
        boolean changed = leadService.removeOptIn(id);
        return new MessageView(changed
                ? "Opt-in removed. Group emails will skip this lead."
                : "This lead has already opted out. Nothing to change.", changed);
    }

    @PostMapping("/leads/{id}/email")
    EmailView emailLead(@PathVariable Long id, @Valid @RequestBody EmailRequest req) {
        return EmailView.of(emailService.sendToLead(currentUser.staff(), id, req.subject(), req.body()));
    }
}
