import api from './api';
export const listEndpoints = () => api.get('/api/v1/webhooks/endpoints');
export const createEndpoint = (url, description) =>
  api.post('/api/v1/webhooks/endpoints', { url, description });
export const deleteEndpoint = (id) => api.delete(`/api/v1/webhooks/endpoints/${id}`);
