import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import type { Recommendation } from '../types';

vi.mock('../context/AuthContext', () => ({ useAuth: () => ({ isAuthenticated: false }) }));
vi.mock('../services', () => ({
  libraryService: { save: vi.fn(), remove: vi.fn() },
  getApiErrorMessage: (_e: unknown, f: string) => f,
  hasStatus: () => false
}));

import { RecommendationCard } from './RecommendationCard';

const rec: Recommendation = {
  bookId: 2,
  title: 'The Silent Patient',
  authors: ['Alex Michaelides'],
  coverImageUrl: null,
  description: 'A tense thriller.',
  genres: ['Psychological Thriller', 'Mystery'],
  themes: ['unreliable narrator'],
  relevanceScore: 87,
  explanation: 'Matches your taste for twist-driven thrillers.'
};

describe('RecommendationCard', () => {
  it('renders the book metadata, relevance and grounded explanation', () => {
    render(
      <MemoryRouter>
        <RecommendationCard rec={rec} />
      </MemoryRouter>
    );

    expect(screen.getByText('The Silent Patient')).toBeInTheDocument();
    expect(screen.getByText('Alex Michaelides')).toBeInTheDocument();
    expect(screen.getByText('Psychological Thriller')).toBeInTheDocument();
    expect(screen.getByText('Matches your taste for twist-driven thrillers.')).toBeInTheDocument();
    expect(screen.getByText(/87% match/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /save/i })).toBeInTheDocument();
  });
});
