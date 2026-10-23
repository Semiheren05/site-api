package com.semih.siteapi.post;

import com.semih.siteapi.common.Slugs;
import com.semih.siteapi.common.exception.InvalidSlugException;
import com.semih.siteapi.common.exception.ResourceNotFoundException;
import com.semih.siteapi.common.exception.SlugAlreadyUsedException;
import com.semih.siteapi.post.dto.PostRequest;
import com.semih.siteapi.post.dto.PostResponse;
import com.semih.siteapi.post.dto.PostSummaryResponse;
import com.semih.siteapi.tag.Tag;
import com.semih.siteapi.tag.TagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.semih.siteapi.common.Texts.trimToNull;

@Service
public class PostService {

    private static final Logger log = LoggerFactory.getLogger(PostService.class);

    private final PostRepository postRepository;
    private final TagService tagService;
    private final PostMapper mapper;
    private final Clock clock;

    public PostService(PostRepository postRepository, TagService tagService, PostMapper mapper, Clock clock) {
        this.postRepository = postRepository;
        this.tagService = tagService;
        this.mapper = mapper;
        this.clock = clock;
    }

    // --- Public ---

    /**
     * Published posts, optionally only those with a tag and/or containing a text.
     * Each page costs a fixed number of queries however many posts it has: the posts, the
     * count, and one batch for all their tags ({@code @BatchSize} on {@code Post#tags}).
     *
     * @param tagSlug e.g. "spring-boot"; null for all tags
     * @param query   searched in title, summary and content; null for no search
     */
    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> listPublished(String tagSlug, String query, Pageable pageable) {
        List<Specification<Post>> filters = new ArrayList<>();
        filters.add(PostSpecifications.isPublished());
        String tag = trimToNull(tagSlug);
        if (tag != null) {
            filters.add(PostSpecifications.hasTag(tag.toLowerCase(Locale.ROOT)));
        }
        String text = trimToNull(query);
        if (text != null) {
            filters.add(PostSpecifications.matches(text));
        }
        return postRepository.findAll(Specification.allOf(filters), pageable).map(mapper::toSummary);
    }

    /** Drafts answer 404 too, so the public API does not reveal that they exist. */
    @Transactional(readOnly = true)
    public PostResponse getPublished(String slug) {
        return postRepository.findBySlugAndStatus(slug, PostStatus.PUBLISHED)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
    }

    // --- Admin ---

    @Transactional(readOnly = true)
    public Page<PostResponse> listAll(Pageable pageable) {
        return postRepository.findAll(pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PostResponse get(Long id) {
        return mapper.toResponse(find(id));
    }

    @Transactional
    public PostResponse create(PostRequest request) {
        String slug = resolveSlug(request);
        if (postRepository.existsBySlug(slug)) {
            throw new SlugAlreadyUsedException(slug);
        }
        Set<Tag> tags = tagService.resolve(request.tags());
        Post post = postRepository.save(mapper.toEntity(request, slug, tags));
        log.info("Post created postId={} slug={}", post.getId(), slug);
        return mapper.toResponse(post);
    }

    @Transactional
    public PostResponse update(Long id, PostRequest request) {
        Post post = find(id);
        String slug = resolveSlug(request);
        if (postRepository.existsBySlugAndIdNot(slug, id)) {
            throw new SlugAlreadyUsedException(slug);
        }
        mapper.updateEntity(post, request, slug, tagService.resolve(request.tags()));
        // Flush so @PreUpdate runs now and the response shows the new updatedAt
        postRepository.flush();
        return mapper.toResponse(post);
    }

    @Transactional
    public PostResponse publish(Long id) {
        Post post = find(id);
        post.publish(clock.instant());
        postRepository.flush();
        log.info("Post published postId={}", id);
        return mapper.toResponse(post);
    }

    @Transactional
    public PostResponse unpublish(Long id) {
        Post post = find(id);
        post.unpublish();
        postRepository.flush();
        log.info("Post moved back to drafts postId={}", id);
        return mapper.toResponse(post);
    }

    @Transactional
    public void delete(Long id) {
        postRepository.delete(find(id));
        log.info("Post deleted postId={}", id);
    }

    private Post find(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post " + id + " not found"));
    }

    /** Business rule: an explicit slug wins, otherwise it is built from the title. */
    private static String resolveSlug(PostRequest request) {
        String slug = request.slug() == null || request.slug().isBlank()
                ? Slugs.from(request.title())
                : request.slug();
        if (slug.isEmpty()) {
            throw new InvalidSlugException();
        }
        return slug;
    }
}
