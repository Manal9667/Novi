import React, { useEffect, useState } from 'react';
import { apiClient } from '../api/client';
import { BookCard } from '../components/BookCard';
import type { ReadingStatus, UserBook } from '../types';

const FILTERS: { label: string; value: ReadingStatus | 'ALL' }[] = [
  { label: 'All', value: 'ALL' },
  { label: 'Want to Read', value: 'WANT_TO_READ' },
  { label: 'Currently Reading', value: 'CURRENTLY_READING' },
  { label: 'Read', value: 'READ' },
  { label: 'DNF', value: 'DNF' }
];

export default function MyLibrary() {
  const [books, setBooks] = useState<UserBook[]>([]);
  const [filter, setFilter] = useState<ReadingStatus | 'ALL'>('ALL');

  useEffect(() => {
    const params = filter === 'ALL' ? {} : { status: filter };
    apiClient.get<UserBook[]>('/library', { params }).then((res) => setBooks(res.data));
  }, [filter]);

  return (
    <div className="page">
      <h1>My Library</h1>
      <div className="filter-bar">
        {FILTERS.map((f) => (
          <button
            key={f.value}
            className={filter === f.value ? 'active' : ''}
            onClick={() => setFilter(f.value)}
          >
            {f.label}
          </button>
        ))}
      </div>
      <div className="book-grid">
        {books.map((ub) => <BookCard key={ub.id} book={ub.book} />)}
      </div>
      {books.length === 0 && <p className="subtle">No books in this view yet.</p>}
    </div>
  );
}
