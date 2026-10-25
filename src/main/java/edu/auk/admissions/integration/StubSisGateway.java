package edu.auk.admissions.integration;

import edu.auk.admissions.domain.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Placeholder until Team 5 publishes the SIS API. Logs the transfer; set app.sis.available=false
 * to see how the module behaves when the SIS cannot be reached.
 */
@Component
public class StubSisGateway implements SisGateway {

    private static final Logger log = LoggerFactory.getLogger(StubSisGateway.class);

    private final boolean available;

    public StubSisGateway(@Value("${app.sis.available:true}") boolean available) {
        this.available = available;
    }

    @Override
    public boolean sendAcceptedApplicant(Application app) {
        if (!available) {
            log.warn("SIS not reachable; application {} stays pending transfer", app.getId());
            return false;
        }
        log.info("SIS transfer: application={} applicant={} <{}> program={} status={}",
                app.getId(), app.getApplicant().getName(), app.getApplicant().getEmail(),
                app.getProgram().getName(), app.getStatus());
        return true;
    }
}
