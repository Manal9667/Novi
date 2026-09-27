import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import Search from './Search';
import { bookService } from '../services';

vi.mock('../services', async () => {
  const actual = await vi.importActual<typeof import('../services')>('../services');
  return {
    ...actual,
    bookService: { search: vi.fn() }
  };
});

const mockedSearch = vi.mocked(bookService.search);

function renderSearch() {
  return render(
    <MemoryRouter>
      <Search />
    </MemoryRouter>
  );
}

describe('Search page', () => {
  beforeEach(() => {
    mockedSearch.mockReset();
  });

  it('shows an idle prompt before any search', () => {
    renderSearch();
    expect(screen.getByText(/search novi's catalog/i)).toBeInTheDocument();
  });

  it('renders results returned by the service', async () => {
    mockedSearch.mockResolvedValue([
      { id: 1, title: 'Dune', coverImageUrl: null, authorNames: ['Frank Herbert'] }
    ]);
    renderSearch();

    await userEvent.type(screen.getByRole('searchbox'), 'dune');
    await userEvent.click(screen.getByRole('button', { name: /search/i }));

    expect(await screen.findByText('Dune')).toBeInTheDocument();
    expect(mockedSearch).toHaveBeenCalledWith('dune');
  });

  it('shows an empty state when nothing matches', async () => {
    mockedSearch.mockResolvedValue([]);
    renderSearch();

    await userEvent.type(screen.getByRole('searchbox'), 'zzzz');
    await userEvent.click(screen.getByRole('button', { name: /search/i }));

    expect(await screen.findByText(/no books found/i)).toBeInTheDocument();
  });

  it('surfaces an error state when the search fails', async () => {
    mockedSearch.mockRejectedValue(new Error('boom'));
    renderSearch();

    await userEvent.type(screen.getByRole('searchbox'), 'dune');
    await userEvent.click(screen.getByRole('button', { name: /search/i }));

    expect(await screen.findByRole('alert')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /try again/i })).toBeInTheDocument();
  });
});
