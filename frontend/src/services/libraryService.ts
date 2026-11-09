import { apiClient } from './http';
import type { Page, ReadingStatus, UserBook } from '../types';

export interface LibraryQuery {
  status?: ReadingStatus;
  page?: number;
  size?: number;
}

/** The current user's saved books. All calls are scoped server-side to the authenticated user. */
export const libraryService = {
  async getSaved({ status, page = 0, size = 100 }: LibraryQuery = {}): Promise<Page<UserBook>> {
    const params: Record<string, unknown> = { page, size };
    if (status) params.status = status;
    const { data } = await apiClient.get<Page<UserBook>>('/library', { params });
    return data;
  },

  async save(bookId: number, status: ReadingStatus = 'SAVED'): Promise<UserBook> {
    const { data } = await apiClient.post<UserBook>('/library/books', { bookId, status });
    return data;
  },

  async remove(bookId: number | string): Promise<void> {
    await apiClient.delete(`/library/books/${bookId}`);
  }
};
