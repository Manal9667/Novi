import { apiClient, hasStatus } from './http';
import type { Rating } from '../types';

/** Per-book star ratings for the current user. */
export const ratingService = {
  /**
   * The current user's rating for a book, or null if they haven't rated it.
   * The backend signals "no rating yet" with 204 No Content (axios resolves
   * with an empty body); a 404 is tolerated too for robustness.
   */
  async getMine(bookId: number | string): Promise<Rating | null> {
    try {
      const res = await apiClient.get<Rating>(`/books/${bookId}/ratings/mine`);
      if (res.status === 204 || !res.data) return null;
      return res.data;
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
