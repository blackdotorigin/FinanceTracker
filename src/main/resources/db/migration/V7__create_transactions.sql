CREATE TABLE transactions (
    id               UUID PRIMARY KEY,
    user_id          UUID           NOT NULL REFERENCES users (id),
    amount           NUMERIC(19, 4) NOT NULL CHECK (amount > 0),
    currency         VARCHAR(3)     NOT NULL,
    type             VARCHAR(10)    NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    transaction_date DATE           NOT NULL,
    description      VARCHAR(255),
    category_id      UUID           NOT NULL REFERENCES categories (id),
    sub_category_id  UUID,
    created_at       TIMESTAMPTZ    NOT NULL,
    updated_at       TIMESTAMPTZ    NOT NULL,
    created_by       VARCHAR(255),
    updated_by       VARCHAR(255),
    version          BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT fk_txn_category_sub
        FOREIGN KEY (category_id, sub_category_id)
        REFERENCES category_sub_categories (category_id, sub_category_id)
);

CREATE INDEX idx_txn_user_date ON transactions (user_id, transaction_date DESC);
