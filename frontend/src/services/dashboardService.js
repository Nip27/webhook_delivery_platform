import api from './api';
export const getStats = () => api.get('/api/v1/dashboard/stats');
