-- Add updated_at column to subscriptions, api_keys, events, delivery_attempts
ALTER TABLE subscriptions ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
ALTER TABLE api_keys ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
ALTER TABLE events ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
ALTER TABLE delivery_attempts ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
