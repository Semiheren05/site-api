package com.semih.siteapi.post;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * {@link JpaSpecificationExecutor} adds {@code findAll(Specification, Pageable)} for the public
 * list, where tag and search filters are optional (see {@link PostSpecifications}).
 */
public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

    Optional<Post> findBySlugAndStatus(String slug, PostStatus status);

    boolean existsBySlug(String slug);

    /** For updates: is the slug taken by a <em>different</em> post? */
    boolean existsBySlugAndIdNot(String slug, Long id);
}
