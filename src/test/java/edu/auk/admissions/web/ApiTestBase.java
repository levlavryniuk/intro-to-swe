package edu.auk.admissions.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.auk.admissions.domain.DeliveryStatus;
import edu.auk.admissions.domain.Event;
import edu.auk.admissions.domain.Program;
import edu.auk.admissions.integration.MailGateway;
import edu.auk.admissions.integration.SisGateway;
import edu.auk.admissions.security.AccountService;
import edu.auk.admissions.service.EventService;
import edu.auk.admissions.service.ProgramService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Runs the whole module (Spring Boot, H2, security) and calls it the way the web pages do.
 * Mail and SIS gateways are mocks so tests can check what was sent and simulate failures.
 * Every test makes its own users, programs and leads, so tests do not depend on each other.
 */
@SpringBootTest(properties = {"app.demo-data=false", "app.storage.dir=target/test-uploads"})
@AutoConfigureMockMvc
abstract class ApiTestBase {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected ObjectMapper json;
    @Autowired
    protected AccountService accounts;
    @Autowired
    protected ProgramService programs;
    @Autowired
    protected EventService events;

    @MockBean
    protected MailGateway mail;
    @MockBean
    protected SisGateway sis;

    protected String staffEmail;

    @BeforeEach
    void baseSetUp() {
        when(mail.send(any())).thenReturn(DeliveryStatus.SENT);
        when(sis.sendAcceptedApplicant(any())).thenReturn(true);
        staffEmail = uniqueEmail("staff");
        accounts.createStaff("Staff " + staffEmail, staffEmail, "password-123");
    }

    protected static String uniqueEmail(String prefix) {
        return prefix + "-" + SEQ.incrementAndGet() + "-" + System.nanoTime() + "@example.com";
    }

    protected static String uniqueName(String prefix) {
        return prefix + " " + SEQ.incrementAndGet() + "-" + System.nanoTime();
    }

    protected RequestPostProcessor asStaff() {
        return user(staffEmail).roles("STAFF");
    }

    protected static RequestPostProcessor asApplicant(String email) {
        return user(email).roles("APPLICANT");
    }

    protected String newApplicant() {
        String email = uniqueEmail("applicant");
        accounts.signUpApplicant("Applicant " + email.substring(0, 12), email, "password-123");
        return email;
    }

    protected Program openProgram() {
        return programs.add(uniqueName("Program"), LocalDate.now().plusDays(30));
    }

    protected Event upcomingEvent() {
        return events.create(uniqueName("Open Day"), LocalDateTime.now().plusDays(10), "Campus", null);
    }

    protected String toJson(Object body) throws Exception {
        return json.writeValueAsString(body);
    }

    protected ResultActions postJson(String url, Object body, RequestPostProcessor who) throws Exception {
        var request = post(url).contentType(MediaType.APPLICATION_JSON).content(toJson(body)).with(csrf());
        if (who != null) {
            request.with(who);
        }
        return mvc.perform(request);
    }

    protected static MockMultipartFile pdf(String field) {
        return new MockMultipartFile(field, field + ".pdf", "application/pdf",
                ("%PDF-1.4\n" + field + " test file\n%%EOF").getBytes(StandardCharsets.US_ASCII));
    }

    protected static MockMultipartFile notPdf(String field) {
        return new MockMultipartFile(field, field + ".pdf", "application/pdf",
                "PK this is really a Word file".getBytes(StandardCharsets.US_ASCII));
    }

    /** The application form as the applicant page sends it. Overrides are name/value pairs. */
    protected static MockMultipartHttpServletRequestBuilder applicationForm(String url, String... overrides) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("phone", "+380 44 123 4567");
        fields.put("address", "Kyiv, Ukraine");
        fields.put("lastSchool", "Kyiv Lyceum No. 1");
        fields.put("graduationYear", "2026");
        fields.put("gradeAverage", "10.5");
        for (int i = 0; i < overrides.length; i += 2) {
            fields.put(overrides[i], overrides[i + 1]);
        }
        MockMultipartHttpServletRequestBuilder form = multipart(url);
        fields.forEach(form::param);
        return form;
    }

    /** UC-3 basic flow: submits a complete application and returns its id. */
    protected long submitApplication(String applicantEmail, Program program) throws Exception {
        String body = mvc.perform(applicationForm("/api/applicant/application")
                        .file(pdf("transcript")).file(pdf("proofOfIdentity")).file(pdf("motivationEssay"))
                        .param("programId", program.getId().toString())
                        .with(asApplicant(applicantEmail)).with(csrf()))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).path("application").path("id").asLong();
    }

    protected ResultActions recordDecision(long applicationId, String status, String condition) throws Exception {
        var body = new java.util.HashMap<String, Object>();
        body.put("status", status);
        body.put("condition", condition);
        return postJson("/api/staff/applications/" + applicationId + "/status", body, asStaff());
    }
}
