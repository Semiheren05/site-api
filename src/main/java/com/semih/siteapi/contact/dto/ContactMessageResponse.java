package com.semih.siteapi.contact.dto;

import com.semih.siteapi.contact.ContactMessage;

import java.time.Instant;

public record ContactMessageResponse(
        Long id,
        String name,
        String email,
        String subject,
        String message,
        boolean read,
        Instant createdAt) {

    public static ContactMessageResponse from(ContactMessage message) {
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
