package com.semih.siteapi.post.dto;

import java.time.Instant;

/** List view of a post: no content, so the blog index stays small. Built by {@code PostMapper}. */
public record PostSummaryResponse(String slug, String title, String summary, Instant publishedAt) {
}
