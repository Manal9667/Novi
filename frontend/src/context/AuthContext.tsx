import React, { createContext, useContext, useEffect, useState } from 'react';
import { apiClient } from '../api/client';
import type { AuthResponse } from '../types';

interface StoredUser {
  userId: number;
  username: string;
  displayName: string;
}

interface AuthContextValue {
  user: StoredUser | null;
  isAuthenticated: boolean;
  login: (username: string, password: string) => Promise<void>;
  register: (username: string, displayName: string, password: string, confirmPassword: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<StoredUser | null>(() => {
    const raw = localStorage.getItem('novi_user');
    return raw ? JSON.parse(raw) : null;
  });

  useEffect(() => {
    if (user) {
      localStorage.setItem('novi_user', JSON.stringify(user));
    } else {
      localStorage.removeItem('novi_user');
    }
  }, [user]);

  function persistAuth(data: AuthResponse) {
    localStorage.setItem('novi_token', data.token);
    const stored: StoredUser = { userId: data.userId, username: data.username, displayName: data.displayName };
    setUser(stored);
  }

  async function login(username: string, password: string) {
    const { data } = await apiClient.post<AuthResponse>('/auth/login', { username, password });
    persistAuth(data);
  }

  async function register(username: string, displayName: string, password: string, confirmPassword: string) {
    const { data } = await apiClient.post<AuthResponse>('/auth/register', {
      username,
      displayName,
      password,
      confirmPassword
    });
    persistAuth(data);
  }

  function logout() {
    localStorage.removeItem('novi_token');
    setUser(null);
  }

  return (
    <AuthContext.Provider value={{ user, isAuthenticated: !!user, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider');
  return ctx;
}
