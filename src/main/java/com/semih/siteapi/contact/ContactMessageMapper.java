package com.semih.siteapi.contact;

import com.semih.siteapi.contact.dto.ContactMessageResponse;
import com.semih.siteapi.contact.dto.ContactRequest;
import org.springframework.stereotype.Component;

import static com.semih.siteapi.common.Texts.trimToNull;

/** Converts between contact form DTOs and the {@link ContactMessage} entity. */
@Component
public class ContactMessageMapper {

    public ContactMessage toEntity(ContactRequest request) {
        return new ContactMessage(
                request.name().trim(),
                request.email().trim(),
                trimToNull(request.subject()),
                request.message().trim());
    }

    public ContactMessageResponse toResponse(ContactMessage message) {
        return new ContactMessageResponse(
                message.getId(),
                message.getName(),
                message.getEmail(),
                message.getSubject(),
                message.getMessage(),
                message.isRead(),
                message.getCreatedAt());
    }
}
