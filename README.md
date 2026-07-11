# WebhookFlow — Webhook Delivery Platform

A production-style event-driven webhook delivery system with async delivery workers, exponential-backoff retry logic, dead-letter queuing, HMAC signatures, JWT + API key authentication, and a React dashboard.

---

## Architecture Overview

```
┌──────────────────────────────────────────────────────────────────┐
│                          React Frontend                          │
│   Dashboard │ Endpoints │ Subscriptions │ Deliveries │ API Keys  │
└─────────────────────────────┬────────────────────────────────────┘
                              │ HTTP (Axios + JWT)
┌─────────────────────────────▼────────────────────────────────────┐
│                     Spring Boot Backend                          │
│                                                                  │
│  AuthController  EndpointController  EventController             │
│  SubscriptionController  DeliveryController  DashboardController │
│                                                                  │
│  ┌─────────────┐   ┌──────────────┐   ┌────────────────────┐    │
│  │ JWT Filter  │   │ ApiKey Filter│   │ Spring Security    │    │
│  └─────────────┘   └──────────────┘   └────────────────────┘    │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                     Services                             │   │
│  │  AuthService  │  EventService  │  DeliveryService        │   │
│  │  ApiKeyService│  SubscriptionService  │  DashboardService │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  ┌──────────────────────┐   ┌──────────────────────────────┐    │
│  │    DeliveryWorker    │   │       RetryScheduler         │    │
│  │  (polls Redis queue) │   │  (promotes due retry tasks)  │    │
│  └──────────────────────┘   └──────────────────────────────┘    │
└──────────┬──────────────────────────┬───────────────────────────┘
           │                          │
  ┌────────▼───────┐        ┌────────▼──────────┐
  │  PostgreSQL 16 │        │     Redis 7        │
  │                │        │                    │
  │  users         │        │  webhook:delivery  │
  │  api_keys      │        │    :queue (LIST)   │
  │  webhook_      │        │  webhook:retry     │
  │    endpoints   │        │    :queue (ZSET)   │
  │  subscriptions │        │  webhook:idempotency│
  │  events        │        │    :* (STRING+TTL) │
  │  delivery_     │        └────────────────────┘
  │    attempts    │
  └────────────────┘
```

### Delivery Flow

1. Client POSTs event to `/api/v1/events/publish` with JWT or API key
2. `EventService` creates an `Event` row, finds active subscriptions, creates `DeliveryAttempt` rows (status=PENDING), enqueues `DeliveryTask` objects to Redis LIST
3. `DeliveryWorker` (scheduled every 1 s) pops tasks, HTTP-POSTs to the target URL with HMAC signature, updates attempt status
4. On failure, exponential back-off + jitter delay is computed and the task is added to a Redis ZSET (score = unix timestamp)
5. `RetryScheduler` (every 10 s) moves due tasks from the ZSET back to the LIST
6. After `max-retries` (default 5), the attempt is marked `DEAD_LETTERED`

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.2, Spring Security, Spring Data JPA |
| Auth | JWT (JJWT 0.12) + API Key (SHA-256 hash) |
| Database | PostgreSQL 16 |
| Queue | Redis 7 (LIST for delivery, ZSET for retry) |
| Frontend | React 18, React Router 6, Axios, Tailwind CSS |
| Build | Maven, Vite |

---

## Local Development

### Prerequisites
- Java 21
- Maven 3.9+
- Node 20+
- PostgreSQL 16 running locally
- Redis 7 running locally

### 1 — Backend

```bash
# Copy env template
cp .env.example .env

# Create DB
psql -U postgres -c "CREATE DATABASE webhookplatform;"

# Start backend
cd backend
mvn spring-boot:run
# API runs on http://localhost:8080
# Swagger UI available at http://localhost:8080/swagger-ui/index.html
```

Flyway applies versioned migrations from `backend/src/main/resources/db/migration`.
Existing databases created from `database/schema.sql` are baselined automatically,
then upgraded by newer migrations. Hibernate validates the resulting schema.

### 2 — Frontend

```bash
cd frontend
npm install
npm run dev
# Runs on http://localhost:5173
```

---



## API Reference

All protected endpoints require `Authorization: Bearer <token>`.

### Auth

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/auth/register` | Register a new user |
| POST | `/api/v1/auth/login` | Login, receive JWT pair |
| POST | `/api/v1/auth/refresh` | Refresh access token |

### Webhook Endpoints

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/webhooks/endpoints` | Register a target URL |
| GET | `/api/v1/webhooks/endpoints` | List your endpoints |
| DELETE | `/api/v1/webhooks/endpoints/{id}` | Soft-delete endpoint |

