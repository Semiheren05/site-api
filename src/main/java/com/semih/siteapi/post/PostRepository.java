package com.semih.siteapi.post;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/**
 * {@link JpaSpecificationExecutor} adds {@code findAll(Specification, Pageable)} for the public
 * list, where tag and search filters are optional (see {@link PostSpecifications}).
 */
public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

    Optional<Post> findBySlugAndStatus(String slug, PostStatus status);

    /** Only the id: counting a view does not need the whole post with its content. */
    @Query("select p.id from Post p where p.slug = :slug and p.status = com.semih.siteapi.post.PostStatus.PUBLISHED")
    Optional<Long> findPublishedId(String slug);

    boolean existsBySlug(String slug);

    /** For updates: is the slug taken by a <em>different</em> post? */
    boolean existsBySlugAndIdNot(String slug, Long id);
}
