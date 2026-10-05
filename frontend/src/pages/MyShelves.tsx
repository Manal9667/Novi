import React, { useState } from 'react';
import { Shelf as ShelfRow } from '../components/Shelf';
import { Modal } from '../components/Modal';
import { AsyncSection } from '../components/states/AsyncSection';
import { EmptyState } from '../components/states/EmptyState';
import { EmptyShelfIllustration } from '../components/icons';
import { useAsync } from '../hooks/useAsync';
import { getApiErrorMessage, shelfService } from '../services';
import type { Shelf } from '../types';

export default function MyShelves() {
  const [newShelfName, setNewShelfName] = useState('');
  const [mutating, setMutating] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [pendingDelete, setPendingDelete] = useState<Shelf | null>(null);

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

  async function confirmDelete() {
    if (!pendingDelete) return;
    const shelf = pendingDelete;
    setPendingDelete(null);
    setActionError(null);
    try {
      await shelfService.remove(shelf.id);
      shelves.reload();
    } catch (err) {
      setActionError(getApiErrorMessage(err, 'Could not delete that shelf.'));
    }
  }

  return (
    <div className="page">
      <h1>My shelves</h1>
      <p className="subtle">Group your books however you like — "Favourites", "2026 reads", "Comfort rereads".</p>

      <form onSubmit={handleCreate} className="shelf-create">
        <label htmlFor="new-shelf" className="visually-hidden">New shelf name</label>
        <input
          id="new-shelf"
          placeholder="Name a new shelf…"
          value={newShelfName}
          onChange={(e) => setNewShelfName(e.target.value)}
          maxLength={100}
        />
        <button type="submit" disabled={mutating}>{mutating ? 'Building…' : 'Add shelf'}</button>
      </form>

      {actionError && <p className="form-error" role="alert">{actionError}</p>}

      <AsyncSection state={shelves} loadingLabel="Dusting off your shelves…">
        {(list) =>
          list.length > 0 ? (
            <div className="shelf-stack">
              {list.map((shelf) => (
                <section key={shelf.id} className="shelf-section">
                  <div className="shelf-section-header">
                    <h2>{shelf.name}</h2>
                    <button className="link-button" onClick={() => setPendingDelete(shelf)}>Remove shelf</button>
                  </div>
                  <ShelfRow
                    books={shelf.books}
                    empty={<p className="subtle shelf-empty">This shelf is waiting for its first book.</p>}
                  />
                </section>
              ))}
            </div>
          ) : (
            <EmptyState
              illustration={<EmptyShelfIllustration className="illo" />}
              title="No shelves yet"
              message="Make your first shelf above to start arranging your books by mood, year, or whatever feels right."
            />
          )
        }
      </AsyncSection>

      <Modal
        open={!!pendingDelete}
        title={`Remove "${pendingDelete?.name}"?`}
        confirmLabel="Remove shelf"
        danger
        onCancel={() => setPendingDelete(null)}
        onConfirm={confirmDelete}
      >
        This removes the shelf and its arrangement. Your books stay safely in your library.
      </Modal>
    </div>
  );
}
