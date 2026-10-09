CREATE TABLE users (
    id               UUID PRIMARY KEY,
    email            VARCHAR(255) NOT NULL,
    username         VARCHAR(50)  NOT NULL UNIQUE,
    password_hash    VARCHAR(255),
    full_name        VARCHAR(150) NOT NULL,
    default_currency VARCHAR(3)   NOT NULL DEFAULT 'INR',
    role             VARCHAR(20)  NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')),
    email_verified   BOOLEAN      NOT NULL DEFAULT FALSE,
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    created_by       VARCHAR(255),
    updated_by       VARCHAR(255),
    version          BIGINT       NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uq_users_email ON users (lower(email));
