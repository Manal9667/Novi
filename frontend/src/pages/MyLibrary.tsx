import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { BookCover } from '../components/BookCover';
import { EmptyState } from '../components/states/EmptyState';
import { ErrorState } from '../components/states/ErrorState';
import { BookGridSkeleton } from '../components/states/Skeleton';
import { EmptyShelfIllustration } from '../components/icons';
import { useAsync } from '../hooks/useAsync';
import { getApiErrorMessage, libraryService } from '../services';
import type { UserBook } from '../types';

export default function MyLibrary() {
  const saved = useAsync<UserBook[]>(async () => {
    const page = await libraryService.getSaved({ size: 200 });
    return page.content;
  }, []);

  const [removing, setRemoving] = useState<number | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  async function handleRemove(bookId: number) {
    setRemoving(bookId);
    setActionError(null);
    try {
      await libraryService.remove(bookId);
      saved.reload();
    } catch (err) {
      setActionError(getApiErrorMessage(err, 'Could not remove this book.'));
    } finally {
      setRemoving(null);
    }
  }

  return (
    <div className="page">
      <h1>Saved books</h1>
      <p className="subtle">Books you’ve saved from your recommendations.</p>

      {actionError && <p className="form-error" role="alert">{actionError}</p>}

      {saved.status === 'loading' && <BookGridSkeleton count={8} />}
      {saved.status === 'error' && (
        <ErrorState message={saved.error ?? 'Could not load your saved books.'} onRetry={saved.reload} />
      )}
      {saved.status === 'success' && saved.data && (
        saved.data.length > 0 ? (
          <div className="book-grid saved-grid">
            {saved.data.map((ub) => (
              <div key={ub.id} className="saved-card">
                <Link to={`/books/${ub.book.id}`} className="book-card">
                  <BookCover src={ub.book.coverImageUrl} title={ub.book.title} />
                  <div className="book-title">{ub.book.title}</div>
                  <div className="book-authors">{ub.book.authorNames.join(', ')}</div>
                </Link>
                <button
                  type="button"
                  className="chip chip-sm saved-remove"
                  onClick={() => handleRemove(ub.book.id)}
                  disabled={removing === ub.book.id}
                >
                  {removing === ub.book.id ? 'Removing…' : 'Remove'}
                </button>
              </div>
            ))}
          </div>
        ) : (
          <EmptyState
            illustration={<EmptyShelfIllustration className="illo" />}
            title="No saved books yet"
            message="Describe what you're in the mood for and save the recommendations you like."
            action={<Link to="/" className="button-link">Find a book</Link>}
          />
        )
      )}
    </div>
  );
}
