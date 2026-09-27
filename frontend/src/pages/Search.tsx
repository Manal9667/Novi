import React, { useState } from 'react';
import { BookCard } from '../components/BookCard';
import { ErrorState } from '../components/states/ErrorState';
import { EmptyState } from '../components/states/EmptyState';
import { Spinner } from '../components/states/Spinner';
import { bookService, getApiErrorMessage } from '../services';
import type { BookSummary } from '../types';

type SearchStatus = 'idle' | 'loading' | 'success' | 'error';

export default function Search() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<BookSummary[]>([]);
  const [status, setStatus] = useState<SearchStatus>('idle');
  const [error, setError] = useState<string | null>(null);
  const [lastQuery, setLastQuery] = useState('');

  async function runSearch(q: string) {
    setStatus('loading');
    setError(null);
    setLastQuery(q);
    try {
      setResults(await bookService.search(q));
      setStatus('success');
    } catch (err) {
      setError(getApiErrorMessage(err, 'Search failed. Please try again.'));
      setStatus('error');
    }
  }

  function handleSearch(e: React.FormEvent) {
    e.preventDefault();
    const trimmed = query.trim();
    if (!trimmed) return;
    void runSearch(trimmed);
  }

  return (
    <div className="page">
      <h1>Search books</h1>
      <form onSubmit={handleSearch} className="search-form" role="search">
        <label htmlFor="search-input" className="visually-hidden">
          Search by title or author
        </label>
        <input
          id="search-input"
          type="search"
          placeholder="Search by title, author…"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <button type="submit" disabled={status === 'loading'}>
          {status === 'loading' ? 'Searching…' : 'Search'}
        </button>
      </form>

      {status === 'loading' && <Spinner label="Searching…" />}

      {status === 'error' && error && (
        <ErrorState message={error} onRetry={() => lastQuery && runSearch(lastQuery)} />
      )}

      {status === 'success' && (
        <>
          <div className="book-grid">
            {results.map((book) => (
              <BookCard key={book.id} book={book} />
            ))}
          </div>
          {results.length === 0 && (
            <EmptyState message={`No books found for “${lastQuery}”. Try a different search.`} />
          )}
        </>
      )}

      {status === 'idle' && (
        <EmptyState message="Search Novi's catalog by title or author to start building your library." />
      )}
    </div>
  );
}
