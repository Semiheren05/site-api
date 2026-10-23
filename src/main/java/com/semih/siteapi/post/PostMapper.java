package com.semih.siteapi.post;

import com.semih.siteapi.post.dto.PostRequest;
import com.semih.siteapi.post.dto.PostResponse;
import com.semih.siteapi.post.dto.PostSummaryResponse;
import com.semih.siteapi.tag.Tag;
import com.semih.siteapi.tag.TagMapper;
import org.springframework.stereotype.Component;

import java.util.Set;

import static com.semih.siteapi.common.Texts.trimToNull;

/**
 * Converts between blog DTOs and the {@link Post} entity. The service decides <em>what</em>
 * happens (slug rules, which tags, publishing); this class only copies and cleans up values.
 * Related entities (tags) are loaded by the service and handed in; a mapper never queries.
 */
@Component
public class PostMapper {

    private final TagMapper tagMapper;

    public PostMapper(TagMapper tagMapper) {
        this.tagMapper = tagMapper;
    }

    /** @param slug already resolved and checked by the service */
    public Post toEntity(PostRequest request, String slug, Set<Tag> tags) {
        Post post = new Post(slug, request.title().trim(), trimToNull(request.summary()), request.content());
        post.replaceTags(tags);
        return post;
    }

    public void updateEntity(Post post, PostRequest request, String slug, Set<Tag> tags) {
        post.update(slug, request.title().trim(), trimToNull(request.summary()), request.content());
        post.replaceTags(tags);
    }

    public PostResponse toResponse(Post post) {
        return new PostResponse(
                post.getId(),
                post.getSlug(),
                post.getTitle(),
                post.getSummary(),
                post.getContent(),
                post.getStatus(),
                tagMapper.toSummaries(post.getTags()),
                post.getPublishedAt(),
                post.getCreatedAt(),
                post.getUpdatedAt());
    }

    public PostSummaryResponse toSummary(Post post) {
        return new PostSummaryResponse(post.getSlug(), post.getTitle(), post.getSummary(),
                tagMapper.toSummaries(post.getTags()), post.getPublishedAt());
    }
}
