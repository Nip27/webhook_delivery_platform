-- ============================================================
-- Webhook Delivery Platform — PostgreSQL Schema
-- Run this manually or let Spring JPA auto-create (ddl-auto=update)
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =====================
-- Users
-- =====================
CREATE TABLE IF NOT EXISTS users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL,
    password_hash TEXT         NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);

-- =====================
-- API Keys
-- =====================
CREATE TABLE IF NOT EXISTS api_keys (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name         VARCHAR(100) NOT NULL,
    key_hash     VARCHAR(64)  NOT NULL,
    key_prefix   VARCHAR(20)  NOT NULL,
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_used_at TIMESTAMPTZ,
    CONSTRAINT uk_api_keys_hash UNIQUE (key_hash)
);

CREATE INDEX IF NOT EXISTS idx_api_keys_user_id  ON api_keys (user_id);
CREATE INDEX IF NOT EXISTS idx_api_keys_key_hash ON api_keys (key_hash);

-- =====================
-- Webhook Endpoints
-- =====================
CREATE TABLE IF NOT EXISTS webhook_endpoints (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    url           VARCHAR(2048) NOT NULL,
    description   VARCHAR(500),
    secret_hash   TEXT          NOT NULL,
    secret_prefix VARCHAR(20)   NOT NULL,
    is_active     BOOLEAN       NOT NULL DEFAULT TRUE,
    deleted_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_webhook_endpoints_user_id    ON webhook_endpoints (user_id);
CREATE INDEX IF NOT EXISTS idx_webhook_endpoints_deleted_at ON webhook_endpoints (deleted_at);

-- =====================
-- Event Types
-- =====================
CREATE TABLE IF NOT EXISTS event_types (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_event_types_user_name UNIQUE (user_id, name)
);

-- =====================
-- Subscriptions
-- =====================
CREATE TABLE IF NOT EXISTS subscriptions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    webhook_endpoint_id UUID         NOT NULL REFERENCES webhook_endpoints (id) ON DELETE CASCADE,
    event_type_name     VARCHAR(100) NOT NULL,
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_subscriptions_endpoint_event UNIQUE (webhook_endpoint_id, event_type_name)
);

CREATE INDEX IF NOT EXISTS idx_subscriptions_user_id         ON subscriptions (user_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_event_type_name ON subscriptions (event_type_name) WHERE is_active = TRUE;

-- =====================
-- Events
-- =====================
CREATE TABLE IF NOT EXISTS events (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    event_type       VARCHAR(100) NOT NULL,
    payload          TEXT,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                         CHECK (status IN ('PENDING','PROCESSING','DELIVERED','FAILED')),
    idempotency_key  VARCHAR(100),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_events_user_id          ON events (user_id);
CREATE INDEX IF NOT EXISTS idx_events_user_idempotency ON events (user_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_events_status           ON events (status);
CREATE INDEX IF NOT EXISTS idx_events_created_at       ON events (created_at DESC);

-- =====================
-- Delivery Attempts
-- =====================
CREATE TABLE IF NOT EXISTS delivery_attempts (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id             UUID        NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    subscription_id      UUID        NOT NULL REFERENCES subscriptions (id) ON DELETE CASCADE,
    status               VARCHAR(20) NOT NULL
                             CHECK (status IN ('PENDING','SUCCESS','FAILED','DEAD_LETTERED')),
    response_status_code INTEGER,
    response_body        TEXT,
    attempt_number       INTEGER     NOT NULL DEFAULT 1,
    duration_ms          BIGINT,
    error_message        TEXT,
    next_retry_at        TIMESTAMPTZ,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_delivery_attempts_event_id        ON delivery_attempts (event_id);
CREATE INDEX IF NOT EXISTS idx_delivery_attempts_subscription_id ON delivery_attempts (subscription_id);
CREATE INDEX IF NOT EXISTS idx_delivery_attempts_status          ON delivery_attempts (status);
CREATE INDEX IF NOT EXISTS idx_delivery_attempts_created_at      ON delivery_attempts (created_at DESC);
