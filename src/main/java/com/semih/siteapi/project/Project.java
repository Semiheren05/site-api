package com.semih.siteapi.project;

import com.semih.siteapi.common.StringListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.List;

/** A portfolio project shown on the website. */
@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 500)
    private String summary;

    @Column(columnDefinition = "text")
    private String description;

    @Convert(converter = StringListConverter.class)
    @Column(nullable = false, length = 1000)
    private List<String> technologies = List.of();

    @Column(name = "repo_url")
    private String repoUrl;

    @Column(name = "live_url")
    private String liveUrl;

    @Column(nullable = false)
    private boolean featured;

    /** Smaller numbers come first on the portfolio page. */
    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean published;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    private Instant updatedAt;

    protected Project() {
        // for JPA
    }

    public Project(ProjectDetails details) {
        apply(details);
    }

    public void update(ProjectDetails details) {
        apply(details);
    }

    private void apply(ProjectDetails details) {
        this.slug = details.slug();
        this.title = details.title();
        this.summary = details.summary();
        this.description = details.description();
        // Copy, so a list the caller changes later cannot change the entity
        this.technologies = List.copyOf(details.technologies());
        this.repoUrl = details.repoUrl();
        this.liveUrl = details.liveUrl();
        this.featured = details.featured();
        this.displayOrder = details.displayOrder();
        this.published = details.published();
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

    public String getDescription() {
        return description;
    }

    public List<String> getTechnologies() {
        return technologies;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public String getLiveUrl() {
        return liveUrl;
    }

    public boolean isFeatured() {
        return featured;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public boolean isPublished() {
        return published;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
