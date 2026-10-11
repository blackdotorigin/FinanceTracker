CREATE TABLE mobile_oauth_handoffs (
    id              UUID PRIMARY KEY,
    user_id         UUID        NOT NULL REFERENCES users (id),
    code_hash       VARCHAR(64) NOT NULL UNIQUE,
    code_challenge  VARCHAR(43) NOT NULL,
    state           VARCHAR(43) NOT NULL,
    expires_at      TIMESTAMPTZ NOT NULL,
    consumed_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_mobile_oauth_handoffs_expiry
    ON mobile_oauth_handoffs (expires_at);
