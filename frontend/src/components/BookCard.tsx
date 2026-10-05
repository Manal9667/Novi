import React from 'react';
import { Link } from 'react-router-dom';
import type { BookSummary } from '../types';
import { BookCover } from './BookCover';

export function BookCard({ book }: { book: BookSummary }) {
  return (
    <Link to={`/books/${book.id}`} className="book-card">
      <BookCover src={book.coverImageUrl} title={book.title} />
      <div className="book-title">{book.title}</div>
      <div className="book-authors">{book.authorNames.join(', ')}</div>
    </Link>
  );
}
