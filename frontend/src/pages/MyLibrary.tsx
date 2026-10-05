import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { BookCard } from '../components/BookCard';
import { EmptyState } from '../components/states/EmptyState';
import { ErrorState } from '../components/states/ErrorState';
import { BookGridSkeleton } from '../components/states/Skeleton';
import { EmptyShelfIllustration } from '../components/icons';
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
      <h1>My library</h1>
      <p className="subtle">Every book you've shelved, in one place.</p>

      <div className="tabs" role="tablist" aria-label="Filter library by reading status">
        {FILTERS.map((f) => (
          <button
            key={f.value}
            role="tab"
            aria-selected={filter === f.value}
            className={filter === f.value ? 'tab active' : 'tab'}
            onClick={() => setFilter(f.value)}
          >
            {f.label}
          </button>
        ))}
      </div>

      {library.status === 'loading' && <BookGridSkeleton count={10} />}
      {library.status === 'error' && <ErrorState message={library.error ?? 'Could not load your library.'} onRetry={library.reload} />}
      {library.status === 'success' && library.data && (
        library.data.length > 0 ? (
          <div className="book-grid shelf-grid">
            {library.data.map((ub) => (
              <BookCard key={ub.id} book={ub.book} />
            ))}
          </div>
        ) : (
          <EmptyState
            illustration={<EmptyShelfIllustration className="illo" />}
            title={filter === 'ALL' ? 'Your library is empty' : 'Nothing on this shelf yet'}
            message={
              filter === 'ALL'
                ? "Let's find your first book to curl up with."
                : 'No books in this view yet. Try another shelf or add something new.'
            }
            action={<Link to="/find" className="button-link">Find a book</Link>}
          />
        )
      )}
    </div>
  );
}
