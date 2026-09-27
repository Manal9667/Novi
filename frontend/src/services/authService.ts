import { apiClient } from './http';
import type { AuthResponse } from '../types';

export interface RegisterPayload {
  username: string;
  displayName: string;
  password: string;
  confirmPassword: string;
}

/** Authentication endpoints (public). */
export const authService = {
  async login(username: string, password: string): Promise<AuthResponse> {
    const { data } = await apiClient.post<AuthResponse>('/auth/login', { username, password });
    return data;
  },

  async register(payload: RegisterPayload): Promise<AuthResponse> {
    const { data } = await apiClient.post<AuthResponse>('/auth/register', payload);
    return data;
  }
};
