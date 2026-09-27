import { apiClient } from './http';
import type { UserProfile } from '../types';

/** Public-facing user profiles and reading stats. */
export const userService = {
  async getProfile(username: string): Promise<UserProfile> {
    const { data } = await apiClient.get<UserProfile>(`/users/${username}`);
    return data;
  }
};
