import { apiClient } from './http';
import type { BookDetail, BookSummary, Page, Review } from '../types';

const DEFAULT_REVIEW_PAGE_SIZE = 20;

/** Book catalog, search, and per-book review reads. */
export const bookService = {
  async getById(id: number | string): Promise<BookDetail> {
    const { data } = await apiClient.get<BookDetail>(`/books/${id}`);
    return data;
  },

  async browse(page = 0, size = 24): Promise<Page<BookSummary>> {
    const { data } = await apiClient.get<Page<BookSummary>>('/books', { params: { page, size } });
    return data;
  },

  /** Live external-metadata-backed search. Requires authentication server-side. */
  async search(query: string): Promise<BookSummary[]> {
    const { data } = await apiClient.get<Page<BookSummary>>('/books/search', {
      params: { q: query }
    });
    return data.content;
  },

  async getReviews(
    bookId: number | string,
    page = 0,
    size = DEFAULT_REVIEW_PAGE_SIZE
  ): Promise<Review[]> {
    const { data } = await apiClient.get<Page<Review>>(`/books/${bookId}/reviews`, {
      params: { page, size }
    });
    return data.content;
  }
};
