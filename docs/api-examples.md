# API Examples

## Register

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123","fullName":"Demo User"}'
```

## Publish Event With API Key

```bash
curl -X POST http://localhost:8080/api/v1/events/publish \
  -H "Content-Type: application/json" \
  -H "X-API-Key: <api-key>" \
  -d '{
    "eventType": "order.created",
    "payload": {"orderId": "ord_123", "amount": 99.99},
    "idempotencyKey": "order-ord_123-created"
  }'
```

## Inspect Dead Letters

```bash
curl http://localhost:8080/api/v1/dead-letters?limit=50 \
  -H "Authorization: Bearer <access-token>"
```

## Replay Dead Letter

```bash
curl -X POST "http://localhost:8080/api/v1/dead-letters/replay?eventId=<event-id>&subscriptionId=<subscription-id>" \
  -H "Authorization: Bearer <access-token>"
```
