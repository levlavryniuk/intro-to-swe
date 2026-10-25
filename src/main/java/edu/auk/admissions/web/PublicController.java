package edu.auk.admissions.web;

import edu.auk.admissions.domain.User;
import edu.auk.admissions.repository.UserRepository;
import edu.auk.admissions.security.AccountService;
import edu.auk.admissions.security.Roles;
import edu.auk.admissions.service.EventService;
import edu.auk.admissions.service.InquiryService;
import edu.auk.admissions.service.ProgramService;
import edu.auk.admissions.web.dto.Requests.InquiryRequest;
import edu.auk.admissions.web.dto.Requests.RegistrationRequest;
import edu.auk.admissions.web.dto.Requests.SignUpRequest;
import edu.auk.admissions.web.dto.Views.EventView;
import edu.auk.admissions.web.dto.Views.MeView;
import edu.auk.admissions.web.dto.Views.MessageView;
import edu.auk.admissions.web.dto.Views.ProgramView;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Pages that need no account: UC-1 Submit Inquiry, UC-2 Register for Event, and applicant sign-up. */
@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final ProgramService programService;
    private final EventService eventService;
    private final InquiryService inquiryService;
    private final AccountService accountService;
    private final UserRepository users;
    private final Clock clock;

    public PublicController(ProgramService programService, EventService eventService, InquiryService inquiryService,
                            AccountService accountService, UserRepository users, Clock clock) {
        this.programService = programService;
        this.eventService = eventService;
        this.inquiryService = inquiryService;
        this.accountService = accountService;
        this.users = users;
        this.clock = clock;
    }

    @GetMapping("/programs")
    List<ProgramView> programs() {
        LocalDate today = LocalDate.now(clock);
        return programService.list().stream().map(p -> ProgramView.of(p, today)).toList();
    }

    @GetMapping("/events")
    List<EventView> events() {
        return eventService.upcoming().stream().map(EventView::publicOf).toList();
    }

    @PostMapping("/inquiries")
    @ResponseStatus(HttpStatus.CREATED)
    MessageView submitInquiry(@Valid @RequestBody InquiryRequest req) {
        inquiryService.submit(req.name(), req.email(), req.programId(), req.question(), req.promotionalOptIn());
        return new MessageView("Thank you. We received your inquiry and will get back to you.", true);
    }

    @PostMapping("/events/{eventId}/registrations")
    @ResponseStatus(HttpStatus.CREATED)
    MessageView register(@PathVariable Long eventId, @Valid @RequestBody RegistrationRequest req) {
        eventService.register(eventId, req.name(), req.email(), req.promotionalOptIn());
        return new MessageView("You are registered. See you at the event!", true);
    }

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    MessageView signUp(@Valid @RequestBody SignUpRequest req) {
        accountService.signUpApplicant(req.name(), req.email(), req.password());
        return new MessageView("Your account is ready. You can sign in now.", true);
    }

    @GetMapping("/me")
    MeView me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
            return MeView.anonymous();
        }
        User user = users.findByEmailIgnoreCase(auth.getName()).orElse(null);
        if (user == null) {
            return MeView.anonymous();
        }
        return new MeView(true, user.getName(), user.getEmail(), Roles.of(user));
    }
}
