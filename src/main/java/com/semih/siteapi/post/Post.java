package com.semih.siteapi.post;

import com.semih.siteapi.tag.Tag;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A blog post. New posts start as {@link PostStatus#DRAFT} and only appear on the
 * public blog after {@link #publish(Instant)}.
 */
@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String summary;

    // columnDefinition matches the migration, so ddl-auto=validate accepts PostgreSQL's text type
    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostStatus status = PostStatus.DRAFT;

    @Column(name = "published_at", columnDefinition = "timestamptz")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    private Instant updatedAt;

    /**
     * Owning side of the many-to-many (the join table is configured here). LAZY by default for
     * collections. {@code @BatchSize}: when a page of posts touches its tags, Hibernate loads the
     * tags of up to 50 posts in one query instead of one query per post (the N+1 problem).
     * A Set, not a List: with a List Hibernate rewrites the whole join table on every change.
     * No cascade: tags live on their own and are saved by TagService.
     */
    @ManyToMany
    @JoinTable(name = "post_tags",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    @BatchSize(size = 50)
    private Set<Tag> tags = new HashSet<>();

    protected Post() {
        // for JPA
    }

    public Post(String slug, String title, String summary, String content) {
        this.slug = slug;
        this.title = title;
        this.summary = summary;
        this.content = content;
    }

    /** Replaces the post's tags; Hibernate turns the difference into inserts/deletes on post_tags. */
    public void replaceTags(Set<Tag> newTags) {
        tags.clear();
        tags.addAll(newTags);
    }

    public void update(String slug, String title, String summary, String content) {
        this.slug = slug;
        this.title = title;
        this.summary = summary;
        this.content = content;
    }

    /**
     * Makes the post public. The first publish date is kept if the post is
     * unpublished and published again, so its place in the blog does not jump.
     */
    public void publish(Instant now) {
        status = PostStatus.PUBLISHED;
        if (publishedAt == null) {
            publishedAt = now;
        }
    }

    public void unpublish() {
        status = PostStatus.DRAFT;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getContent() {
        return content;
    }

    public PostStatus getStatus() {
        return status;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Set<Tag> getTags() {
        return Collections.unmodifiableSet(tags);
    }
}
