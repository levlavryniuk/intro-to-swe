package edu.auk.admissions.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.auk.admissions.domain.Program;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/** UC-3 Submit Application, UC-4 Edit, UC-5 Withdraw and UC-6 Track Application Status. */
class ApplicantApiTest extends ApiTestBase {

    private static final String URL = "/api/applicant/application";

    // ---- UC-3 Submit Application ----

    @Test
    @DisplayName("TC-3.1 Complete application with three PDFs is saved as Submitted with a first log entry")
    void submit() throws Exception {
        String applicant = newApplicant();
        Program program = openProgram();

        mvc.perform(applicationForm(URL)
                        .file(pdf("transcript")).file(pdf("proofOfIdentity")).file(pdf("motivationEssay"))
                        .param("programId", program.getId().toString())
                        .with(asApplicant(applicant)).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.application.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.application.program.name").value(program.getName()))
                .andExpect(jsonPath("$.application.documents.length()").value(3))
                .andExpect(jsonPath("$.application.decisionLog.length()").value(1))
                .andExpect(jsonPath("$.application.decisionLog[0].oldStatus").doesNotExist())
                .andExpect(jsonPath("$.application.decisionLog[0].newStatus").value("SUBMITTED"))
                .andExpect(jsonPath("$.application.canEdit").value(true));
    }

    @Test
    @DisplayName("TC-3.2 An applicant cannot submit a second application")
    void onlyOne() throws Exception {
        String applicant = newApplicant();
        Program program = openProgram();
        submitApplication(applicant, program);

        mvc.perform(applicationForm(URL)
                        .file(pdf("transcript")).file(pdf("proofOfIdentity")).file(pdf("motivationEssay"))
                        .param("programId", openProgram().getId().toString())
                        .with(asApplicant(applicant)).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("already have an application")));
    }

