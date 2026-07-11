# Architecture

WebhookFlow is a production-style webhook delivery service. It accepts events from client applications, persists them in PostgreSQL, queues delivery work in Redis, and asynchronously POSTs webhook payloads to subscribed endpoints.

## Components

- React frontend: dashboard, webhook endpoints, subscriptions, delivery logs, API keys.
- Spring Boot API: authentication, event publishing, subscription management, dashboards.
- PostgreSQL: durable system of record for users, API keys, endpoints, events, subscriptions, and delivery attempts.
- Redis: operational queues for pending deliveries, delayed retries, idempotency keys, and dead-letter entries.
- Delivery worker: polls Redis, sends webhook requests, records attempts, schedules retries.
- Retry scheduler: promotes due retry tasks from the Redis sorted set back to the delivery queue.
- Dead-letter queue: stores exhausted deliveries for inspection and replay.

## Delivery Flow

1. A client publishes an event through `POST /api/v1/events/publish`.
2. The backend stores an `events` row and one `delivery_attempts` row per active subscription.
3. A `DeliveryTask` is pushed to `webhook:delivery:queue`.
4. `DeliveryWorker` pops the task, signs the payload, and sends it to the target endpoint.
5. Successful deliveries are marked `SUCCESS`.
6. Failed deliveries are retried with exponential backoff and jitter through `webhook:retry:queue`.
7. Exhausted deliveries are marked `DEAD_LETTERED` and copied to `webhook:dead-letter:queue`.

## Tradeoffs

- Redis LIST/ZSET keeps the implementation understandable for a portfolio project, but a production system may prefer Redis Streams, Kafka, SQS, or another broker with stronger acknowledgment semantics.
- Webhook delivery is at-least-once in spirit, so consumers should treat webhook events as idempotent.
- Flyway now owns schema evolution. Hibernate validates the schema instead of mutating it at runtime.
- The DLQ replay API is an operational feature and should usually be restricted to admins in a real organization.
- Managed PostgreSQL and Redis are recommended for public production deployment.
