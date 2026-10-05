import React from 'react';
import { BookCard } from './BookCard';
import type { BookSummary } from '../types';

interface ShelfProps {
  books: BookSummary[];
  /** Shown when there are no books on the shelf. */
  empty?: React.ReactNode;
}

/**
 * A horizontal row of books standing on a wooden shelf plank. Scrolls sideways
 * when it overflows. Used on Home and elsewhere for shelf-style rows.
 */
export function Shelf({ books, empty }: ShelfProps) {
  if (books.length === 0) {
    return <>{empty}</>;
  }
  return (
    <div className="shelf">
      <div className="shelf-books">
        {books.map((book) => (
          <BookCard key={book.id} book={book} />
        ))}
      </div>
      <div className="shelf-plank" aria-hidden="true" />
    </div>
  );
}
