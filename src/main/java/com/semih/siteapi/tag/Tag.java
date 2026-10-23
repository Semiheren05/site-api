package com.semih.siteapi.tag;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/**
 * A blog tag such as "Spring Boot". Posts point to tags (see {@code Post#tags}); a tag does not
 * know its posts, because "all posts of a tag" is a paginated query, never a collection to load.
 */
@Entity
@Table(name = "tags")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** As first written, e.g. "Spring Boot". */
    @Column(nullable = false, length = 30)
    private String name;

    /** The identity of a tag: "Spring Boot" and "spring boot" are the same tag, spring-boot. */
    @Column(nullable = false, unique = true, length = 40, updatable = false)
    private String slug;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    private Instant createdAt;

    protected Tag() {
        // for JPA
    }

    public Tag(String name, String slug) {
        this.name = name;
        this.slug = slug;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Equal by slug, the natural key that never changes. Not by id: a new tag has no id until it is
     * saved, and a Set that compared ids would lose it.
     */
    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof Tag tag && slug.equals(tag.slug);
    }

    @Override
    public int hashCode() {
        return Objects.hash(slug);
    }
}
