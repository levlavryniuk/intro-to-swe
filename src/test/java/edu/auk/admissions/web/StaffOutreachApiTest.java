package edu.auk.admissions.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import edu.auk.admissions.domain.DeliveryStatus;
import edu.auk.admissions.domain.Event;
import edu.auk.admissions.domain.Lead;
import edu.auk.admissions.domain.Program;
import edu.auk.admissions.repository.LeadRepository;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/** UC-7, UC-10 to UC-14: inquiries, email, opt-outs, events, programs and the pipeline overview. */
class StaffOutreachApiTest extends ApiTestBase {

    @Autowired
    LeadRepository leads;

    private void inquire(String name, String email, Program program, boolean optIn) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("email", email);
        body.put("programId", program.getId());
        body.put("promotionalOptIn", optIn);
        postJson("/api/public/inquiries", body, null).andExpect(status().isCreated());
    }

    private void register(Event event, String email, boolean optIn) throws Exception {
        postJson("/api/public/events/" + event.getId() + "/registrations",
                Map.of("name", "Guest", "email", email, "promotionalOptIn", optIn), null)
                .andExpect(status().isCreated());
    }

    private long leadId(String email) {
        return leads.findByEmail(Lead.normalizeEmail(email)).orElseThrow().getId();
    }

    // ---- UC-7 Manage Inquiries ----

    @Test
    @DisplayName("TC-7.1/7.2 Inquiries are listed newest first and can be filtered by program")
    void listInquiries() throws Exception {
        Program program = openProgram();
        Program empty = openProgram();
        String first = uniqueEmail("first");
        String second = uniqueEmail("second");
        inquire("First", first, program, true);
        inquire("Second", second, program, false);

        mvc.perform(get("/api/staff/inquiries").param("programId", program.getId().toString()).with(asStaff()))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].email").value(second))
                .andExpect(jsonPath("$[1].email").value(first))
                .andExpect(jsonPath("$[1].promotionalOptIn").value(true));
        mvc.perform(get("/api/staff/inquiries").param("programId", empty.getId().toString()).with(asStaff()))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("TC-7.3 Staff set the interest level and can change it later")
    void rateInquiry() throws Exception {
        Program program = openProgram();
        inquire("Rated", uniqueEmail("rated"), program, false);
        String list = mvc.perform(get("/api/staff/inquiries").param("programId", program.getId().toString())
                .with(asStaff())).andReturn().getResponse().getContentAsString();
        long id = json.readTree(list).get(0).get("id").asLong();

        for (String level : new String[] {"HIGH", "LOW"}) {
            mvc.perform(put("/api/staff/inquiries/" + id + "/interest").with(asStaff()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content("{\"interest\":\"" + level + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.interest").value(level));
        }
    }

    // ---- UC-10 Send Email ----

    @Test
    @DisplayName("TC-10.1 A personal email goes to a lead even without opt-in")
    void personalEmail() throws Exception {
        String email = uniqueEmail("nooptin");
        inquire("No opt-in", email, openProgram(), false);

        postJson("/api/staff/leads/" + leadId(email) + "/email", Map.of("subject", "Your question", "body", "Hello"), asStaff())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipientEmail").value(email))
                .andExpect(jsonPath("$.status").value("SENT"));
    }

    @Test
    @DisplayName("TC-10.2 A group email to a program goes only to leads who opted in")
    void groupByProgram() throws Exception {
        Program program = openProgram();
        String in = uniqueEmail("in");
        inquire("In", in, program, true);
        inquire("Out", uniqueEmail("out"), program, false);

        mvc.perform(get("/api/staff/emails/recipients").param("programId", program.getId().toString()).with(asStaff()))
                .andExpect(jsonPath("$.count").value(1));
        postJson("/api/staff/emails/group",
                Map.of("programId", program.getId(), "subject", "Open day", "body", "Come visit"), asStaff())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipients").value(1))
                .andExpect(jsonPath("$.sent").value(1))
                .andExpect(jsonPath("$.results[0].recipientEmail").value(in));
    }

    @Test
    @DisplayName("TC-10.3 A group email to an event goes only to registered leads who opted in")
    void groupByEvent() throws Exception {
        Event event = upcomingEvent();
        register(event, uniqueEmail("in"), true);
        register(event, uniqueEmail("in"), true);
        register(event, uniqueEmail("out"), false);

        postJson("/api/staff/emails/group",
                Map.of("eventId", event.getId(), "subject", "Reminder", "body", "See you tomorrow"), asStaff())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipients").value(2));
        verify(mail, times(2)).send(any());
    }

    @Test
    @DisplayName("TC-10.4 A group with nobody opted in is refused with a message")
    void emptyGroup() throws Exception {
        Program program = openProgram();
        inquire("Out", uniqueEmail("out"), program, false);
        postJson("/api/staff/emails/group",
                Map.of("programId", program.getId(), "subject", "Hi", "body", "Hello"), asStaff())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Nobody in this group has opted in to promotional email."));
    }

    @Test
    @DisplayName("TC-10.5 Subject and message are required")
    void subjectAndBodyRequired() throws Exception {
        String email = uniqueEmail("lead");
        inquire("Lead", email, openProgram(), true);
        postJson("/api/staff/leads/" + leadId(email) + "/email", Map.of("subject", "", "body", " "), asStaff())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.subject").value("Enter a subject."))
                .andExpect(jsonPath("$.fieldErrors.body").value("Enter a message."));
    }

    @Test
    @DisplayName("TC-10.6 The result shows which emails were sent and which failed")
    void partialFailure() throws Exception {
        Program program = openProgram();
        String good = uniqueEmail("good");
        String bad = uniqueEmail("bad");
        inquire("Good", good, program, true);
        inquire("Bad", bad, program, true);
        when(mail.send(argThat(m -> m != null && m.getRecipientEmail().equals(bad)))).thenReturn(DeliveryStatus.FAILED);

        String body = postJson("/api/staff/emails/group",
                Map.of("programId", program.getId(), "subject", "News", "body", "Hello"), asStaff())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sent").value(1))
                .andExpect(jsonPath("$.failed").value(1))
                .andReturn().getResponse().getContentAsString();
        for (JsonNode result : json.readTree(body).get("results")) {
            String expected = result.get("recipientEmail").asText().equals(bad) ? "FAILED" : "SENT";
            assertThat(result.get("status").asText()).isEqualTo(expected);
        }
    }

    // ---- UC-11 Remove Lead Opt-in ----

    @Test
    @DisplayName("TC-11.1/11.2 Removing an opt-in takes the lead out of group emails; doing it again changes nothing")
    void removeOptIn() throws Exception {
        Program program = openProgram();
        String email = uniqueEmail("optout");
        inquire("Opt out", email, program, true);
        long lead = leadId(email);

        postJson("/api/staff/leads/" + lead + "/remove-opt-in", Map.of(), asStaff())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changed").value(true));
        mvc.perform(get("/api/staff/emails/recipients").param("programId", program.getId().toString()).with(asStaff()))
                .andExpect(jsonPath("$.count").value(0));
        postJson("/api/staff/leads/" + lead + "/remove-opt-in", Map.of(), asStaff())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changed").value(false))
                .andExpect(jsonPath("$.message").value("This lead has already opted out. Nothing to change."));
    }

    // ---- UC-12 Manage Events ----

    @Test
    @DisplayName("TC-12.1/12.2 Events with a location or only an online link appear on the public page")
    void createEvents() throws Exception {
        String onSite = uniqueName("Open Day");
        String online = uniqueName("Webinar");
        postJson("/api/staff/events", Map.of("name", onSite, "startsAt", "2099-05-01T11:00:00", "location", "Campus"), asStaff())
                .andExpect(status().isCreated());
        postJson("/api/staff/events", Map.of("name", online, "startsAt", "2099-05-02T18:00:00",
                "onlineLink", "https://meet.example.com/x"), asStaff())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.location").doesNotExist());

        mvc.perform(get("/api/public/events"))
                .andExpect(jsonPath("$[*].name").value(hasItem(onSite)))
                .andExpect(jsonPath("$[*].name").value(hasItem(online)));
    }

    @Test
    @DisplayName("TC-12.3 An event without a name, a date, or both location and link is refused")
    void invalidEvent() throws Exception {
        postJson("/api/staff/events", Map.of("location", "Campus"), asStaff())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.startsAt").exists());
        postJson("/api/staff/events", Map.of("name", "No place", "startsAt", "2099-05-01T11:00:00"), asStaff())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Enter a location or an online link."));
    }

    @Test
    @DisplayName("TC-12.4 An event with no registrations shows an empty list")
    void emptyRegistrations() throws Exception {
        Event event = upcomingEvent();
        mvc.perform(get("/api/staff/events/" + event.getId() + "/registrations").with(asStaff()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ---- UC-13 Manage Programs and Deadlines ----

    @Test
    @DisplayName("TC-13.1/13.2 Programs can be added; a duplicate name is refused")
    void addProgram() throws Exception {
        String name = uniqueName("Data Science");
        postJson("/api/staff/programs", Map.of("name", name), asStaff())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.open").value(false));
        postJson("/api/staff/programs", Map.of("name", name.toUpperCase()), asStaff())
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("TC-13.3/13.4 Setting a later deadline reopens a closed program")
    void deadline() throws Exception {
        Program program = programs.add(uniqueName("Closed"), LocalDate.now().minusDays(1));
        String applicant = newApplicant();

        mvc.perform(put("/api/staff/programs/" + program.getId() + "/deadline").with(asStaff()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deadline\":\"" + LocalDate.now().plusDays(7) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.open").value(true));
        long id = submitApplication(applicant, program);
        assertThat(id).isPositive();
    }

    // ---- UC-14 View Pipeline Overview ----

    @Test
    @DisplayName("TC-14.1/14.3 Overview counts inquiries, registrations and applications per status and stage")
    void overview() throws Exception {
        JsonNode before = overviewNow();

        Program program = openProgram();
        inquire("Counted", uniqueEmail("counted"), program, false);
        register(upcomingEvent(), uniqueEmail("counted"), false);
        submitApplication(newApplicant(), program);
        long accepted = submitApplication(newApplicant(), program);
        recordDecision(accepted, "ACCEPTED", null);
        long rejected = submitApplication(newApplicant(), program);
        recordDecision(rejected, "REJECTED", null);

        JsonNode after = overviewNow();
        assertThat(after.get("inquiries").asLong() - before.get("inquiries").asLong()).isEqualTo(1);
        assertThat(after.get("eventRegistrations").asLong() - before.get("eventRegistrations").asLong()).isEqualTo(1);
        assertThat(stage(after, "APPLIED") - stage(before, "APPLIED")).isEqualTo(1);
        assertThat(stage(after, "ADMITTED") - stage(before, "ADMITTED")).isEqualTo(1);
        assertThat(stage(after, "CLOSED") - stage(before, "CLOSED")).isEqualTo(1);
        assertThat(statusCount(after, "CLOSED", "REJECTED") - statusCount(before, "CLOSED", "REJECTED")).isEqualTo(1);
    }

    private JsonNode overviewNow() throws Exception {
        return json.readTree(mvc.perform(get("/api/staff/overview").with(asStaff()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private static long stage(JsonNode overview, String stage) {
        for (JsonNode s : overview.get("stages")) {
            if (s.get("stage").asText().equals(stage)) {
                return s.get("total").asLong();
            }
        }
        throw new AssertionError("No stage " + stage);
    }

    private static long statusCount(JsonNode overview, String stage, String status) {
        for (JsonNode s : overview.get("stages")) {
            if (s.get("stage").asText().equals(stage)) {
                return s.get("statuses").get(status).asLong();
            }
        }
        throw new AssertionError("No stage " + stage);
    }
}
