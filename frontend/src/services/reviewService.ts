import { apiClient } from './http';
import type { Review } from '../types';

/** Written reviews. Editing/deleting a review is authorized to its owner server-side. */
export const reviewService = {
  async create(bookId: number | string, content: string): Promise<Review> {
    const { data } = await apiClient.post<Review>(`/books/${bookId}/reviews`, { content });
    return data;
  },

  async update(reviewId: number, content: string): Promise<Review> {
    const { data } = await apiClient.put<Review>(`/reviews/${reviewId}`, { content });
    return data;
  },

  async remove(reviewId: number): Promise<void> {
    await apiClient.delete(`/reviews/${reviewId}`);
  }
};
