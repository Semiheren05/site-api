package com.semih.siteapi.stats.dto;

import java.time.LocalDate;

public record DailyViews(LocalDate day, long views) {
}
