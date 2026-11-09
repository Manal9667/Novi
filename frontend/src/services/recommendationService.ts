import { apiClient } from './http';
import type { Recommendation } from '../types';

/** Novi's AI recommendation engine: natural-language book discovery. */
export const recommendationService = {
  /**
   * Describe what you want to read in natural language; the RAG pipeline returns
   * ranked, explained recommendations. Backed by the public /recommendations/ask
   * endpoint (works signed-out).
   */
  async findBooks(query: string): Promise<Recommendation[]> {
    const { data } = await apiClient.post<Recommendation[]>('/recommendations/ask', { query });
    return data;
  }
};
