CREATE TABLE user_budget_limit (
    id               UUID PRIMARY KEY,
    user_id          UUID           NOT NULL REFERENCES users (id),
    category_id      UUID           NOT NULL REFERENCES categories (id),
    sub_category_id  UUID,
    limit_amount     NUMERIC(19, 4) NOT NULL CHECK (limit_amount > 0),
    created_at       TIMESTAMPTZ    NOT NULL,
    updated_at       TIMESTAMPTZ    NOT NULL,
    created_by       VARCHAR(255),
    updated_by       VARCHAR(255),
    version          BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT fk_budget_limit_category_sub
        FOREIGN KEY (category_id, sub_category_id)
        REFERENCES category_sub_categories (category_id, sub_category_id)
);

CREATE UNIQUE INDEX uq_budget_limit_category
    ON user_budget_limit (user_id, category_id)
    WHERE sub_category_id IS NULL;

CREATE UNIQUE INDEX uq_budget_limit_sub_category
    ON user_budget_limit (user_id, category_id, sub_category_id)
    WHERE sub_category_id IS NOT NULL;

CREATE INDEX idx_budget_limit_user ON user_budget_limit (user_id);
