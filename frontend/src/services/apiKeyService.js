import api from './api';
export const listApiKeys = () => api.get('/api/v1/api-keys');
export const createApiKey = (name) => api.post('/api/v1/api-keys', { name });
export const revokeApiKey = (id) => api.delete(`/api/v1/api-keys/${id}`);
