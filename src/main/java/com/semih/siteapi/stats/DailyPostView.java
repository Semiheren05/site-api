package com.semih.siteapi.stats;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * How many people read a post on one day. Rows are only ever written by the upsert in
 * {@link DailyPostViewRepository#increment}; the entity exists for the read queries.
 */
@Entity
@Table(name = "post_daily_views")
public class DailyPostView {

    @EmbeddedId
    private DailyPostViewId id;

    @Column(nullable = false)
    private int views;

    protected DailyPostView() {
        // for JPA
    }

    public DailyPostViewId getId() {
        return id;
    }

    public int getViews() {
        return views;
    }
}
