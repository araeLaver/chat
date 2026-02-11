import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { User } from '../types/auth';
import { authApi } from '../api/auth';

interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (username: string, password: string) => Promise<void>;
  register: (username: string, password: string, displayName: string) => Promise<void>;
  guestLogin: () => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const savedToken = localStorage.getItem('token');
    const savedUser = localStorage.getItem('user');
    if (savedToken && savedUser) {
      try {
        setToken(savedToken);
        setUser(JSON.parse(savedUser));
      } catch {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
      }
    }
    setIsLoading(false);
  }, []);

  const saveAuth = (token: string, user: User) => {
    localStorage.setItem('token', token);
    localStorage.setItem('user', JSON.stringify(user));
    setToken(token);
    setUser(user);
  };

  const login = useCallback(async (username: string, password: string) => {
    const res = await authApi.login({ username, password });
    const data = res.data;
    saveAuth(data.token, {
      id: data.userId,
      username: data.username,
      displayName: data.displayName || data.username,
      phoneNumber: data.phoneNumber,
    });
  }, []);

  const register = useCallback(async (username: string, password: string, displayName: string) => {
    const res = await authApi.register({ username, password, displayName });
    const data = res.data;
    saveAuth(data.token, {
      id: data.userId,
      username: data.username,
      displayName: data.displayName || data.username,
      phoneNumber: data.phoneNumber,
    });
  }, []);

  const guestLogin = useCallback(async () => {
    const res = await authApi.guestLogin();
    const data = res.data;
    saveAuth(data.token, {
      id: data.user.id,
      username: data.user.username,
      displayName: data.user.displayName,
    });
  }, []);

  const logout = useCallback(() => {
    authApi.logout().catch(() => {});
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setToken(null);
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!token && !!user,
        isLoading,
        login,
        register,
        guestLogin,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
