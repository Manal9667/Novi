import { apiClient, hasStatus } from './http';
import type { Rating } from '../types';

/** Per-book star ratings for the current user. */
export const ratingService = {
  /**
   * The current user's rating for a book, or null if they haven't rated it.
   * A 404 is an expected "no rating yet" signal, not an error.
   */
  async getMine(bookId: number | string): Promise<Rating | null> {
    try {
      const { data } = await apiClient.get<Rating>(`/books/${bookId}/ratings/mine`);
      return data;
    } catch (err) {
      if (hasStatus(err, 404)) return null;
      throw err;
    }
  },

  /** Upsert the current user's rating (1-5 stars). */
  async rate(bookId: number | string, stars: number): Promise<Rating> {
    const { data } = await apiClient.post<Rating>(`/books/${bookId}/ratings`, { stars });
    return data;
  }
};
