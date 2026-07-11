import api from './api';
export const listDeliveries = (page = 0, size = 20) =>
  api.get(`/api/v1/deliveries?page=${page}&size=${size}`);
