CREATE TABLE user_identities (
    id               UUID PRIMARY KEY,
    user_id          UUID         NOT NULL REFERENCES users (id),
    provider         VARCHAR(30)  NOT NULL,
    provider_user_id VARCHAR(100) NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    created_by       VARCHAR(255),
    updated_by       VARCHAR(255),
    version          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_identity_provider_user UNIQUE (provider, provider_user_id)
);

CREATE INDEX idx_identity_user ON user_identities (user_id);
