package edu.auk.admissions.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.auk.admissions.domain.Program;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** UC-8 Review Application, UC-9 Record Decision, UC-15 Transfer Accepted Applicant to SIS. */
class StaffApplicationApiTest extends ApiTestBase {

    private static final String URL = "/api/staff/applications";

    // ---- UC-8 Review Application ----

    @Test
    @DisplayName("TC-8.1 The list can be narrowed by program, by status, or both")
    void filters() throws Exception {
        Program a = openProgram();
        Program b = openProgram();
        long a1 = submitApplication(newApplicant(), a);
        long a2 = submitApplication(newApplicant(), a);
        long b1 = submitApplication(newApplicant(), b);
        recordDecision(a2, "UNDER_REVIEW", null);

        mvc.perform(get(URL).param("programId", a.getId().toString()).with(asStaff()))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].id").value(not(hasItem((int) b1))));
        mvc.perform(get(URL).param("programId", a.getId().toString()).param("status", "UNDER_REVIEW").with(asStaff()))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(a2));
        mvc.perform(get(URL).param("status", "SUBMITTED").with(asStaff()))
                .andExpect(jsonPath("$[*].status").value(everyItem(is("SUBMITTED"))))
                .andExpect(jsonPath("$[*].id").value(hasItem((int) a1)));
        mvc.perform(get(URL).param("programId", b.getId().toString()).param("status", "ACCEPTED").with(asStaff()))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("TC-8.2/8.5 The review screen shows the applicant, program, status, documents and decision log")
    void detail() throws Exception {
        Program program = openProgram();
        long id = submitApplication(newApplicant(), program);

        mvc.perform(get(URL + "/" + id).with(asStaff()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.program.name").value(program.getName()))
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.lastSchool").value("Kyiv Lyceum No. 1"))
                .andExpect(jsonPath("$.documents.length()").value(3))
                .andExpect(jsonPath("$.decisionLog[0].newStatus").value("SUBMITTED"))
                .andExpect(jsonPath("$.decisionLog[0].changedBy").exists())
                .andExpect(jsonPath("$.decisionLog[0].changedAt").exists());
    }

    @Test
    @DisplayName("TC-8.3 Staff can open each of the three documents as a PDF")
    void openDocuments() throws Exception {
        long id = submitApplication(newApplicant(), openProgram());
        for (String type : new String[] {"TRANSCRIPT", "PROOF_OF_IDENTITY", "MOTIVATION_ESSAY"}) {
            mvc.perform(get(URL + "/" + id + "/documents/" + type).with(asStaff()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                    .andExpect(content().string(containsString("%PDF-")));
        }
    }

    @Test
    @DisplayName("TC-8.4 Documents are not reachable by visitors or applicants")
    void documentsProtected() throws Exception {
        String owner = newApplicant();
        long id = submitApplication(owner, openProgram());
        String docUrl = URL + "/" + id + "/documents/TRANSCRIPT";

        mvc.perform(get(docUrl)).andExpect(status().isUnauthorized());
        mvc.perform(get(docUrl).with(asApplicant(owner))).andExpect(status().isForbidden());
        mvc.perform(get(docUrl).with(asApplicant(newApplicant()))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-A.2 An applicant cannot reach the staff API")
    void applicantBlocked() throws Exception {
        String applicant = newApplicant();
        mvc.perform(get(URL).with(asApplicant(applicant))).andExpect(status().isForbidden());
        mvc.perform(get("/api/staff/overview").with(asApplicant(applicant))).andExpect(status().isForbidden());
    }

    // ---- UC-9 Record Decision ----

    @Test
    @DisplayName("TC-9.1 A new status is saved, logged with the staff member and emailed to the applicant")
    void recordDecisionBasic() throws Exception {
        long id = submitApplication(newApplicant(), openProgram());

        recordDecision(id, "UNDER_REVIEW", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application.status").value("UNDER_REVIEW"))
                .andExpect(jsonPath("$.application.decisionLog.length()").value(2))
                .andExpect(jsonPath("$.application.decisionLog[1].oldStatus").value("SUBMITTED"))
                .andExpect(jsonPath("$.application.decisionLog[1].newStatus").value("UNDER_REVIEW"))
                .andExpect(jsonPath("$.application.decisionLog[1].changedBy").value("Staff " + staffEmail))
                .andExpect(jsonPath("$.email.status").value("SENT"))
                .andExpect(jsonPath("$.email.decisionEmail").value(true));
        verify(mail).send(any());
    }

    @Test
    @DisplayName("TC-9.2 A conditional offer without a condition is refused")
    void conditionRequired() throws Exception {
        long id = submitApplication(newApplicant(), openProgram());
        recordDecision(id, "CONDITIONAL_OFFER", "")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Enter the condition of the conditional offer."));
        mvc.perform(get(URL + "/" + id).with(asStaff())).andExpect(jsonPath("$.status").value("SUBMITTED"));
        verify(mail, never()).send(any());
    }

    @Test
    @DisplayName("TC-9.3 A conditional offer keeps its condition and the email includes it")
    void conditionalOffer() throws Exception {
        long id = submitApplication(newApplicant(), openProgram());
        recordDecision(id, "CONDITIONAL_OFFER", "Pass the English test")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application.condition").value("Pass the English test"))
                .andExpect(jsonPath("$.email.body").value(containsString("Pass the English test")));
    }

    @Test
    @DisplayName("TC-9.4 An earlier decision can be changed, e.g. Waitlisted to Accepted")
    void changeDecision() throws Exception {
        long id = submitApplication(newApplicant(), openProgram());
        recordDecision(id, "WAITLISTED", null).andExpect(status().isOk());
        recordDecision(id, "ACCEPTED", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.application.decisionLog[2].oldStatus").value("WAITLISTED"));
    }

    @Test
    @DisplayName("TC-9.5 If the email fails, the status stays saved and staff are told")
    void emailFails() throws Exception {
        long id = submitApplication(newApplicant(), openProgram());
        when(mail.send(any())).thenThrow(new IllegalStateException("Mail server not reachable"));

        recordDecision(id, "REJECTED", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application.status").value("REJECTED"))
                .andExpect(jsonPath("$.email.status").value("FAILED"))
                .andExpect(jsonPath("$.email.failureReason").value("Mail server not reachable"));
        mvc.perform(get(URL + "/" + id).with(asStaff())).andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @DisplayName("TC-9.6 Enrolled can only follow an admission")
    void enrolledAfterAdmission() throws Exception {
        long id = submitApplication(newApplicant(), openProgram());
        recordDecision(id, "ENROLLED", null).andExpect(status().isConflict());
        recordDecision(id, "ACCEPTED", null).andExpect(status().isOk());
        recordDecision(id, "ENROLLED", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application.stage").value("ENROLLED"));
    }

    // ---- UC-15 Transfer Accepted Applicant to SIS ----

    @Test
    @DisplayName("TC-15.1 Accepting an applicant passes them to the SIS and marks the transfer done")
    void sisTransfer() throws Exception {
        long id = submitApplication(newApplicant(), openProgram());
        recordDecision(id, "ACCEPTED", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application.sisTransferDone").value(true))
                .andExpect(jsonPath("$.sisWarning").doesNotExist());
        verify(sis).sendAcceptedApplicant(any());
    }

    @Test
    @DisplayName("TC-15.2 If the SIS cannot be reached, the applicant stays Accepted, transfer pending, with a warning")
    void sisDown() throws Exception {
        long id = submitApplication(newApplicant(), openProgram());
        when(sis.sendAcceptedApplicant(any())).thenThrow(new IllegalStateException("connection refused"));

        recordDecision(id, "ACCEPTED", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.application.sisTransferDone").value(false))
                .andExpect(jsonPath("$.sisWarning").value(containsString("pending")));
    }

    @Test
    @DisplayName("TC-15.3 Statuses other than Accepted are not sent to the SIS")
    void noTransferForOtherStatuses() throws Exception {
        long id = submitApplication(newApplicant(), openProgram());
        recordDecision(id, "WAITLISTED", null).andExpect(status().isOk());
        verify(sis, never()).sendAcceptedApplicant(any());
    }
}
