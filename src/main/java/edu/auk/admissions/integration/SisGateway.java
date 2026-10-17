package edu.auk.admissions.integration;

import edu.auk.admissions.domain.Application;

/**
 * Boundary to the Student Registration &amp; Records module (Team 5), UC-15.
 * Returns true when the SIS confirms it took the applicant.
 */
public interface SisGateway {

    boolean sendAcceptedApplicant(Application app);
}
