package com.semih.siteapi.contact.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param website honeypot: the form hides this field from people, so only bots fill it in.
 *                Such requests get the normal "accepted" answer but are silently dropped,
 *                so the bot learns nothing.
 */
public record ContactRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 200) String subject,
        @NotBlank @Size(min = 10, max = 5000) String message,
        String website) {

    public boolean looksLikeBot() {
        return website != null && !website.isBlank();
    }
}
