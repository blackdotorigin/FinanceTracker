CREATE TABLE sub_categories (
    id         UUID PRIMARY KEY,
    name       VARCHAR(80) NOT NULL,
    system_key VARCHAR(50) NOT NULL UNIQUE,
    icon       VARCHAR(50),
    sort_order INT         NOT NULL DEFAULT 0,
    active     BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version    BIGINT      NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uq_sub_categories_name ON sub_categories (lower(name));
