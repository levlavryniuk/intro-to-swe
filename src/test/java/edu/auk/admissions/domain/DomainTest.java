package edu.auk.admissions.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** Unit tests of the rules kept in the domain classes. */
class DomainTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);
    private static final LocalDateTime NOW = TODAY.atTime(12, 0);

    private Program program;
    private Applicant applicant;
    private StaffMember staff;

    @BeforeEach
    void setUp() {
        program = new Program("Computer Science", TODAY.plusDays(10));
        applicant = withId(new Applicant("Ann Applicant", "ann@example.com"), 1L);
        staff = withId(new StaffMember("Sam Staff", "sam@auk.edu.ua"), 2L);
    }

    @Test
    @DisplayName("TC-3.8/TC-13.4 Program is open up to and including the deadline date, closed after it and without one")
    void programDeadline() {
        Program p = new Program("Law", TODAY);
        assertThat(p.isOpen(TODAY)).isTrue();
        assertThat(p.isOpen(TODAY.plusDays(1))).isFalse();
        assertThat(new Program("No deadline", null).isOpen(TODAY)).isFalse();
        p.changeDeadline(TODAY.plusDays(5));
        assertThat(p.isOpen(TODAY.plusDays(1))).isTrue();
    }

    @Test
    @DisplayName("Statuses map to the pipeline stages from FR-07")
    void stages() {
        assertThat(ApplicationStatus.SUBMITTED.stage()).isEqualTo(Stage.APPLIED);
        assertThat(ApplicationStatus.UNDER_REVIEW.stage()).isEqualTo(Stage.APPLIED);
        assertThat(ApplicationStatus.WAITLISTED.stage()).isEqualTo(Stage.APPLIED);
        assertThat(ApplicationStatus.ACCEPTED.stage()).isEqualTo(Stage.ADMITTED);
        assertThat(ApplicationStatus.CONDITIONAL_OFFER.stage()).isEqualTo(Stage.ADMITTED);
        assertThat(ApplicationStatus.ENROLLED.stage()).isEqualTo(Stage.ENROLLED);
        assertThat(ApplicationStatus.REJECTED.stage()).isEqualTo(Stage.CLOSED);
        assertThat(ApplicationStatus.WITHDRAWN.stage()).isEqualTo(Stage.CLOSED);
    }

    @Test
    @DisplayName("TC-3.5 Document is valid only as a PDF of at most 15 MB (boundary: exactly 15 MB passes)")
    void documentValidity() {
        assertThat(pdf(DocumentType.TRANSCRIPT, Document.MAX_SIZE_BYTES).isValid()).isTrue();
        assertThat(pdf(DocumentType.TRANSCRIPT, Document.MAX_SIZE_BYTES + 1).isValid()).isFalse();
        assertThat(pdf(DocumentType.TRANSCRIPT, 0).isValid()).isFalse();
        assertThat(new Document(DocumentType.TRANSCRIPT, "a.png", "image/png", 10, "x").isValid()).isFalse();
    }

    @Test
    @DisplayName("Application cannot be submitted until all fields and all three documents are there")
    void submitNeedsEverything() {
        Application app = new Application(applicant, program);
        app.setDetails("+380", "Kyiv", "Lyceum 1", 2026, 10.5);
        app.addDocument(pdf(DocumentType.TRANSCRIPT, 100));
        app.addDocument(pdf(DocumentType.PROOF_OF_IDENTITY, 100));
        assertThat(app.isComplete()).isFalse();
        assertThatThrownBy(() -> app.submit(NOW)).isInstanceOf(BusinessRuleException.class);

        app.addDocument(pdf(DocumentType.MOTIVATION_ESSAY, 100));
        StatusChange first = app.submit(NOW);
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
        assertThat(first.getOldStatus()).isNull();
        assertThat(first.getNewStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
        assertThat(first.getChangedBy()).isSameAs(applicant);
    }

    @Test
    @DisplayName("A new document replaces the earlier one of the same type")
    void documentReplaced() {
        Application app = new Application(applicant, program);
        app.addDocument(pdf(DocumentType.TRANSCRIPT, 100));
        app.addDocument(pdf(DocumentType.TRANSCRIPT, 200));
        assertThat(app.getDocuments()).hasSize(1);
        assertThat(app.documentOf(DocumentType.TRANSCRIPT)).get().extracting(Document::getSizeBytes).isEqualTo(200L);
    }

    @Test
    @DisplayName("Application is refused after the program deadline")
    void submitAfterDeadline() {
        Application app = complete(new Application(applicant, new Program("Closed", TODAY.minusDays(1))));
        assertThatThrownBy(() -> app.submit(NOW))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("closed");
    }

    @Test
    @DisplayName("Editing is allowed only while Submitted and before the deadline")
    void canBeEdited() {
        Application app = submitted();
        assertThat(app.canBeEdited(TODAY)).isTrue();
        assertThat(app.canBeEdited(TODAY.plusDays(11))).isFalse();
        app.changeStatus(ApplicationStatus.UNDER_REVIEW, null, staff, NOW);
        assertThat(app.canBeEdited(TODAY)).isFalse();
    }

    @Test
    @DisplayName("TC-8.5 Every status change is logged with who made it and when")
    void decisionLog() {
        Application app = submitted();
        app.changeStatus(ApplicationStatus.UNDER_REVIEW, null, staff, NOW.plusHours(1));
        app.changeStatus(ApplicationStatus.WAITLISTED, null, staff, NOW.plusHours(2));
        app.changeStatus(ApplicationStatus.ACCEPTED, null, staff, NOW.plusHours(3));

        assertThat(app.getStatusChanges()).hasSize(4);
        StatusChange last = app.getStatusChanges().get(3);
        assertThat(last.getOldStatus()).isEqualTo(ApplicationStatus.WAITLISTED);
        assertThat(last.getNewStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
        assertThat(last.getChangedBy()).isSameAs(staff);
        assertThat(last.getChangedAt()).isEqualTo(NOW.plusHours(3));
    }

    @Test
    @DisplayName("Conditional offer needs a condition; other statuses clear it")
    void conditionalOffer() {
        Application app = submitted();
        assertThatThrownBy(() -> app.changeStatus(ApplicationStatus.CONDITIONAL_OFFER, "  ", staff, NOW))
                .isInstanceOf(BusinessRuleException.class);
        app.changeStatus(ApplicationStatus.CONDITIONAL_OFFER, "Send the final certificate", staff, NOW);
        assertThat(app.getCondition()).isEqualTo("Send the final certificate");
        app.changeStatus(ApplicationStatus.ACCEPTED, "ignored", staff, NOW);
        assertThat(app.getCondition()).isNull();
    }

    @Test
    @DisplayName("Only staff record decisions, and only with staff statuses")
    void onlyStaffDecide() {
        Application app = submitted();
        assertThatThrownBy(() -> app.changeStatus(ApplicationStatus.ACCEPTED, null, applicant, NOW))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> app.changeStatus(ApplicationStatus.WITHDRAWN, null, staff, NOW))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> app.changeStatus(ApplicationStatus.SUBMITTED, null, staff, NOW))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("TC-9.6 Only an admitted applicant can be marked as enrolled")
    void enrolledNeedsAdmission() {
        Application app = submitted();
        assertThatThrownBy(() -> app.changeStatus(ApplicationStatus.ENROLLED, null, staff, NOW))
                .isInstanceOf(BusinessRuleException.class);
        app.changeStatus(ApplicationStatus.ACCEPTED, null, staff, NOW);
        app.changeStatus(ApplicationStatus.ENROLLED, null, staff, NOW);
        assertThat(app.getStatus().stage()).isEqualTo(Stage.ENROLLED);
    }

    @Test
    @DisplayName("Setting the same status again is refused")
    void sameStatusRefused() {
        Application app = submitted();
        app.changeStatus(ApplicationStatus.WAITLISTED, null, staff, NOW);
        assertThatThrownBy(() -> app.changeStatus(ApplicationStatus.WAITLISTED, null, staff, NOW))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("TC-5.1/TC-9.7 Withdraw works before a decision, is logged with the applicant, and closes the application")
    void withdraw() {
        Application app = submitted();
        app.changeStatus(ApplicationStatus.UNDER_REVIEW, null, staff, NOW);
        StatusChange change = app.withdraw(applicant, NOW);
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.WITHDRAWN);
        assertThat(change.getChangedBy()).isSameAs(applicant);
        assertThat(app.canBeEdited(TODAY)).isFalse();
        assertThatThrownBy(() -> app.changeStatus(ApplicationStatus.ACCEPTED, null, staff, NOW))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Withdraw is refused after a decision and for another applicant")
    void withdrawRefused() {
        Application app = submitted();
        Applicant other = withId(new Applicant("Other", "other@example.com"), 99L);
        assertThatThrownBy(() -> app.withdraw(other, NOW)).isInstanceOf(BusinessRuleException.class);
        app.changeStatus(ApplicationStatus.WAITLISTED, null, staff, NOW);
        assertThatThrownBy(() -> app.withdraw(applicant, NOW)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("TC-A.5 Applicants can view only their own application; staff can view any")
    void canView() {
        Application app = submitted();
        assertThat(applicant.canView(app)).isTrue();
        assertThat(withId(new Applicant("Other", "other@example.com"), 99L).canView(app)).isFalse();
        assertThat(staff.canView(app)).isTrue();
    }

    @Test
    @DisplayName("Lead opt-in can be removed once; then there is nothing to change")
    void leadOptIn() {
        Lead lead = new Lead("Lena", " Lena@Example.com ", true);
        assertThat(lead.getEmail()).isEqualTo("lena@example.com");
        assertThat(lead.canReceiveGroupEmail()).isTrue();
        assertThat(lead.removeOptIn()).isTrue();
        assertThat(lead.canReceiveGroupEmail()).isFalse();
        assertThat(lead.removeOptIn()).isFalse();
    }

    @Test
    @DisplayName("Event needs a location or a link and refuses a second registration of the same email")
    void eventRegistration() {
        assertThatThrownBy(() -> new Event("Open Day", NOW, " ", null)).isInstanceOf(BusinessRuleException.class);

        Event event = new Event("Open Day", NOW.plusDays(3), "Campus", null);
        Lead in = new Lead("In", "in@example.com", true);
        Lead out = new Lead("Out", "out@example.com", false);
        event.register(in, NOW);
        event.register(out, NOW);
        assertThatThrownBy(() -> event.register(new Lead("Again", "IN@example.com", true), NOW))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(event.optedInLeads()).containsExactly(in);
    }

    private Application submitted() {
        Application app = complete(new Application(applicant, program));
        app.submit(NOW);
        return app;
    }

    private static Application complete(Application app) {
        app.setDetails("+380", "Kyiv", "Lyceum 1", 2026, 10.5);
        for (DocumentType type : DocumentType.values()) {
            app.addDocument(pdf(type, 1000));
        }
        return app;
    }

    private static Document pdf(DocumentType type, long size) {
        return new Document(type, type + ".pdf", Document.PDF_CONTENT_TYPE, size, type + ".pdf");
    }

    private static <T extends User> T withId(T user, long id) {
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
