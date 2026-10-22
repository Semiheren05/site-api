package com.semih.siteapi.post.dto;

import com.semih.siteapi.post.Post;

import java.time.Instant;

/** List view of a post: no content, so the blog index stays small. */
public record PostSummaryResponse(String slug, String title, String summary, Instant publishedAt) {

    public static PostSummaryResponse from(Post post) {
        return new PostSummaryResponse(post.getSlug(), post.getTitle(), post.getSummary(), post.getPublishedAt());
    }
}
