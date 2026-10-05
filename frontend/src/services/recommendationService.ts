import { apiClient } from './http';
import type {
  FeedbackType,
  ReadingPersonalityResponse,
  RecommendationResponse,
  WrappedResponse
} from '../types';

/** AI recommendation engine: personalized picks, natural-language asks, feedback, and the taste summary. */
export const recommendationService = {
  /** "Recommended for You": picks driven primarily by the reader's own history/taste. */
  async getPersonalized(): Promise<RecommendationResponse[]> {
    const { data } = await apiClient.get<RecommendationResponse[]>('/recommendations');
    return data;
  },

  /**
   * "Find Your Next Read": explicit natural-language request where the query is
   * the primary signal. Backed by the query-first /ask endpoint.
   */
  async findBooks(query: string): Promise<RecommendationResponse[]> {
    const { data } = await apiClient.post<RecommendationResponse[]>('/recommendations/ask', { query });
    return data;
  },

  /** @deprecated kept for back-compat; use findBooks. */
  async ask(query: string): Promise<RecommendationResponse[]> {
    return this.findBooks(query);
  },

  async sendFeedback(
    recommendationId: number,
    feedbackType: FeedbackType,
    reason?: string
  ): Promise<void> {
    await apiClient.post(`/recommendations/${recommendationId}/feedback`, { feedbackType, reason });
  },

  async getReadingPersonality(): Promise<ReadingPersonalityResponse> {
    const { data } = await apiClient.get<ReadingPersonalityResponse>(
      '/recommendations/reading-personality'
    );
    return data;
  },

  /** "Novi Wrapped": a card-based reading summary computed from the user's real data. */
  async getWrapped(): Promise<WrappedResponse> {
    const { data } = await apiClient.get<WrappedResponse>('/recommendations/wrapped');
    return data;
  }
};
