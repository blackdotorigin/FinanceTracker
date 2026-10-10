CREATE TABLE user_activity (
    id               UUID PRIMARY KEY,
    user_id          UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    activity_date    DATE        NOT NULL,
    transaction_count INTEGER    NOT NULL DEFAULT 0 CHECK (transaction_count >= 0),
    login_count      INTEGER     NOT NULL DEFAULT 0 CHECK (login_count >= 0),
    created_at       TIMESTAMPTZ NOT NULL,
    updated_at       TIMESTAMPTZ NOT NULL,
    created_by       VARCHAR(255),
    updated_by       VARCHAR(255),
    version          BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uk_user_activity_user_date UNIQUE (user_id, activity_date)
);

INSERT INTO user_activity
    (id, user_id, activity_date, transaction_count, login_count,
     created_at, updated_at, version)
SELECT gen_random_uuid(), user_id, transaction_date, COUNT(*)::INTEGER, 0,
       now(), now(), 0
FROM transactions
GROUP BY user_id, transaction_date;
