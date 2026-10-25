package edu.auk.admissions.integration;

import edu.auk.admissions.domain.DeliveryStatus;
import edu.auk.admissions.domain.EmailMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Default for the prototype: writes each email to the log instead of sending it. */
@Component
@ConditionalOnProperty(name = "app.mail.mode", havingValue = "log", matchIfMissing = true)
public class LoggingMailGateway implements MailGateway {

    private static final Logger log = LoggerFactory.getLogger(LoggingMailGateway.class);

    @Override
    public DeliveryStatus send(EmailMessage message) {
        log.info("EMAIL to={} subject=\"{}\"\n{}", message.getRecipientEmail(), message.getSubject(), message.getBody());
        return DeliveryStatus.SENT;
    }
}
