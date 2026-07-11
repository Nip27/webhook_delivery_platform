# Demo Assets

Add resume/demo assets here after you deploy or run the project locally.

Recommended assets:

- Login/register screenshot
- Dashboard screenshot
- Endpoint and subscription setup screenshot
- Delivery logs screenshot showing `SUCCESS`
- Delivery logs screenshot showing retry or `DEAD_LETTERED`
- Short video showing event publish -> webhook.site receipt -> delivery log update

Suggested recording flow:

1. Start PostgreSQL, Redis, backend, and frontend.
2. Register a user.
3. Create an API key.
4. Create a webhook endpoint using webhook.site.
5. Subscribe to `order.created`.
6. Publish an event.
7. Show webhook.site receiving the signed request.
8. Show dashboard and delivery logs updating.
