package com.semih.siteapi.post.dto;

import com.semih.siteapi.post.PostStatus;

import java.time.Instant;

/** Full view of a post, including its content. Built by {@code PostMapper}. */
public record PostResponse(
        Long id,
        String slug,
        String title,
        String summary,
        String content,
        PostStatus status,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt) {
}
