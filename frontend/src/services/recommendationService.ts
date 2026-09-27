import { apiClient } from './http';
import type {
  FeedbackType,
  ReadingPersonalityResponse,
  RecommendationResponse
} from '../types';

/** AI recommendation engine: personalized picks, natural-language asks, feedback, and the taste summary. */
export const recommendationService = {
  async getPersonalized(): Promise<RecommendationResponse[]> {
    const { data } = await apiClient.get<RecommendationResponse[]>('/recommendations');
    return data;
  },

  async ask(query: string): Promise<RecommendationResponse[]> {
    const { data } = await apiClient.post<RecommendationResponse[]>('/recommendations/ask', { query });
    return data;
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
  }
};
