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
    private final ContactMessageMapper mapper;
    private final ApplicationEventPublisher events;
    private final SlidingWindowRateLimiter rateLimiter;

    public ContactService(ContactMessageRepository repository,
                          ContactMessageMapper mapper,
                          ApplicationEventPublisher events,
                          ContactProperties properties,
                          Clock clock) {
        this.repository = repository;
        this.mapper = mapper;
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
            // The IP is personal data, so it is not logged; the request id links the line to the request
            log.warn("Contact form rate limit exceeded retryAfterSeconds={}", retryAfter.toSeconds());
            throw new RateLimitExceededException(retryAfter);
        });
        if (request.looksLikeBot()) {
            log.warn("Dropped a contact message that filled the honeypot field");
            return;
        }
        ContactMessage saved = repository.save(mapper.toEntity(request));
        log.info("Contact message received messageId={}", saved.getId());
        events.publishEvent(ContactMessageReceived.from(saved));
    }

    // --- Admin ---

    @Transactional(readOnly = true)
    public Page<ContactMessageResponse> list(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toResponse);
    }

    @Transactional
    public ContactMessageResponse markAsRead(Long id) {
        ContactMessage message = find(id);
        message.markAsRead();
        return mapper.toResponse(message);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(find(id));
    }

    private ContactMessage find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Message " + id + " not found"));
    }
}
