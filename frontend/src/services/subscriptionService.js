import api from './api';
export const listSubscriptions = () => api.get('/api/v1/webhooks/subscriptions');
export const createSubscription = (webhookEndpointId, eventTypeName) =>
  api.post('/api/v1/webhooks/subscriptions', { webhookEndpointId, eventTypeName });
export const deleteSubscription = (id) => api.delete(`/api/v1/webhooks/subscriptions/${id}`);
