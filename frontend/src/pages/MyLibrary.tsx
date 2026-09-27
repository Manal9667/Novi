import React, { useState } from 'react';
import { BookCard } from '../components/BookCard';
import { AsyncSection } from '../components/states/AsyncSection';
import { EmptyState } from '../components/states/EmptyState';
import { useAsync } from '../hooks/useAsync';
import { libraryService } from '../services';
import type { ReadingStatus, UserBook } from '../types';

type Filter = ReadingStatus | 'ALL';

const FILTERS: { label: string; value: Filter }[] = [
  { label: 'All', value: 'ALL' },
  { label: 'Want to Read', value: 'WANT_TO_READ' },
  { label: 'Currently Reading', value: 'CURRENTLY_READING' },
  { label: 'Read', value: 'READ' },
  { label: 'DNF', value: 'DNF' }
];

export default function MyLibrary() {
  const [filter, setFilter] = useState<Filter>('ALL');

  const library = useAsync<UserBook[]>(async () => {
    const page = await libraryService.getLibrary(
      filter === 'ALL' ? { size: 200 } : { status: filter, size: 200 }
    );
    return page.content;
  }, [filter]);

  return (
    <div className="page">
      <h1>My Library</h1>
      <div className="filter-bar" role="tablist" aria-label="Filter library by reading status">
        {FILTERS.map((f) => (
          <button
            key={f.value}
            role="tab"
            aria-selected={filter === f.value}
            className={filter === f.value ? 'active' : ''}
            onClick={() => setFilter(f.value)}
          >
            {f.label}
          </button>
        ))}
      </div>

      <AsyncSection state={library} loadingLabel="Loading your library…">
        {(books) =>
          books.length > 0 ? (
            <div className="book-grid">
              {books.map((ub) => (
                <BookCard key={ub.id} book={ub.book} />
              ))}
            </div>
          ) : (
            <EmptyState
              message={
                filter === 'ALL'
                  ? 'Your library is empty. Search for a book to add your first one.'
                  : 'No books in this view yet.'
              }
            />
          )
        }
      </AsyncSection>
    </div>
  );
}
