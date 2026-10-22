package com.semih.siteapi.post;

import com.semih.siteapi.common.Slugs;
import com.semih.siteapi.common.exception.InvalidSlugException;
import com.semih.siteapi.common.exception.ResourceNotFoundException;
import com.semih.siteapi.common.exception.SlugAlreadyUsedException;
import com.semih.siteapi.post.dto.PostRequest;
import com.semih.siteapi.post.dto.PostResponse;
import com.semih.siteapi.post.dto.PostSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final Clock clock;

    public PostService(PostRepository postRepository, Clock clock) {
        this.postRepository = postRepository;
        this.clock = clock;
    }

    // --- Public ---

    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> listPublished(Pageable pageable) {
        return postRepository.findByStatus(PostStatus.PUBLISHED, pageable).map(PostSummaryResponse::from);
    }

    /** Drafts answer 404 too, so the public API does not reveal that they exist. */
    @Transactional(readOnly = true)
    public PostResponse getPublished(String slug) {
        return postRepository.findBySlugAndStatus(slug, PostStatus.PUBLISHED)
                .map(PostResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
    }

    // --- Admin ---

    @Transactional(readOnly = true)
    public Page<PostResponse> listAll(Pageable pageable) {
        return postRepository.findAll(pageable).map(PostResponse::from);
    }

    @Transactional(readOnly = true)
    public PostResponse get(Long id) {
        return PostResponse.from(find(id));
    }

    @Transactional
    public PostResponse create(PostRequest request) {
        String slug = resolveSlug(request);
        if (postRepository.existsBySlug(slug)) {
            throw new SlugAlreadyUsedException(slug);
        }
        Post post = postRepository.save(new Post(slug, request.title().trim(), trimToNull(request.summary()),
                request.content()));
        return PostResponse.from(post);
    }

    @Transactional
    public PostResponse update(Long id, PostRequest request) {
        Post post = find(id);
        String slug = resolveSlug(request);
        if (postRepository.existsBySlugAndIdNot(slug, id)) {
            throw new SlugAlreadyUsedException(slug);
        }
        post.update(slug, request.title().trim(), trimToNull(request.summary()), request.content());
        // Flush so @PreUpdate runs now and the response shows the new updatedAt
        postRepository.flush();
        return PostResponse.from(post);
    }

    @Transactional
    public PostResponse publish(Long id) {
        Post post = find(id);
        post.publish(clock.instant());
        postRepository.flush();
        return PostResponse.from(post);
    }

    @Transactional
    public PostResponse unpublish(Long id) {
        Post post = find(id);
        post.unpublish();
        postRepository.flush();
        return PostResponse.from(post);
    }

    @Transactional
    public void delete(Long id) {
        postRepository.delete(find(id));
    }

    private Post find(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post " + id + " not found"));
    }

    private static String resolveSlug(PostRequest request) {
        String slug = request.slug() == null || request.slug().isBlank()
                ? Slugs.from(request.title())
                : request.slug();
        if (slug.isEmpty()) {
            throw new InvalidSlugException();
        }
        return slug;
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
