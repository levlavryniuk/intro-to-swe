package edu.auk.admissions.domain;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

/** The numbers on the overview screen (FR-07). Not stored; built from what is in the database. */
public class PipelineReport {

    private final LocalDateTime generatedAt;
    private final long inquiries;
    private final long eventRegistrations;
    private final Map<ApplicationStatus, Long> byStatus = new EnumMap<>(ApplicationStatus.class);

    public PipelineReport(LocalDateTime generatedAt, long inquiries, long eventRegistrations,
                          Map<ApplicationStatus, Long> statusCounts) {
        this.generatedAt = generatedAt;
        this.inquiries = inquiries;
        this.eventRegistrations = eventRegistrations;
        for (ApplicationStatus status : ApplicationStatus.values()) {
            byStatus.put(status, statusCounts.getOrDefault(status, 0L));
        }
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public long countInquiries() {
        return inquiries;
    }

    public long countEventRegistrations() {
        return eventRegistrations;
    }

    public long countByStatus(ApplicationStatus status) {
        return byStatus.get(status);
    }

    public long countByStage(Stage stage) {
        return byStatus.entrySet().stream()
                .filter(e -> e.getKey().stage() == stage)
                .mapToLong(Map.Entry::getValue)
                .sum();
    }
}