    @Test
    @DisplayName("TC-3.3 Applying after the program deadline is refused")
    void closedProgram() throws Exception {
        Program closed = programs.add(uniqueName("Closed program"), LocalDate.now().minusDays(1));
        mvc.perform(applicationForm(URL)
                        .file(pdf("transcript")).file(pdf("proofOfIdentity")).file(pdf("motivationEssay"))
                        .param("programId", closed.getId().toString())
                        .with(asApplicant(newApplicant())).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("closed")));
    }

    @Test
    @DisplayName("TC-3.4 A file that is not a PDF is rejected with a message")
    void notAPdf() throws Exception {
        mvc.perform(applicationForm(URL)
                        .file(pdf("transcript")).file(notPdf("proofOfIdentity")).file(pdf("motivationEssay"))
                        .param("programId", openProgram().getId().toString())
                        .with(asApplicant(newApplicant())).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("The proof of identity must be a PDF file."));
    }

    @Test
    @DisplayName("TC-3.5 A file over 15 MB is rejected with a message")
    void tooLarge() throws Exception {
        byte[] big = new byte[15 * 1024 * 1024 + 1];
        System.arraycopy("%PDF-".getBytes(), 0, big, 0, 5);
        mvc.perform(applicationForm(URL)
                        .file(new MockMultipartFile("transcript", "scan.pdf", "application/pdf", big))
                        .file(pdf("proofOfIdentity")).file(pdf("motivationEssay"))
                        .param("programId", openProgram().getId().toString())
                        .with(asApplicant(newApplicant())).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("The academic transcript is larger than 15 MB."));
    }

    @Test
    @DisplayName("TC-3.6 The application is not saved while a document is missing")
    void missingDocument() throws Exception {
        String applicant = newApplicant();
        mvc.perform(applicationForm(URL)
                        .file(pdf("transcript")).file(pdf("proofOfIdentity"))
                        .param("programId", openProgram().getId().toString())
                        .with(asApplicant(applicant)).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Upload the motivation essay."));
        mvc.perform(get(URL).with(asApplicant(applicant))).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("TC-3.7 Empty or invalid fields are reported per field")
    void invalidFields() throws Exception {
        mvc.perform(applicationForm(URL, "phone", "", "graduationYear", "1900")
                        .file(pdf("transcript")).file(pdf("proofOfIdentity")).file(pdf("motivationEssay"))
                        .with(asApplicant(newApplicant())).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.phone").value("Enter your phone number."))
                .andExpect(jsonPath("$.fieldErrors.graduationYear").exists());
    }

    // ---- UC-4 Edit Application ----

    @Test
    @DisplayName("TC-4.1 While Submitted, the applicant can change fields and replace a document")
    void edit() throws Exception {
        String applicant = newApplicant();
        submitApplication(applicant, openProgram());

        mvc.perform(applicationForm(URL + "/edit", "lastSchool", "Lviv Gymnasium")
                        .file(new MockMultipartFile("motivationEssay", "essay-v2.pdf", "application/pdf",
                                "%PDF-1.4 new essay".getBytes()))
                        .with(asApplicant(applicant)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application.lastSchool").value("Lviv Gymnasium"))
                .andExpect(jsonPath("$.application.documents.length()").value(3))
                .andExpect(jsonPath("$.application.documents[?(@.type=='MOTIVATION_ESSAY')].fileName").value("essay-v2.pdf"));
    }

    @Test
    @DisplayName("TC-4.2 Editing is refused once review has started")
    void editAfterReview() throws Exception {
        String applicant = newApplicant();
        long id = submitApplication(applicant, openProgram());
        recordDecision(id, "UNDER_REVIEW", null).andExpect(status().isOk());

        mvc.perform(applicationForm(URL + "/edit").with(asApplicant(applicant)).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("only be edited while its status is Submitted")));
    }

    @Test
    @DisplayName("TC-4.3 Editing is refused after the program deadline")
    void editAfterDeadline() throws Exception {
        String applicant = newApplicant();
        Program program = openProgram();
        submitApplication(applicant, program);
        programs.setDeadline(program.getId(), LocalDate.now().minusDays(1));

        mvc.perform(applicationForm(URL + "/edit").with(asApplicant(applicant)).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("deadline")));
    }

    @Test
    @DisplayName("TC-4.4 An edit that empties a required field is refused and nothing changes")
    void editInvalid() throws Exception {
        String applicant = newApplicant();
        submitApplication(applicant, openProgram());

        mvc.perform(applicationForm(URL + "/edit", "address", " ").with(asApplicant(applicant)).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.address").exists());
        mvc.perform(get(URL).with(asApplicant(applicant)))
                .andExpect(jsonPath("$.application.address").value("Kyiv, Ukraine"));
    }

    @Test
    @DisplayName("TC-4.5 A replacement that is not a PDF is rejected and the old file stays")
    void editBadFile() throws Exception {
        String applicant = newApplicant();
        submitApplication(applicant, openProgram());

        mvc.perform(applicationForm(URL + "/edit").file(notPdf("transcript")).with(asApplicant(applicant)).with(csrf()))
                .andExpect(status().isConflict());
        mvc.perform(get(URL).with(asApplicant(applicant)))
                .andExpect(jsonPath("$.application.documents[?(@.type=='TRANSCRIPT')].fileName").value("transcript.pdf"));
    }

    // ---- UC-5 Withdraw Application ----

    @Test
    @DisplayName("TC-5.1 Withdraw sets Withdrawn, logs the applicant, and the application can no longer be edited")
    void withdraw() throws Exception {
        String applicant = newApplicant();
        submitApplication(applicant, openProgram());

        mvc.perform(post(URL + "/withdraw").with(asApplicant(applicant)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application.status").value("WITHDRAWN"))
                .andExpect(jsonPath("$.application.stage").value("CLOSED"))
                .andExpect(jsonPath("$.application.decisionLog[1].changedBy").value(containsString("Applicant")))
                .andExpect(jsonPath("$.application.canEdit").value(false))
                .andExpect(jsonPath("$.application.canWithdraw").value(false));

        mvc.perform(applicationForm(URL + "/edit").with(asApplicant(applicant)).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("TC-5.3 Withdraw is refused after a decision")
    void withdrawAfterDecision() throws Exception {
        String applicant = newApplicant();
        long id = submitApplication(applicant, openProgram());
        recordDecision(id, "WAITLISTED", null).andExpect(status().isOk());

        mvc.perform(post(URL + "/withdraw").with(asApplicant(applicant)).with(csrf()))
                .andExpect(status().isConflict());
    }

    // ---- UC-6 Track Application Status ----

    @Test
    @DisplayName("TC-6.1 Without an application the page invites the applicant to apply")
    void noApplication() throws Exception {
        mvc.perform(get(URL).with(asApplicant(newApplicant())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("You have not submitted an application yet."));
    }

    @Test
    @DisplayName("TC-6.2/6.3 Status, offer condition and decision emails are shown")
    void trackStatus() throws Exception {
        String applicant = newApplicant();
        long id = submitApplication(applicant, openProgram());
        recordDecision(id, "CONDITIONAL_OFFER", "Send the final certificate by August 1").andExpect(status().isOk());

        mvc.perform(get(URL).with(asApplicant(applicant)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application.status").value("CONDITIONAL_OFFER"))
                .andExpect(jsonPath("$.application.condition").value("Send the final certificate by August 1"))
                .andExpect(jsonPath("$.decisionEmails.length()").value(1))
                .andExpect(jsonPath("$.decisionEmails[0].body").value(containsString("Conditional offer")))
                .andExpect(jsonPath("$.decisionEmails[0].body").value(containsString("August 1")));
    }

    // ---- Access control (NFR-01) ----

    @Test
    @DisplayName("TC-A.3 Staff cannot use the applicant pages")
    void staffCannotApply() throws Exception {
        mvc.perform(get(URL).with(asStaff())).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-A.4 Requests without the CSRF token are refused")
    void csrfRequired() throws Exception {
        String applicant = newApplicant();
        submitApplication(applicant, openProgram());
        mvc.perform(post(URL + "/withdraw").with(asApplicant(applicant))).andExpect(status().isForbidden());
        mvc.perform(get(URL).with(asApplicant(applicant))).andExpect(jsonPath("$.application.status").value("SUBMITTED"));
    }
}
