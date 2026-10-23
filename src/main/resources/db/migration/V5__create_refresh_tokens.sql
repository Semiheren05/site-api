-- Refresh tokens are stored hashed (SHA-256): a leaked database does not hand out sessions.
-- A "family" is one login: every rotation hands out a new token in the same family, so if an
-- old, already used token shows up again, the whole family (that login) can be revoked.
CREATE TABLE refresh_tokens (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    family_id  UUID        NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at TIMESTAMPTZ,
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX ix_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX ix_refresh_tokens_family ON refresh_tokens (family_id);
-- For the nightly clean-up of expired tokens
CREATE INDEX ix_refresh_tokens_expires_at ON refresh_tokens (expires_at);
