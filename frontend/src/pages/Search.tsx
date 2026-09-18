import React, { useState } from 'react';
import { apiClient } from '../api/client';
import { BookCard } from '../components/BookCard';
import type { BookSummary } from '../types';

export default function Search() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<BookSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSearch(e: React.FormEvent) {
    e.preventDefault();
    if (!query.trim()) return;
    setLoading(true);
    setError(null);
    try {
      const { data } = await apiClient.get<BookSummary[]>('/books', { params: { q: query } });
      setResults(data);
    } catch (err) {
      setError('Search failed. Please try again.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="page">
      <h1>Search books</h1>
      <form onSubmit={handleSearch} className="search-form">
        <input
          placeholder="Search by title, author..."
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <button type="submit" disabled={loading}>{loading ? 'Searching...' : 'Search'}</button>
      </form>
      {error && <p className="form-error">{error}</p>}
      <div className="book-grid">
        {results.map((book) => (
          <BookCard key={book.id} book={book} />
        ))}
      </div>
      {!loading && results.length === 0 && query && <p className="subtle">No results yet - try a different search.</p>}
    </div>
  );
}
