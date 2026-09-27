import { apiClient } from './http';
import type { Page, Shelf } from '../types';

/** Custom user shelves. Ownership is enforced server-side. */
export const shelfService = {
  async getAll(page = 0, size = 50): Promise<Shelf[]> {
    const { data } = await apiClient.get<Page<Shelf>>('/shelves', { params: { page, size } });
    return data.content;
  },

  async create(name: string): Promise<Shelf> {
    const { data } = await apiClient.post<Shelf>('/shelves', { name });
    return data;
  },

  async remove(shelfId: number): Promise<void> {
    await apiClient.delete(`/shelves/${shelfId}`);
  },

  async addBook(shelfId: number, bookId: number): Promise<Shelf> {
    const { data } = await apiClient.post<Shelf>(`/shelves/${shelfId}/books/${bookId}`);
    return data;
  },

  async removeBook(shelfId: number, bookId: number): Promise<Shelf> {
    const { data } = await apiClient.delete<Shelf>(`/shelves/${shelfId}/books/${bookId}`);
    return data;
  }
};
