package com.semih.siteapi.post;

import com.semih.siteapi.tag.Tag;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

/**
 * Building blocks for the public blog list. Each filter is optional, so the query is put
 * together from the ones the visitor used (Specification) instead of writing a repository
 * method for every combination.
 */
final class PostSpecifications {

    private static final char ESCAPE = '\\';

    private PostSpecifications() {
    }

    static Specification<Post> isPublished() {
        return (root, query, cb) -> cb.equal(root.get("status"), PostStatus.PUBLISHED);
    }

    /** Posts with the tag. A post has a tag at most once, so the join adds no duplicate rows. */
    static Specification<Post> hasTag(String tagSlug) {
        return (root, query, cb) -> {
            Join<Post, Tag> tags = root.join("tags");
            return cb.equal(tags.get("slug"), tagSlug);
        };
    }

    /** Case-insensitive search in title, summary and content. */
    static Specification<Post> matches(String text) {
        String pattern = "%" + escapeLike(text) + "%";
        return (root, query, cb) -> {
            // lower() on both sides in the database, so both use the same rules
            Expression<String> lowered = cb.lower(cb.literal(pattern));
            return cb.or(
                    cb.like(cb.lower(root.get("title")), lowered, ESCAPE),
                    cb.like(cb.lower(root.get("summary")), lowered, ESCAPE),
                    cb.like(cb.lower(root.get("content")), lowered, ESCAPE));
        };
    }

    /** "100%" must search for a percent sign, not "100 followed by anything". */
    static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