### Subscriptions

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/webhooks/subscriptions` | Subscribe endpoint to event type |
| GET | `/api/v1/webhooks/subscriptions` | List subscriptions |
| DELETE | `/api/v1/webhooks/subscriptions/{id}` | Remove subscription |

### Events

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/events/publish` | JWT or `X-API-Key` | Publish an event |
| GET | `/api/v1/events` | JWT | List events (paginated) |

**Publish event body:**
```json
{
  "eventType": "order.created",
  "idempotencyKey": "order-123-attempt-1",
  "payload": { "orderId": "123", "amount": 99.99 }
}
```

### Delivery Logs

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/deliveries?page=0&size=20` | Paginated delivery log |

### API Keys

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/api-keys` | Create API key (returned once) |
| GET | `/api/v1/api-keys` | List active keys |
| DELETE | `/api/v1/api-keys/{id}` | Revoke key |

### Dashboard

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/dashboard/stats` | 30-day delivery statistics |

---

## HMAC Signature Verification

Each delivery includes the header `X-Webhook-Signature: sha256=<hex>`.

**Verification (Node.js example):**
```javascript
const crypto = require('crypto');

function verifySignature(rawBody, secretPrefix, signatureHeader) {
  const expected = 'sha256=' + crypto
    .createHmac('sha256', secretPrefix)
    .update(rawBody)
    .digest('hex');
  return crypto.timingSafeEqual(
    Buffer.from(signatureHeader),
    Buffer.from(expected)
  );
}
```

---

## Retry Policy

| Attempt | Delay (approx) |
|---|---|
| 2 | ~30 s |
| 3 | ~60 s |
| 4 | ~2 min |
| 5 | ~4 min |
| After 5 | DEAD_LETTERED |

Jitter of ±20% is applied to each delay to avoid thundering-herd.

---

## Running Tests

```bash
cd backend
mvn test
```

Tests use H2 in-memory database (`application-test.properties`). Covers:
- `HmacUtilTest` — HMAC signature correctness
- `ApiKeyGeneratorTest` — key generation and hashing
- `AuthServiceTest` — registration, login, error paths
- `AuthControllerTest` — HTTP layer validation

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `DB_USERNAME` | `postgres` | PostgreSQL user |
| `DB_PASSWORD` | `postgres` | PostgreSQL password |
| `JWT_SECRET` | *(required in prod)* | HS256 signing secret (32+ bytes) |
| `DELIVERY_MAX_RETRIES` | `5` | Max attempts before dead-letter |
| `DELIVERY_HTTP_TIMEOUT_MS` | `5000` | HTTP delivery timeout |
| `DELIVERY_WORKERS_ENABLED` | `true` | Enable delivery and retry workers |
| `REDIS_HOST` | `localhost` | Redis hostname |
| `REDIS_PORT` | `6379` | Redis port |

---

## Project Structure

```
webhook-delivery-platform/
├── backend/
│   ├── src/main/java/com/webhookplatform/
│   │   ├── config/          # Security, Redis, AppConfig
│   │   ├── controller/      # REST controllers
│   │   ├── dto/             # Request/response DTOs
│   │   ├── entity/          # JPA entities + enums
│   │   ├── exception/       # GlobalExceptionHandler
│   │   ├── repository/      # Spring Data JPA repositories
│   │   ├── security/        # JWT + API key filters
│   │   ├── service/         # Business logic
│   │   ├── util/            # HMAC, key generation, Redis keys
│   │   └── worker/          # DeliveryWorker, RetryScheduler
│   └── src/test/            # Unit + slice tests
├── frontend/
│   └── src/
│       ├── components/      # Layout, UI primitives
│       ├── context/         # AuthContext
│       ├── hooks/           # useAsync
│       ├── pages/           # Dashboard, Endpoints, Subscriptions, Logs, ApiKeys
│       ├── services/        # Axios API clients
│       └── utils/           # localStorage tokens, formatters
├── database/
│   └── schema.sql           # Full PostgreSQL DDL with indexes
├── postman/
│   └── WebhookPlatform.postman_collection.json
└── .env.example
```

## More Documentation

- [Architecture](docs/architecture.md)
- [Deployment Guide](docs/deployment.md)
- [API Examples](docs/api-examples.md)
- [Demo Asset Checklist](docs/demo.md)
