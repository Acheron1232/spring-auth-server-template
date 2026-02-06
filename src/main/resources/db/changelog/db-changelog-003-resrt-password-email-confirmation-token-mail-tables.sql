--liquibase formatted sql

--changeset acheron:1
--comment create token and mail tables

-- =================================================================================================
-- 1. token table
-- Stores reset/confirmation tokens linked to users
-- =================================================================================================
CREATE TABLE token
(
    id           uuid                                NOT NULL,
    token        varchar(255)                        NOT NULL,
    user_id      uuid                                NOT NULL,
    expired_at   timestamp                           NOT NULL,
    token_status varchar(50)                         NOT NULL, -- ENUM: ACTIVE, INACTIVE
    token_type   varchar(50)                         NOT NULL, -- ENUM: RESET, CONFIRM

    -- Audit fields
    created_at   timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at   timestamp DEFAULT NULL,
    deleted_at   timestamp DEFAULT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uc_token_value UNIQUE (token),
    CONSTRAINT fk_token_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Index for faster lookup by token string (critical for security validation)
-- Note: The UNIQUE constraint usually creates an index, but explicit naming is good practice.
CREATE INDEX idx_token_value ON token (token);

-- Index for finding all tokens belonging to a specific user
CREATE INDEX idx_token_user_id ON token (user_id);


-- =================================================================================================
-- 2. mail table
-- Stores email logs/history
-- =================================================================================================
CREATE TABLE mail
(
    id         uuid                                NOT NULL,
    to_email   varchar(255)                        NOT NULL,
    from_email varchar(255)                        NOT NULL,
    subject    varchar(255),
    content    text, -- Using TEXT for email body as it can be large
    user_id    uuid, -- Changed to UUID to match your User entity ID type

    -- Audit fields
    created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp DEFAULT NULL,
    deleted_at timestamp DEFAULT NULL,

    PRIMARY KEY (id)
);

-- Index to quickly find email history for a specific user
CREATE INDEX idx_mail_user_id ON mail (user_id);

-- Index to filter emails by recipient (useful for support/debugging)
CREATE INDEX idx_mail_to_email ON mail (to_email);