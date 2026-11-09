import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import type { Recommendation } from '../types';

const findBooksMock = vi.fn();
vi.mock('../services', () => ({
  recommendationService: { findBooks: (...a: unknown[]) => findBooksMock(...a) },
  getApiErrorMessage: (_e: unknown, f: string) => f,
  libraryService: { save: vi.fn(), remove: vi.fn() },
  hasStatus: () => false
}));
vi.mock('../context/AuthContext', () => ({ useAuth: () => ({ isAuthenticated: false }) }));

import FindYourNextRead from './FindYourNextRead';

const rec: Recommendation = {
  bookId: 2,
  title: 'The Silent Patient',
  authors: ['Alex Michaelides'],
  coverImageUrl: null,
  description: 'A tense thriller.',
  genres: ['Psychological Thriller'],
  themes: ['twist'],
  relevanceScore: 87,
  explanation: 'A twist-driven match.'
};

function renderPage() {
  render(
    <MemoryRouter>
      <FindYourNextRead />
    </MemoryRouter>
  );
}

async function submit(query: string) {
  await userEvent.type(screen.getByLabelText(/describe the book you want/i), query);
  await userEvent.click(screen.getByRole('button', { name: /find my next read/i }));
}

describe('FindYourNextRead', () => {
  beforeEach(() => findBooksMock.mockReset());

  it('submits the query and renders recommendation results', async () => {
    findBooksMock.mockResolvedValue([rec]);
    renderPage();
    await submit('a psychological thriller');

    expect(await screen.findByText('The Silent Patient')).toBeInTheDocument();
    expect(findBooksMock).toHaveBeenCalledWith('a psychological thriller');
  });

  it('shows an empty state when no books match', async () => {
    findBooksMock.mockResolvedValue([]);
    renderPage();
    await submit('nonexistent vibe');

    expect(await screen.findByText(/nothing quite matched/i)).toBeInTheDocument();
  });
});
