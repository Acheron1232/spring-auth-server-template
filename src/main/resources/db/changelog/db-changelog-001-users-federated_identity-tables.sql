--liquibase formatted sql

--changeset acheron:1
--comment Initial schema for Users and Federated Identity with Soft Delete support

-- =================================================================================================
-- 1. USERS TABLE
-- =================================================================================================
CREATE TABLE IF NOT EXISTS users
(
    id
                   UUID
        PRIMARY
            KEY
                                                        DEFAULT
                                                            gen_random_uuid
                                                            (
                                                            ),

    -- Auditing fields
    created_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now
                                                                (
                                                                ),
    updated_at     TIMESTAMP
                       WITHOUT TIME ZONE,
    deleted_at     TIMESTAMP
                       WITHOUT TIME ZONE,

    -- Core fields
    email          VARCHAR(255)                NOT NULL,
    username       VARCHAR(255)                NOT NULL,
    password_hash  VARCHAR(255), -- Nullable for OAuth-only users
    role           VARCHAR(50)                 NOT NULL DEFAULT 'USER',

    -- Status flags
    email_verified BOOLEAN                     NOT NULL DEFAULT FALSE,
    enabled        BOOLEAN                     NOT NULL DEFAULT TRUE,
    locked         BOOLEAN                     NOT NULL DEFAULT FALSE,
    mfa_enabled    BOOLEAN                     NOT NULL DEFAULT FALSE,
    mfa_secret     VARCHAR(255)
);

-- Documentation comments for Users
COMMENT
    ON TABLE users IS 'Core user table supporting local and OAuth authentication';
COMMENT
    ON COLUMN users.deleted_at IS 'Timestamp for soft-delete logic. If NULL, user is active';
COMMENT
    ON COLUMN users.password_hash IS 'BCrypt hash. Can be NULL if user registered via OAuth';
COMMENT
    ON COLUMN users.role IS 'User permission level (USER, ADMIN, etc.) stored as string';

-- =================================================================================================
-- 2. FEDERATED IDENTITY TABLE (OAuth)
-- =================================================================================================
CREATE TABLE IF NOT EXISTS federated_identity
(
    id
                      UUID
        PRIMARY
            KEY
                                                           DEFAULT
                                                               gen_random_uuid
                                                               (
                                                               ),

    -- Auditing fields
    created_at        TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now
                                                                   (
                                                                   ),
    updated_at        TIMESTAMP
                          WITHOUT TIME ZONE,
    deleted_at        TIMESTAMP
                          WITHOUT TIME ZONE,

    -- Provider details
    provider          VARCHAR(50)                 NOT NULL, -- e.g., GITHUB, GOOGLE
    provider_user_id  VARCHAR(255)                NOT NULL, -- Unique ID from external provider
    provider_username VARCHAR(255),                         -- Email or Login from provider

-- Tokens
    access_token      TEXT,
    refresh_token     TEXT,
    token_expires_at  TIMESTAMP
                          WITHOUT TIME ZONE,

    -- Metadata
    provider_metadata JSONB,                                -- Raw JSON payload from provider

-- Relationships
    user_id           UUID                        NOT NULL,
    CONSTRAINT fk_federated_identity_user
        FOREIGN KEY
            (
             user_id
                )
            REFERENCES users
                (
                 id
                    )
            ON DELETE CASCADE
);

-- Documentation comments for Federated Identity
COMMENT
    ON TABLE federated_identity IS 'Links local users to external OAuth providers (Google, GitHub, etc.)';
COMMENT
    ON COLUMN federated_identity.provider_metadata IS 'Stores raw user info from provider as JSONB for flexibility';

-- =================================================================================================
-- 3. INDEXES
-- =================================================================================================

-- Partial indexes for Soft Delete logic (User)
-- Ensures email/username uniqueness only among ACTIVE (non-deleted) users
CREATE UNIQUE INDEX idx_users_email_unique_active
    ON users (email) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX idx_users_username_unique_active
    ON users (username) WHERE deleted_at IS NULL;

-- Indexes for Federated Identity
CREATE INDEX idx_federated_identity_user_id
    ON federated_identity (user_id);

-- Ensures one user cannot link the same external account twice (unless soft-deleted)
CREATE UNIQUE INDEX idx_federated_unique_provider_user
    ON federated_identity (provider, provider_user_id) WHERE deleted_at IS NULL;

-- GIN Index for efficient JSONB searching
CREATE INDEX idx_federated_metadata_gin
    ON federated_identity USING gin (provider_metadata);