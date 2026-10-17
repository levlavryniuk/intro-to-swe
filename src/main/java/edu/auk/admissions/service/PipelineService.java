package edu.auk.admissions.service;

import edu.auk.admissions.domain.ApplicationStatus;
import edu.auk.admissions.domain.PipelineReport;
import edu.auk.admissions.repository.ApplicationRepository;
import edu.auk.admissions.repository.EventRegistrationRepository;
import edu.auk.admissions.repository.InquiryRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** UC-14 View Pipeline Overview. */
@Service
public class PipelineService {

    private final InquiryRepository inquiries;
    private final EventRegistrationRepository registrations;
    private final ApplicationRepository applications;
    private final Clock clock;

    public PipelineService(InquiryRepository inquiries, EventRegistrationRepository registrations,
                           ApplicationRepository applications, Clock clock) {
        this.inquiries = inquiries;
        this.registrations = registrations;
        this.applications = applications;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PipelineReport report() {
        Map<ApplicationStatus, Long> counts = new EnumMap<>(ApplicationStatus.class);
        for (Object[] row : applications.countByStatus()) {
            counts.put((ApplicationStatus) row[0], (Long) row[1]);
        }
        return new PipelineReport(LocalDateTime.now(clock), inquiries.count(), registrations.count(), counts);
    }
}
