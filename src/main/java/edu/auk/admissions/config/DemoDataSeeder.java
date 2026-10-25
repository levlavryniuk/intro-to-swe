package edu.auk.admissions.config;

import edu.auk.admissions.domain.Applicant;
import edu.auk.admissions.domain.Application;
import edu.auk.admissions.domain.ApplicationStatus;
import edu.auk.admissions.domain.DocumentType;
import edu.auk.admissions.domain.Event;
import edu.auk.admissions.domain.InterestLevel;
import edu.auk.admissions.domain.Program;
import edu.auk.admissions.domain.StaffMember;
import edu.auk.admissions.repository.ApplicationRepository;
import edu.auk.admissions.repository.ProgramRepository;
import edu.auk.admissions.security.AccountService;
import edu.auk.admissions.service.DocumentStorage;
import edu.auk.admissions.service.EventService;
import edu.auk.admissions.service.InquiryService;
import edu.auk.admissions.service.ProgramService;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sample data for the demo: programs, events, inquiries, a few applications and two sign-in accounts.
 * Runs only when app.demo-data=true and the database has no programs yet. All names are made up.
 */
@Component
@ConditionalOnProperty(name = "app.demo-data", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final ProgramRepository programs;
    private final ProgramService programService;
    private final EventService eventService;
    private final InquiryService inquiryService;
    private final AccountService accountService;
    private final ApplicationRepository applications;
    private final DocumentStorage storage;
    private final Clock clock;
    private final String staffEmail;
    private final String staffPassword;
    private final String applicantEmail;
    private final String applicantPassword;

    public DemoDataSeeder(ProgramRepository programs, ProgramService programService, EventService eventService,
                          InquiryService inquiryService, AccountService accountService,
                          ApplicationRepository applications, DocumentStorage storage, Clock clock,
                          @Value("${app.demo.staff-email}") String staffEmail,
                          @Value("${app.demo.staff-password}") String staffPassword,
                          @Value("${app.demo.applicant-email}") String applicantEmail,
                          @Value("${app.demo.applicant-password}") String applicantPassword) {
        this.programs = programs;
        this.programService = programService;
        this.eventService = eventService;
        this.inquiryService = inquiryService;
        this.accountService = accountService;
        this.applications = applications;
        this.storage = storage;
        this.clock = clock;
        this.staffEmail = staffEmail;
        this.staffPassword = staffPassword;
        this.applicantEmail = applicantEmail;
        this.applicantPassword = applicantPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (programs.count() > 0) {
            return;
        }
        LocalDate today = LocalDate.now(clock);
        LocalDateTime now = LocalDateTime.now(clock);

        Program cs = programService.add("Computer Science", today.plusDays(60));
        Program business = programService.add("Business Administration", today.plusDays(45));
        programService.add("Software Engineering", today.plusDays(30));
        programService.add("Psychology", today.minusDays(3));

        Event openDay = eventService.create("Open Day", now.plusDays(14).withHour(11).withMinute(0).withSecond(0).withNano(0),
                "AUK main building, 3 Kyrylivska St, Kyiv", null);
        eventService.create("Webinar: Studying Computer Science at AUK",
                now.plusDays(7).withHour(18).withMinute(0).withSecond(0).withNano(0), null, "https://meet.example.com/auk-cs");

        inquiryService.submit("Olena Kovalenko", "olena.k@example.com", cs.getId(),
                "Is there a scholarship for first-year students?", true);
        var rated = inquiryService.submit("Maksym Shevchuk", "maksym.s@example.com", business.getId(),
                "Can I apply with a foreign school certificate?", true);
        inquiryService.rate(rated.getId(), InterestLevel.HIGH);
        inquiryService.submit("Iryna Bondar", "iryna.b@example.com", cs.getId(), null, false);
        eventService.register(openDay.getId(), "Olena Kovalenko", "olena.k@example.com", true);
        eventService.register(openDay.getId(), "Taras Melnyk", "taras.m@example.com", false);

        StaffMember staff = accountService.createStaff("Admissions Officer", staffEmail, staffPassword);
        accountService.signUpApplicant("Demo Applicant", applicantEmail, applicantPassword);

        sampleApplication("Sofiia Hnatiuk", "sofiia.h@example.com", cs, now, staff, null);
        sampleApplication("Dmytro Lysenko", "dmytro.l@example.com", business, now, staff, ApplicationStatus.UNDER_REVIEW);
        sampleApplication("Anna Petrenko", "anna.p@example.com", cs, now, staff, ApplicationStatus.CONDITIONAL_OFFER);

        log.info("Demo data loaded. Staff sign-in: {}  Applicant sign-in: {}  (passwords in application.properties)",
                staffEmail, applicantEmail);
    }

    private void sampleApplication(String name, String email, Program program, LocalDateTime now,
                                   StaffMember staff, ApplicationStatus status) {
        Applicant applicant = accountService.signUpApplicant(name, email, "sample-" + Long.toHexString(System.nanoTime()));
        Application app = new Application(applicant, program);
        app.setDetails("+380 44 000 0000", "Kyiv, Ukraine", "Kyiv Lyceum No. 1", 2026, 10.5);
        for (DocumentType type : DocumentType.values()) {
            String label = DocumentStorage.label(type);
            app.addDocument(storage.store(type, label.replace(' ', '-') + ".pdf", samplePdf(label + " of " + name)));
        }
        app.submit(now.minusDays(2));
        if (status == ApplicationStatus.CONDITIONAL_OFFER) {
            app.changeStatus(ApplicationStatus.UNDER_REVIEW, null, staff, now.minusDays(1));
            app.changeStatus(status, "Submit the final school certificate by August 1.", staff, now);
        } else if (status != null) {
            app.changeStatus(status, null, staff, now.minusDays(1));
        }
        applications.save(app);
    }

    /** A one-page PDF with a line of text, so the review screen has something to open. */
    static byte[] samplePdf(String text) {
        String safe = text.replace("\\", "").replace("(", "").replace(")", "");
        String stream = "BT /F1 16 Tf 72 720 Td (Sample " + safe + ") Tj ET";
        String pdf = "%PDF-1.4\n"
                + "1 0 obj <</Type /Catalog /Pages 2 0 R>> endobj\n"
                + "2 0 obj <</Type /Pages /Kids [3 0 R] /Count 1>> endobj\n"
                + "3 0 obj <</Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R"
                + " /Resources <</Font <</F1 5 0 R>>>>>> endobj\n"
                + "4 0 obj <</Length " + stream.length() + ">> stream\n" + stream + "\nendstream endobj\n"
                + "5 0 obj <</Type /Font /Subtype /Type1 /BaseFont /Helvetica>> endobj\n"
                + "trailer <</Root 1 0 R>>\n%%EOF\n";
        return pdf.getBytes(StandardCharsets.US_ASCII);
    }
}
