import React from 'react';
import { Link } from 'react-router-dom';
import type { BookSummary } from '../types';

export function BookCard({ book }: { book: BookSummary }) {
  return (
    <Link to={`/books/${book.id}`} className="book-card">
      <div className="book-cover">
        {book.coverImageUrl ? (
          <img src={book.coverImageUrl} alt={book.title} />
        ) : (
          <div className="book-cover-placeholder">{book.title[0]}</div>
        )}
      </div>
      <div className="book-title">{book.title}</div>
      <div className="book-authors">{book.authorNames.join(', ')}</div>
    </Link>
  );
}
