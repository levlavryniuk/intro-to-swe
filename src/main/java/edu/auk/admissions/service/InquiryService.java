package edu.auk.admissions.service;

import edu.auk.admissions.domain.Inquiry;
import edu.auk.admissions.domain.InterestLevel;
import edu.auk.admissions.domain.Lead;
import edu.auk.admissions.domain.Program;
import edu.auk.admissions.repository.InquiryRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** UC-1 Submit Inquiry and UC-7 Manage Inquiries. */
@Service
public class InquiryService {

    private final InquiryRepository inquiries;
    private final LeadService leadService;
    private final ProgramService programService;
    private final Clock clock;

    public InquiryService(InquiryRepository inquiries, LeadService leadService,
                          ProgramService programService, Clock clock) {
        this.inquiries = inquiries;
        this.leadService = leadService;
        this.programService = programService;
        this.clock = clock;
    }

    @Transactional
    public Inquiry submit(String name, String email, Long programId, String question, boolean promotionalOptIn) {
        Program program = programService.get(programId);
        Lead lead = leadService.findOrCreate(name, email, promotionalOptIn);
        String trimmedQuestion = question == null || question.isBlank() ? null : question.trim();
        return inquiries.save(new Inquiry(lead, program, trimmedQuestion, LocalDateTime.now(clock)));
    }

    /** Newest first, optionally only for one program. */
    @Transactional(readOnly = true)
    public List<Inquiry> list(Long programId) {
        return programId == null
                ? inquiries.findAllByOrderBySubmittedAtDescIdDesc()
                : inquiries.findByProgramIdOrderBySubmittedAtDescIdDesc(programId);
    }

    @Transactional
    public Inquiry rate(Long inquiryId, InterestLevel level) {
        Inquiry inquiry = inquiries.findById(inquiryId)
                .orElseThrow(() -> new NotFoundException("Inquiry not found."));
        inquiry.setInterest(level);
        return inquiry;
    }
}
