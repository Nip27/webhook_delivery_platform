-- Legacy reference schema.
-- Runtime schema changes are now managed by Flyway migrations in db/migration.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS api_keys (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL,
    name         VARCHAR(100) NOT NULL,
    key_hash     VARCHAR(64) NOT NULL,
    key_prefix   VARCHAR(20) NOT NULL,
    is_active    BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    last_used_at TIMESTAMP,
    CONSTRAINT fk_api_keys_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_api_keys_hash UNIQUE (key_hash)
);
CREATE INDEX IF NOT EXISTS idx_api_keys_user_id ON api_keys(user_id);
CREATE INDEX IF NOT EXISTS idx_api_keys_key_hash ON api_keys(key_hash);

CREATE TABLE IF NOT EXISTS webhook_endpoints (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL,
    url           VARCHAR(2048) NOT NULL,
    description   VARCHAR(500),
    secret_hash   VARCHAR(255) NOT NULL,
    secret_prefix VARCHAR(20) NOT NULL,
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at    TIMESTAMP,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_webhook_endpoints_user FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE INDEX IF NOT EXISTS idx_webhook_endpoints_user_id ON webhook_endpoints(user_id);

CREATE TABLE IF NOT EXISTS event_types (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_event_types_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_event_types_user_name UNIQUE (user_id, name)
);

CREATE TABLE IF NOT EXISTS subscriptions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL,
    webhook_endpoint_id UUID NOT NULL,
    event_type_name     VARCHAR(100) NOT NULL,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_subscriptions_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_subscriptions_endpoint FOREIGN KEY (webhook_endpoint_id) REFERENCES webhook_endpoints(id),
    CONSTRAINT uk_subscriptions_endpoint_event UNIQUE (webhook_endpoint_id, event_type_name)
);
CREATE INDEX IF NOT EXISTS idx_subscriptions_user_id ON subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_event_type ON subscriptions(event_type_name);

CREATE TABLE IF NOT EXISTS events (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID NOT NULL,
    event_type       VARCHAR(100) NOT NULL,
    payload          TEXT,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    idempotency_key  VARCHAR(100),
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_events_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_events_user_idempotency UNIQUE (user_id, idempotency_key)
);
CREATE INDEX IF NOT EXISTS idx_events_user_id ON events(user_id);
CREATE INDEX IF NOT EXISTS idx_events_event_type ON events(event_type);
CREATE INDEX IF NOT EXISTS idx_events_created_at ON events(created_at DESC);

CREATE TABLE IF NOT EXISTS delivery_attempts (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id             UUID NOT NULL,
    subscription_id      UUID NOT NULL,
    status               VARCHAR(20) NOT NULL,
    response_status_code INTEGER,
    response_body        TEXT,
    attempt_number       INTEGER NOT NULL DEFAULT 1,
    duration_ms          BIGINT,
    error_message        TEXT,
    next_retry_at        TIMESTAMP,
    created_at           TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_delivery_event FOREIGN KEY (event_id) REFERENCES events(id),
    CONSTRAINT fk_delivery_subscription FOREIGN KEY (subscription_id) REFERENCES subscriptions(id)
);
CREATE INDEX IF NOT EXISTS idx_delivery_attempts_event_id ON delivery_attempts(event_id);
CREATE INDEX IF NOT EXISTS idx_delivery_attempts_subscription_id ON delivery_attempts(subscription_id);
CREATE INDEX IF NOT EXISTS idx_delivery_attempts_status ON delivery_attempts(status);
CREATE INDEX IF NOT EXISTS idx_delivery_attempts_created_at ON delivery_attempts(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_delivery_attempts_status_created ON delivery_attempts(status, created_at DESC);
