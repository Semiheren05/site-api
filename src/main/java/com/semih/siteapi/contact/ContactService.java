package com.semih.siteapi.contact;

import com.semih.siteapi.common.SlidingWindowRateLimiter;
import com.semih.siteapi.common.exception.RateLimitExceededException;
import com.semih.siteapi.common.exception.ResourceNotFoundException;
import com.semih.siteapi.contact.dto.ContactMessageResponse;
import com.semih.siteapi.contact.dto.ContactRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class ContactService {

    private static final Logger log = LoggerFactory.getLogger(ContactService.class);

    private final ContactMessageRepository repository;
    private final ApplicationEventPublisher events;
    private final SlidingWindowRateLimiter rateLimiter;

    public ContactService(ContactMessageRepository repository,
                          ApplicationEventPublisher events,
                          ContactProperties properties,
                          Clock clock) {
        this.repository = repository;
        this.events = events;
        this.rateLimiter = new SlidingWindowRateLimiter(properties.maxRequests(), properties.window(), clock);
    }

    /**
     * Saves a message from the website and announces it (the email notification
     * listens). The rate limit is checked first, so bots count against it too.
     *
     * @throws RateLimitExceededException if this client sent too many messages recently
     */
    @Transactional
    public void submit(ContactRequest request, String clientIp) {
        rateLimiter.tryAcquire(clientIp).ifPresent(retryAfter -> {
            throw new RateLimitExceededException(retryAfter);
        });
        if (request.looksLikeBot()) {
            log.info("Dropped a contact message that filled the honeypot field");
            return;
        }
        ContactMessage saved = repository.save(new ContactMessage(
                request.name().trim(),
                request.email().trim(),
                trimToNull(request.subject()),
                request.message().trim()));
        events.publishEvent(ContactMessageReceived.from(saved));
    }

    // --- Admin ---

    @Transactional(readOnly = true)
    public Page<ContactMessageResponse> list(Pageable pageable) {
        return repository.findAll(pageable).map(ContactMessageResponse::from);
    }

    @Transactional
    public ContactMessageResponse markAsRead(Long id) {
        ContactMessage message = find(id);
        message.markAsRead();
        return ContactMessageResponse.from(message);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(find(id));
    }

    private ContactMessage find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Message " + id + " not found"));
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
