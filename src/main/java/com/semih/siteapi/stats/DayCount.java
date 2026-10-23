package com.semih.siteapi.stats;

import java.time.LocalDate;

/** Query result: total views of all posts on one day. */
public record DayCount(LocalDate day, Long views) {
}
