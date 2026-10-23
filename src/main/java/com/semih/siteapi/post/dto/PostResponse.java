package com.semih.siteapi.post.dto;

import com.semih.siteapi.post.PostStatus;
import com.semih.siteapi.tag.dto.TagSummary;

import java.time.Instant;
import java.util.List;

/** Full view of a post, including its content. Built by {@code PostMapper}. */
public record PostResponse(
        Long id,
        String slug,
        String title,
        String summary,
        String content,
        PostStatus status,
        List<TagSummary> tags,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt) {
}
