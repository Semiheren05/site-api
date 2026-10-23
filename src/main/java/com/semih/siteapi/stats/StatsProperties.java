package com.semih.siteapi.stats;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.ZoneId;

/** @param timeZone where a "day" starts and ends for the daily counts, e.g. Europe/Istanbul */
@Validated
@ConfigurationProperties(prefix = "app.stats")
public record StatsProperties(@NotNull ZoneId timeZone) {
}
