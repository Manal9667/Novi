import React from 'react';
import { useLocation, useParams } from 'react-router-dom';
import { BookCover } from '../components/BookCover';
import { SaveButton } from '../components/SaveButton';
import { AsyncSection } from '../components/states/AsyncSection';
import { useAuth } from '../context/AuthContext';
import { useAsync } from '../hooks/useAsync';
import { bookService, libraryService } from '../services';
import type { BookDetail } from '../types';

interface DetailBundle {
  book: BookDetail;
  saved: boolean;
}

interface RecommendationContext {
  explanation?: string;
  relevanceScore?: number;
}

export default function BookDetails() {
  const { id } = useParams();
  const location = useLocation();
  const { isAuthenticated } = useAuth();
  const recContext = (location.state ?? {}) as RecommendationContext;

  const state = useAsync<DetailBundle>(async () => {
    if (!id) throw new Error('No book specified');
    const book = await bookService.getById(id);
    let saved = false;
    if (isAuthenticated) {
      const page = await libraryService.getSaved({ size: 200 });
      saved = page.content.some((ub) => ub.book.id === Number(id));
    }
    return { book, saved };
  }, [id, isAuthenticated]);

  return (
    <div className="page book-details">
      <AsyncSection state={state} loadingLabel="Finding this book…">
        {({ book, saved }) => (
          <>
            <div className="book-hero">
              <div className="book-hero-cover">
                <BookCover src={book.coverImageUrl} title={book.title} />
              </div>

              <div className="book-hero-info">
                <h1>{book.title}</h1>
                <p className="book-byline">{book.authors.map((a) => a.name).join(', ')}</p>
                {book.publicationDate && (
                  <p className="subtle book-year">{new Date(book.publicationDate).getFullYear()}</p>
                )}

                {recContext.explanation && (
                  <div className="why-note">
                    <span className="rec-note-label hand">
                      Why Novi recommended it{typeof recContext.relevanceScore === 'number' ? ` · ${recContext.relevanceScore}% match` : ''}
                    </span>
                    <p>{recContext.explanation}</p>
                  </div>
                )}

                {book.description && <p className="book-description measure">{book.description}</p>}

                {(book.genres.length > 0 || book.themes.length > 0) && (
                  <div className="book-tags">
                    {book.genres.map((g) => (
                      <span key={`g-${g.id}`} className="chip chip-static">{g.name}</span>
                    ))}
                    {book.themes.map((t) => (
                      <span key={`t-${t}`} className="chip chip-static chip-theme">{t}</span>
                    ))}
                  </div>
                )}

                <div className="book-actions">
                  <SaveButton bookId={book.id} initialSaved={saved} />
                </div>
              </div>
            </div>
          </>
        )}
      </AsyncSection>
    </div>
  );
}
