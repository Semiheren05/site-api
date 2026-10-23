package com.semih.siteapi.post;

import com.semih.siteapi.post.dto.PostRequest;
import com.semih.siteapi.post.dto.PostResponse;
import com.semih.siteapi.post.dto.PostSummaryResponse;
import org.springframework.stereotype.Component;

import static com.semih.siteapi.common.Texts.trimToNull;

/**
 * Converts between blog DTOs and the {@link Post} entity. The service decides <em>what</em>
 * happens (slug rules, publishing); this class only copies and cleans up values.
 */
@Component
public class PostMapper {

    /** @param slug already resolved and checked by the service */
    public Post toEntity(PostRequest request, String slug) {
        return new Post(slug, request.title().trim(), trimToNull(request.summary()), request.content());
    }

    public void updateEntity(Post post, PostRequest request, String slug) {
        post.update(slug, request.title().trim(), trimToNull(request.summary()), request.content());
    }

    public PostResponse toResponse(Post post) {
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

    public PostSummaryResponse toSummary(Post post) {
        return new PostSummaryResponse(post.getSlug(), post.getTitle(), post.getSummary(), post.getPublishedAt());
    }
}
