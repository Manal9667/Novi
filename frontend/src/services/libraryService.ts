import { apiClient } from './http';
import type { Page, ReadingStatus, UserBook } from '../types';

export interface LibraryQuery {
  status?: ReadingStatus;
  page?: number;
  size?: number;
}

/** The current user's personal library. All calls are scoped server-side to the authenticated user. */
export const libraryService = {
  async getLibrary({ status, page = 0, size = 50 }: LibraryQuery = {}): Promise<Page<UserBook>> {
    const params: Record<string, unknown> = { page, size };
    if (status) params.status = status;
    const { data } = await apiClient.get<Page<UserBook>>('/library', { params });
    return data;
  },

  async addBook(bookId: number, status: ReadingStatus): Promise<UserBook> {
    const { data } = await apiClient.post<UserBook>('/library/books', { bookId, status });
    return data;
  },

  async updateStatus(bookId: number | string, status: ReadingStatus): Promise<UserBook> {
    const { data } = await apiClient.patch<UserBook>(`/library/books/${bookId}/status`, { status });
    return data;
  },

  async removeBook(bookId: number | string): Promise<void> {
    await apiClient.delete(`/library/books/${bookId}`);
  }
};
