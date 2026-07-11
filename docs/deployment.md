# Deployment Guide

This project can be deployed as separate frontend and backend services with managed PostgreSQL and Redis.

## Required Managed Resources

- PostgreSQL 16 or newer
- Redis 7 compatible service
- Backend runtime with Java 21
- Frontend static hosting or Node/Vite build pipeline

Examples:

- Azure App Service + Azure Database for PostgreSQL + Azure Managed Redis
- Render/Railway/Fly.io + managed PostgreSQL + managed Redis
- Kubernetes + managed PostgreSQL + managed Redis

## Backend Environment Variables

```text
SPRING_PROFILE=dev
SPRING_DATASOURCE_URL=jdbc:postgresql://<postgres-host>:5432/webhookplatform
DB_USERNAME=<database-user>
DB_PASSWORD=<database-password>
REDIS_HOST=<redis-host>
REDIS_PORT=6379
REDIS_PASSWORD=<redis-password-if-any>
JWT_SECRET=<32-plus-byte-secret>
DELIVERY_MAX_RETRIES=5
DELIVERY_HTTP_TIMEOUT_MS=5000
DELIVERY_WORKERS_ENABLED=true
```

Flyway runs automatically at startup. For an existing manually initialized database, Flyway baselines version `1` and then applies later migrations.

## Frontend Environment Variables

```text
VITE_API_BASE_URL=https://<backend-domain>
```

## Production Checklist

- Use HTTPS for frontend and backend.
- Store secrets in the cloud provider's secret manager.
- Restrict PostgreSQL and Redis network access to the backend service.
- Enable Redis authentication and TLS if supported by the provider.
- Run the GitHub Actions CI workflow before deployment.
- Monitor `/actuator/health` and `/actuator/metrics`.
- Create screenshots or a short demo video after deployment.
