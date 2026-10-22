CREATE TABLE posts (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    slug         VARCHAR(120) NOT NULL,
    title        VARCHAR(200) NOT NULL,
    summary      VARCHAR(500),
    content      TEXT         NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    published_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_posts_slug UNIQUE (slug),
    CONSTRAINT ck_posts_status CHECK (status IN ('DRAFT', 'PUBLISHED')),
    CONSTRAINT ck_posts_published_at CHECK (status = 'DRAFT' OR published_at IS NOT NULL)
);

-- The public blog list filters by status and sorts by publish date
CREATE INDEX ix_posts_status_published_at ON posts (status, published_at DESC);

CREATE TABLE projects (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    slug          VARCHAR(120)  NOT NULL,
    title         VARCHAR(200)  NOT NULL,
    summary       VARCHAR(500)  NOT NULL,
    description   TEXT,
    technologies  VARCHAR(1000) NOT NULL DEFAULT '',
    repo_url      VARCHAR(255),
    live_url      VARCHAR(255),
    featured      BOOLEAN       NOT NULL DEFAULT FALSE,
    display_order INTEGER       NOT NULL DEFAULT 0,
    published     BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uk_projects_slug UNIQUE (slug),
    CONSTRAINT ck_projects_display_order CHECK (display_order >= 0)
);
