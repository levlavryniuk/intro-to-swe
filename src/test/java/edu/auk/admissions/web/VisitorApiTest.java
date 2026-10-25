package edu.auk.admissions.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.auk.admissions.domain.Event;
import edu.auk.admissions.domain.Program;
import edu.auk.admissions.repository.InquiryRepository;
import edu.auk.admissions.repository.LeadRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** UC-1 Submit Inquiry and UC-2 Register for Event (public pages, no account). */
class VisitorApiTest extends ApiTestBase {

    @Autowired
    LeadRepository leads;
    @Autowired
    InquiryRepository inquiries;

    private Map<String, Object> inquiry(String name, String email, Long programId, boolean optIn) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("email", email);
        body.put("programId", programId);
        body.put("question", "Is there a scholarship?");
        body.put("promotionalOptIn", optIn);
        return body;
    }

    // ---- UC-1 Submit Inquiry ----

    @Test
    @DisplayName("TC-1.1 Valid inquiry is saved, a lead is created with the opt-in choice")
    void validInquiry() throws Exception {
        Program program = openProgram();
        String email = uniqueEmail("visitor");

        postJson("/api/public/inquiries", inquiry("Olena", email, program.getId(), true), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("received")));

        var lead = leads.findByEmail(email).orElseThrow();
        assertThat(lead.isPromotionalOptIn()).isTrue();
        assertThat(inquiries.findByProgramIdOrderBySubmittedAtDescIdDesc(program.getId())).hasSize(1);
    }

    @Test
    @DisplayName("TC-1.3/1.4/1.5 Empty name, invalid email and no program are reported per field")
    void invalidFields() throws Exception {
        postJson("/api/public/inquiries", inquiry("", "anna@", null, false), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.email").value("Enter a valid email address."))
                .andExpect(jsonPath("$.fieldErrors.programId").value("Choose a program."));
    }

    @Test
    @DisplayName("TC-1.6 A second inquiry from the same email is attached to the same lead")
    void sameLead() throws Exception {
        Program program = openProgram();
        String email = uniqueEmail("visitor");
        postJson("/api/public/inquiries", inquiry("Olena", email, program.getId(), true), null)
                .andExpect(status().isCreated());
        postJson("/api/public/inquiries", inquiry("Olena K.", email.toUpperCase(), program.getId(), false), null)
                .andExpect(status().isCreated());

        var lead = leads.findByEmail(email).orElseThrow();
        assertThat(inquiries.findByProgramIdOrderBySubmittedAtDescIdDesc(program.getId()))
                .hasSize(2)
                .allSatisfy(i -> assertThat(i.getLead().getId()).isEqualTo(lead.getId()));
        assertThat(lead.isPromotionalOptIn()).as("latest choice wins").isFalse();
    }

    // ---- UC-2 Register for Event ----

    private Map<String, Object> registration(String name, String email, boolean optIn) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("email", email);
        body.put("promotionalOptIn", optIn);
        return body;
    }

    @Test
    @DisplayName("TC-2.1 Valid registration is saved and shown in the event's registration list")
    void validRegistration() throws Exception {
        Event event = upcomingEvent();
        String email = uniqueEmail("visitor");

        postJson("/api/public/events/" + event.getId() + "/registrations", registration("Taras", email, true), null)
                .andExpect(status().isCreated());

        mvc.perform(get("/api/staff/events/" + event.getId() + "/registrations").with(asStaff()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value(email))
                .andExpect(jsonPath("$[0].promotionalOptIn").value(true));
    }

    @Test
    @DisplayName("TC-2.2 Missing name or invalid email is refused with a message")
    void invalid() throws Exception {
        Event event = upcomingEvent();
        postJson("/api/public/events/" + event.getId() + "/registrations", registration(" ", "nope", false), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    @DisplayName("TC-2.3 The same email cannot register twice for one event")
    void duplicate() throws Exception {
        Event event = upcomingEvent();
        String email = uniqueEmail("visitor");
        String url = "/api/public/events/" + event.getId() + "/registrations";
        postJson(url, registration("Taras", email, false), null).andExpect(status().isCreated());
        postJson(url, registration("Taras", email, false), null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This email address is already registered for the event."));
    }

    @Test
    @DisplayName("TC-2.4 Registration uses the existing lead of an earlier inquiry")
    void reusesLead() throws Exception {
        Program program = openProgram();
        Event event = upcomingEvent();
        String email = uniqueEmail("visitor");
        postJson("/api/public/inquiries", inquiry("Iryna", email, program.getId(), false), null)
                .andExpect(status().isCreated());
        postJson("/api/public/events/" + event.getId() + "/registrations", registration("Iryna", email, true), null)
                .andExpect(status().isCreated());

        assertThat(leads.findAll()).filteredOn(l -> l.getEmail().equals(email)).hasSize(1);
    }

    @Test
    @DisplayName("TC-2.5 The public events page lists upcoming events only")
    void upcomingOnly() throws Exception {
        Event future = upcomingEvent();
        Event past = events.create(uniqueName("Past webinar"), LocalDateTime.now().minusDays(1), null, "https://x.test");
        mvc.perform(get("/api/public/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(hasItem(future.getId().intValue())))
                .andExpect(jsonPath("$[*].id").value(not(hasItem(past.getId().intValue()))));
    }

    @Test
    @DisplayName("TC-A.1 A visitor cannot reach the applicant or staff API")
    void visitorBlocked() throws Exception {
        mvc.perform(get("/api/staff/applications")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/staff/inquiries")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/applicant/application")).andExpect(status().isUnauthorized());
    }
}
