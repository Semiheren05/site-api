package com.semih.siteapi.contact.dto;

import java.time.Instant;

/** A message in the admin inbox. Built by {@code ContactMessageMapper}. */
public record ContactMessageResponse(
        Long id,
        String name,
        String email,
        String subject,
        String message,
        boolean read,
        Instant createdAt) {
}
