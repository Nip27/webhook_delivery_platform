SET LOCAL TIME ZONE 'UTC';

ALTER TABLE users
    ALTER COLUMN password_hash TYPE VARCHAR(255),
    ALTER COLUMN created_at TYPE TIMESTAMP USING created_at::timestamp,
    ALTER COLUMN updated_at TYPE TIMESTAMP USING updated_at::timestamp;

ALTER TABLE api_keys
    ALTER COLUMN created_at TYPE TIMESTAMP USING created_at::timestamp,
    ALTER COLUMN last_used_at TYPE TIMESTAMP USING last_used_at::timestamp;

ALTER TABLE webhook_endpoints
    ALTER COLUMN secret_hash TYPE VARCHAR(255),
    ALTER COLUMN deleted_at TYPE TIMESTAMP USING deleted_at::timestamp,
    ALTER COLUMN created_at TYPE TIMESTAMP USING created_at::timestamp,
    ALTER COLUMN updated_at TYPE TIMESTAMP USING updated_at::timestamp;

ALTER TABLE event_types
    ALTER COLUMN created_at TYPE TIMESTAMP USING created_at::timestamp;

ALTER TABLE subscriptions
    ALTER COLUMN created_at TYPE TIMESTAMP USING created_at::timestamp;

ALTER TABLE events
    ALTER COLUMN created_at TYPE TIMESTAMP USING created_at::timestamp;

ALTER TABLE delivery_attempts
    ALTER COLUMN next_retry_at TYPE TIMESTAMP USING next_retry_at::timestamp,
    ALTER COLUMN created_at TYPE TIMESTAMP USING created_at::timestamp;
