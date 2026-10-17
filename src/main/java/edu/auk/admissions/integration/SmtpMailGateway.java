package edu.auk.admissions.integration;

import edu.auk.admissions.domain.DeliveryStatus;
import edu.auk.admissions.domain.EmailMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Sends through an SMTP server, e.g. a local test mail server such as Mailpit.
 * Enable with app.mail.mode=smtp and spring.mail.host / spring.mail.port.
 */
@Component
@ConditionalOnProperty(name = "app.mail.mode", havingValue = "smtp")
public class SmtpMailGateway implements MailGateway {

    private final JavaMailSender mailSender;
    private final String from;

    public SmtpMailGateway(JavaMailSender mailSender, @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public DeliveryStatus send(EmailMessage message) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(message.getRecipientEmail());
        mail.setSubject(message.getSubject());
        mail.setText(message.getBody());
        mailSender.send(mail);
        return DeliveryStatus.SENT;
    }
}
