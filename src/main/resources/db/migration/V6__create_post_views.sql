-- Daily view counts per post: only numbers, no IP address, no cookie, no visitor id.
CREATE TABLE post_daily_views (
    post_id   BIGINT  NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    viewed_on DATE    NOT NULL,
    views     INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (post_id, viewed_on)
);

CREATE INDEX ix_post_daily_views_viewed_on ON post_daily_views (viewed_on);

-- Who was already counted today, so a refresh does not count twice. The hash is made with a
-- random salt that lives only in memory and changes every day, so it cannot be turned back into
-- an IP address or linked across days. Rows are deleted once their day is over.
CREATE TABLE post_view_visitors (
    post_id      BIGINT      NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    viewed_on    DATE        NOT NULL,
    visitor_hash VARCHAR(64) NOT NULL,
    PRIMARY KEY (post_id, viewed_on, visitor_hash)
);

CREATE INDEX ix_post_view_visitors_viewed_on ON post_view_visitors (viewed_on);
