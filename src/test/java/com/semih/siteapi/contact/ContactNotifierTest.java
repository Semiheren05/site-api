package com.semih.siteapi.contact;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactNotifierTest {

    private static final ContactMessageReceived EVENT = new ContactMessageReceived(
            7L, "Ayşe", "ayse@example.com", "Proje teklifi", "Merhaba Semih, birlikte çalışalım mı?");

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;
    @Mock
    private JavaMailSender mailSender;

    @Test
    void sendsANotificationThatRepliesToTheVisitor() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);

        notifier("semih@example.com", "site@example.com").onMessageReceived(EVENT);

        ArgumentCaptor<SimpleMailMessage> mail = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mail.capture());
        assertThat(mail.getValue().getTo()).containsExactly("semih@example.com");
        assertThat(mail.getValue().getFrom()).isEqualTo("site@example.com");
        assertThat(mail.getValue().getReplyTo()).isEqualTo("ayse@example.com");
        assertThat(mail.getValue().getSubject()).isEqualTo("Siteden yeni mesaj: Proje teklifi");
        assertThat(mail.getValue().getText()).contains("Ayşe <ayse@example.com>", "birlikte çalışalım mı?");
    }

    @Test
    void doesNothingWhenNoRecipientIsConfigured() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);

        notifier("", null).onMessageReceived(EVENT);

        verifyNoInteractions(mailSender);
    }

    @Test
    void doesNothingWhenNoMailServerIsConfigured() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);

        assertThatCode(() -> notifier("semih@example.com", null).onMessageReceived(EVENT)).doesNotThrowAnyException();
    }

    @Test
    void aMailServerErrorIsLoggedNotThrown() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        doThrow(new MailSendException("SMTP down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> notifier("semih@example.com", null).onMessageReceived(EVENT)).doesNotThrowAnyException();
    }

    @Test
    void lineBreaksCannotReachTheMailSubject() {
        ContactMessageReceived sneaky = new ContactMessageReceived(
                8L, "X", "x@example.com", "Merhaba\r\nBcc: herkes@example.com", "...");

        SimpleMailMessage mail = notifier("semih@example.com", null).buildMail(sneaky);

        assertThat(mail.getSubject()).doesNotContain("\r", "\n");
    }

    private ContactNotifier notifier(String notifyTo, String mailFrom) {
        return new ContactNotifier(mailSenderProvider, new ContactProperties(notifyTo, mailFrom, 3, Duration.ofMinutes(10)));
    }
}
