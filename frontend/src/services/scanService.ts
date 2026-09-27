import { apiClient } from './http';
import type { ConfirmScanResponse, ReadingStatus, ScanResult } from '../types';

export interface ScanDecision {
  candidateId: number;
  confirm: boolean;
  status?: ReadingStatus;
  overrideBookId?: number;
}

/** Computer-vision book scanner: single book or whole shelf, then confirm into the library. */
export const scanService = {
  async scanBook(file: File): Promise<ScanResult> {
    return upload('/scan/book', file);
  },

  async scanShelf(file: File): Promise<ScanResult> {
    return upload('/scan/shelf', file);
  },

  async getSession(sessionId: number): Promise<ScanResult> {
    const { data } = await apiClient.get<ScanResult>(`/scan/sessions/${sessionId}`);
    return data;
  },

  async confirm(sessionId: number, decisions: ScanDecision[]): Promise<ConfirmScanResponse> {
    const { data } = await apiClient.post<ConfirmScanResponse>(
      `/scan/sessions/${sessionId}/confirm`,
      { decisions }
    );
    return data;
  }
};

async function upload(endpoint: string, file: File): Promise<ScanResult> {
  const form = new FormData();
  form.append('image', file);
  const { data } = await apiClient.post<ScanResult>(endpoint, form);
  return data;
}
