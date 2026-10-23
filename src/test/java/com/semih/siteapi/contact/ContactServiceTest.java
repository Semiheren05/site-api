package com.semih.siteapi.contact;

import com.semih.siteapi.common.exception.RateLimitExceededException;
import com.semih.siteapi.contact.dto.ContactRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-22T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    private ContactMessageRepository repository;
    @Mock
    private ApplicationEventPublisher events;

    private ContactService contactService;

    @BeforeEach
    void setUp() {
        ContactProperties properties = new ContactProperties(null, null, 2, Duration.ofMinutes(10));
        contactService = new ContactService(repository, new ContactMessageMapper(), events, properties, CLOCK);
    }

    @Test
    void savesTheTrimmedMessageAndAnnouncesIt() {
        when(repository.save(any(ContactMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        contactService.submit(new ContactRequest(" Ayşe ", " ayse@example.com ", "  ", "  Merhaba Semih, bir sorum var.  ", null),
                "1.2.3.4");

        ArgumentCaptor<ContactMessage> saved = ArgumentCaptor.forClass(ContactMessage.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("Ayşe");
        assertThat(saved.getValue().getEmail()).isEqualTo("ayse@example.com");
        assertThat(saved.getValue().getSubject()).isNull();
        assertThat(saved.getValue().getMessage()).isEqualTo("Merhaba Semih, bir sorum var.");

        ArgumentCaptor<ContactMessageReceived> event = ArgumentCaptor.forClass(ContactMessageReceived.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue().email()).isEqualTo("ayse@example.com");
    }

    @Test
    void honeypotMessagesAreDroppedSilently() {
        contactService.submit(new ContactRequest("Bot", "bot@example.com", null, "Ucuz takipçi satın al!", "http://spam"),
                "6.6.6.6");

        verifyNoInteractions(repository, events);
    }

    @Test
    void tooManyMessagesFromOneIpAreRejectedAndNotSaved() {
        when(repository.save(any(ContactMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ContactRequest request = new ContactRequest("Ayşe", "ayse@example.com", null, "Merhaba, bir sorum var.", null);

        contactService.submit(request, "1.2.3.4");
        contactService.submit(request, "1.2.3.4");

        assertThatThrownBy(() -> contactService.submit(request, "1.2.3.4"))
                .isInstanceOf(RateLimitExceededException.class);
        verify(repository, times(2)).save(any());
    }

    @Test
    void botsCountAgainstTheLimitToo() {
        ContactRequest bot = new ContactRequest("Bot", "bot@example.com", null, "Ucuz takipçi satın al!", "x");

        contactService.submit(bot, "6.6.6.6");
        contactService.submit(bot, "6.6.6.6");

        assertThatThrownBy(() -> contactService.submit(bot, "6.6.6.6"))
                .isInstanceOf(RateLimitExceededException.class);
        verify(repository, never()).save(any());
    }
}
