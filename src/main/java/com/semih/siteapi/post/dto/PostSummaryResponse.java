package com.semih.siteapi.post.dto;

import com.semih.siteapi.tag.dto.TagSummary;

import java.time.Instant;
import java.util.List;

/** List view of a post: no content, so the blog index stays small. Built by {@code PostMapper}. */
public record PostSummaryResponse(String slug, String title, String summary, List<TagSummary> tags,
                                  Instant publishedAt) {
}
