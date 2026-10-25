package edu.auk.admissions.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PipelineReportTest {

    @Test
    @DisplayName("TC-14.2 With no data every count is zero")
    void emptyReport() {
        PipelineReport report = new PipelineReport(LocalDateTime.now(), 0, 0, Map.of());
        for (ApplicationStatus status : ApplicationStatus.values()) {
            assertThat(report.countByStatus(status)).isZero();
        }
        for (Stage stage : Stage.values()) {
            assertThat(report.countByStage(stage)).isZero();
        }
    }

    @Test
    @DisplayName("Stage totals add up the statuses inside them")
    void stageTotals() {
        PipelineReport report = new PipelineReport(LocalDateTime.now(), 5, 3, Map.of(
                ApplicationStatus.SUBMITTED, 4L, ApplicationStatus.WAITLISTED, 1L,
                ApplicationStatus.ACCEPTED, 2L, ApplicationStatus.REJECTED, 1L, ApplicationStatus.WITHDRAWN, 2L));
        assertThat(report.countByStage(Stage.APPLIED)).isEqualTo(5);
        assertThat(report.countByStage(Stage.ADMITTED)).isEqualTo(2);
        assertThat(report.countByStage(Stage.CLOSED)).isEqualTo(3);
        assertThat(report.countByStage(Stage.ENROLLED)).isZero();
        assertThat(report.countInquiries()).isEqualTo(5);
        assertThat(report.countEventRegistrations()).isEqualTo(3);
    }
}
