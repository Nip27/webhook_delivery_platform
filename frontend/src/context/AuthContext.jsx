import { createContext, useContext, useState, useCallback } from 'react';
import { saveTokens, clearTokens, getAccessToken } from '../utils/storage';
import * as authService from '../services/authService';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const token = getAccessToken();
    if (!token) return null;
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return { id: payload.sub, email: payload.email };
    } catch {
      return null;
    }
  });

  const login = useCallback(async (email, password) => {
    const response = await authService.login(email, password);
    const { accessToken, refreshToken, user: userData } = response.data;
    saveTokens(accessToken, refreshToken);
    setUser(userData);
    return userData;
  }, []);

  const register = useCallback(async (email, password, fullName) => {
    const response = await authService.register(email, password, fullName);
    const { accessToken, refreshToken, user: userData } = response.data;
    saveTokens(accessToken, refreshToken);
    setUser(userData);
    return userData;
  }, []);

  const logout = useCallback(() => {
    clearTokens();
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider value={{ user, login, register, logout, isAuthenticated: !!user }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within AuthProvider');
  return context;
}
