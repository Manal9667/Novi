import { apiClient } from './http';
import type { Page, ReadingHistoryEntry } from '../types';

/** The current user's chronological reading-activity feed. */
export const readingHistoryService = {
  async get(page = 0, size = 50): Promise<Page<ReadingHistoryEntry>> {
    const { data } = await apiClient.get<Page<ReadingHistoryEntry>>('/reading-history', {
      params: { page, size }
    });
    return data;
  }
};
