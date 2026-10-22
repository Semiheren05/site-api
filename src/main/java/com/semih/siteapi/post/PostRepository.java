package com.semih.siteapi.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findByStatus(PostStatus status, Pageable pageable);

    Optional<Post> findBySlugAndStatus(String slug, PostStatus status);

    boolean existsBySlug(String slug);

    /** For updates: is the slug taken by a <em>different</em> post? */
    boolean existsBySlugAndIdNot(String slug, Long id);
}
