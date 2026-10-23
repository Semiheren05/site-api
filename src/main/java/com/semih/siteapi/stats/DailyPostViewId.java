package com.semih.siteapi.stats;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalDate;
import java.util.Objects;

/** Composite key of {@link DailyPostView}: one row per post per day. */
@Embeddable
public class DailyPostViewId {

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "viewed_on", nullable = false)
    private LocalDate viewedOn;

    protected DailyPostViewId() {
        // for JPA
    }

    public DailyPostViewId(Long postId, LocalDate viewedOn) {
        this.postId = postId;
        this.viewedOn = viewedOn;
    }

    public Long getPostId() {
        return postId;
    }

    public LocalDate getViewedOn() {
        return viewedOn;
    }

    // An id class must compare by value: Hibernate looks entities up by it
    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof DailyPostViewId id
                && Objects.equals(postId, id.postId) && Objects.equals(viewedOn, id.viewedOn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(postId, viewedOn);
    }
}
