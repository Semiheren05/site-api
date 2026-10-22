package com.semih.siteapi.contact;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * @param notifyTo    where new-message notifications are emailed; empty turns notifications off
 * @param mailFrom    sender address for notifications; empty uses the mail server's default
 * @param maxRequests how many messages one client (IP) may send per {@code window}
 * @param window      the rolling time window for {@code maxRequests}
 */
@Validated
@ConfigurationProperties(prefix = "app.contact")
public record ContactProperties(
        String notifyTo,
        String mailFrom,
        @Positive int maxRequests,
        @NotNull Duration window) {

    public boolean notificationsEnabled() {
        return notifyTo != null && !notifyTo.isBlank();
    }
}
