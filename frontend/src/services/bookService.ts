import { apiClient } from './http';
import type { BookDetail, BookSummary, Page } from '../types';

/** Read access to the seeded book catalog. */
export const bookService = {
  async getById(id: number | string): Promise<BookDetail> {
    const { data } = await apiClient.get<BookDetail>(`/books/${id}`);
    return data;
  },

  async browse(page = 0, size = 24): Promise<Page<BookSummary>> {
    const { data } = await apiClient.get<Page<BookSummary>>('/books', { params: { page, size } });
    return data;
  }
};
