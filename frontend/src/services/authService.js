import api from './api';
export const register = (email, password, fullName) =>
  api.post('/api/v1/auth/register', { email, password, fullName });
export const login = (email, password) =>
  api.post('/api/v1/auth/login', { email, password });
export const refresh = (refreshToken) =>
  api.post('/api/v1/auth/refresh', { refreshToken });
