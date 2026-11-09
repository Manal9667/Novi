import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getApiErrorMessage, hasStatus, libraryService } from '../services';
import { BookmarkIcon } from './icons';

interface SaveButtonProps {
  bookId: number;
  initialSaved?: boolean;
  /** Called after a successful toggle, e.g. to remove the card from a saved list. */
  onChange?: (saved: boolean) => void;
}

/**
 * Save / unsave a book to the signed-in reader's library. Anonymous readers are
 * routed to the login page (discovery itself is public, saving is not).
 */
export function SaveButton({ bookId, initialSaved = false, onChange }: SaveButtonProps) {
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [saved, setSaved] = useState(initialSaved);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function toggle() {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    setPending(true);
    setError(null);
    try {
      if (saved) {
        await libraryService.remove(bookId);
        setSaved(false);
        onChange?.(false);
      } else {
        try {
          await libraryService.save(bookId);
        } catch (err) {
          // 409 = already in library; treat as saved rather than an error.
          if (!hasStatus(err, 409)) throw err;
        }
        setSaved(true);
        onChange?.(true);
      }
    } catch (err) {
      setError(getApiErrorMessage(err, 'Could not update your saved books.'));
    } finally {
      setPending(false);
    }
  }

  return (
    <span className="save-control">
      <button
        type="button"
        className={saved ? 'chip chip-saved' : 'chip'}
        onClick={toggle}
        disabled={pending}
        aria-pressed={saved}
      >
        <BookmarkIcon size={16} /> {saved ? 'Saved' : 'Save'}
      </button>
      {error && <span className="save-error" role="alert">{error}</span>}
    </span>
  );
}
