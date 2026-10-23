package com.semih.siteapi.stats.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * @param daily    every day of the period, days without views included (as 0), ready for a chart
 * @param topPosts the most read posts of the period, at most 10
 */
public record ViewStatsResponse(LocalDate from, LocalDate to, long totalViews,
                                List<DailyViews> daily, List<PostViews> topPosts) {
}
