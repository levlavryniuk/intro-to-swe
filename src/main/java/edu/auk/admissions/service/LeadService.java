package edu.auk.admissions.service;

import edu.auk.admissions.domain.Lead;
import edu.auk.admissions.repository.LeadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeadService {

    private final LeadRepository leads;

    public LeadService(LeadRepository leads) {
        this.leads = leads;
    }

    /** UC-1, UC-2: attach to the lead with this email address, or create one. */
    @Transactional
    public Lead findOrCreate(String name, String email, boolean promotionalOptIn) {
        String normalized = Lead.normalizeEmail(email);
        Lead lead = leads.findByEmail(normalized).orElse(null);
        if (lead == null) {
            return leads.save(new Lead(name.trim(), normalized, promotionalOptIn));
        }
        lead.updateFromForm(name.trim(), promotionalOptIn);
        return lead;
    }

    /** UC-11. Returns false when there was nothing to change. */
    @Transactional
    public boolean removeOptIn(Long leadId) {
        return get(leadId).removeOptIn();
    }

    @Transactional(readOnly = true)
    public Lead get(Long leadId) {
        return leads.findById(leadId).orElseThrow(() -> new NotFoundException("Lead not found."));
    }
}
