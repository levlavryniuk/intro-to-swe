package edu.auk.admissions.integration;

import edu.auk.admissions.domain.DeliveryStatus;
import edu.auk.admissions.domain.EmailMessage;

/**
 * The only way the module sends mail. Hides which mail service is used, so the test
 * mail server can be replaced by the Core Platform's (Team 2) service later.
 */
public interface MailGateway {

    DeliveryStatus send(EmailMessage message);
}
