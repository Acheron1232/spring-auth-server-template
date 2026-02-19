--liquibase formatted sql

--changeset acheron:1
--comment Add auth history audit log and user token version for security-stamp revocation

-- =================================================================================================
-- 1. users.token_version
-- =================================================================================================
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS token_version UUID NOT NULL DEFAULT gen_random_uuid();

-- =================================================================================================
-- 2. auth_history table
-- =================================================================================================
CREATE TABLE IF NOT EXISTS auth_history
(
    id           UUID                                NOT NULL DEFAULT gen_random_uuid(),
    user_id      UUID                                NOT NULL,
    ip_address   VARCHAR(64)                         NOT NULL,
    user_agent   VARCHAR(512)                        NOT NULL,
    timestamp    TIMESTAMP WITHOUT TIME ZONE         NOT NULL,
    location     VARCHAR(128)                        NOT NULL,
    login_method VARCHAR(32)                         NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT fk_auth_history_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_auth_history_user_time
    ON auth_history (user_id, timestamp DESC);
