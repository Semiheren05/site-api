CREATE TABLE tags (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(30) NOT NULL,
    slug       VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_tags_slug UNIQUE (slug)
);

-- Join table of the posts <-> tags many-to-many. It has no columns of its own,
-- so a plain @ManyToMany fits; with extra columns it would need its own entity.
CREATE TABLE post_tags (
    post_id BIGINT NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    tag_id  BIGINT NOT NULL REFERENCES tags (id) ON DELETE CASCADE,
    PRIMARY KEY (post_id, tag_id)
);

-- The primary key already serves "tags of a post"; this index serves "posts with a tag".
-- PostgreSQL does not index foreign keys on its own.
CREATE INDEX ix_post_tags_tag ON post_tags (tag_id);
