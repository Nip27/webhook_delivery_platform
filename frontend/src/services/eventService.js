import api from './api';
export const publishEvent = (eventType, payload, idempotencyKey) =>
  api.post('/api/v1/events/publish', { eventType, payload, idempotencyKey });
export const listEvents = (page = 0, size = 20) =>
  api.get(`/api/v1/events?page=${page}&size=${size}`);
