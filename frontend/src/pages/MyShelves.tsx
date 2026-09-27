import React, { useState } from 'react';
import { BookCard } from '../components/BookCard';
import { AsyncSection } from '../components/states/AsyncSection';
import { EmptyState } from '../components/states/EmptyState';
import { useAsync } from '../hooks/useAsync';
import { getApiErrorMessage, shelfService } from '../services';
import type { Shelf } from '../types';

export default function MyShelves() {
  const [newShelfName, setNewShelfName] = useState('');
  const [mutating, setMutating] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  const shelves = useAsync<Shelf[]>(() => shelfService.getAll(), []);

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    const name = newShelfName.trim();
    if (!name) return;
    setMutating(true);
    setActionError(null);
    try {
      await shelfService.create(name);
      setNewShelfName('');
      shelves.reload();
    } catch (err) {
      setActionError(getApiErrorMessage(err, 'Could not create that shelf.'));
    } finally {
      setMutating(false);
    }
  }

  async function handleDelete(shelfId: number) {
    setActionError(null);
    try {
      await shelfService.remove(shelfId);
      shelves.reload();
    } catch (err) {
      setActionError(getApiErrorMessage(err, 'Could not delete that shelf.'));
    }
  }

  return (
    <div className="page">
      <h1>My Shelves</h1>
      <form onSubmit={handleCreate} className="inline-form">
        <label htmlFor="new-shelf" className="visually-hidden">
          New shelf name
        </label>
        <input
          id="new-shelf"
          placeholder="New shelf name (e.g. Favorites)"
          value={newShelfName}
          onChange={(e) => setNewShelfName(e.target.value)}
          maxLength={100}
        />
        <button type="submit" disabled={mutating}>
          {mutating ? 'Creating…' : 'Create shelf'}
        </button>
      </form>

      {actionError && <p className="form-error">{actionError}</p>}

      <AsyncSection state={shelves} loadingLabel="Loading your shelves…">
        {(list) =>
          list.length > 0 ? (
            <>
              {list.map((shelf) => (
                <section key={shelf.id} className="shelf">
                  <div className="shelf-header">
                    <h2>{shelf.name}</h2>
                    <button className="link-button" onClick={() => handleDelete(shelf.id)}>
                      Delete shelf
                    </button>
                  </div>
                  {shelf.books.length > 0 ? (
                    <div className="book-grid">
                      {shelf.books.map((book) => (
                        <BookCard key={book.id} book={book} />
                      ))}
                    </div>
                  ) : (
                    <p className="subtle">No books on this shelf yet.</p>
                  )}
                </section>
              ))}
            </>
          ) : (
            <EmptyState message="You haven't created any shelves yet. Make one above to organize your books." />
          )
        }
      </AsyncSection>
    </div>
  );
}
