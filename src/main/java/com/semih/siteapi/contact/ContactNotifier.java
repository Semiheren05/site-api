package com.semih.siteapi.contact;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Emails the site owner when a contact message arrives.
 *
 * <ul>
 *   <li>{@code @TransactionalEventListener}: runs only after the message is really
 *       committed, so no email goes out for a message that was rolled back.</li>
 *   <li>{@code @Async}: runs on another thread, so the visitor does not wait for SMTP.</li>
 *   <li>A mail failure is logged, never thrown: the message is already saved and
 *       visible in the admin inbox.</li>
 * </ul>
 *
 * <p>Spring only creates a {@link JavaMailSender} when {@code spring.mail.host} is set,
 * so without mail settings this listener simply does nothing.
 */
@Component
public class ContactNotifier {

    private static final Logger log = LoggerFactory.getLogger(ContactNotifier.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final ContactProperties properties;

    public ContactNotifier(ObjectProvider<JavaMailSender> mailSender, ContactProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Async
    @TransactionalEventListener
    public void onMessageReceived(ContactMessageReceived event) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || !properties.notificationsEnabled()) {
            return;
        }
        try {
            sender.send(buildMail(event));
        } catch (MailException e) {
            log.warn("Could not send the notification for contact message {}: {}", event.id(), e.getMessage());
        }
    }

    SimpleMailMessage buildMail(ContactMessageReceived event) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(properties.notifyTo());
        if (properties.mailFrom() != null && !properties.mailFrom().isBlank()) {
            mail.setFrom(properties.mailFrom());
        }
        // "Reply" in the mail client answers the visitor directly
        mail.setReplyTo(event.email());
        String topic = event.subject() != null ? event.subject() : "(konu yok)";
        mail.setSubject(singleLine("Siteden yeni mesaj: " + topic));
        mail.setText("""
                Gönderen: %s <%s>
                Konu: %s

                %s
                """.formatted(event.name(), event.email(), topic, event.message()));
        return mail;
    }

    /** Line breaks in a mail header could inject extra headers, so they are removed. */
    private static String singleLine(String value) {
        return value.replaceAll("[\\r\\n]+", " ");
    }
}
