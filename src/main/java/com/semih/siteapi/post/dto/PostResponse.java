package com.semih.siteapi.post.dto;

import com.semih.siteapi.post.Post;
import com.semih.siteapi.post.PostStatus;

import java.time.Instant;

/** Full view of a post, including its content. */
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

    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getId(),
                post.getSlug(),
                post.getTitle(),
                post.getSummary(),
                post.getContent(),
                post.getStatus(),
                post.getPublishedAt(),
                post.getCreatedAt(),
                post.getUpdatedAt());
    }
}
