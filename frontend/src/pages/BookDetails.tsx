import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { StarRating } from '../components/StarRating';
import { BookCover } from '../components/BookCover';
import { AsyncSection } from '../components/states/AsyncSection';
import { useAsync } from '../hooks/useAsync';
import {
  bookService,
  getApiErrorMessage,
  hasStatus,
  libraryService,
  ratingService,
  reviewService
} from '../services';
import type { BookDetail, Rating, ReadingStatus, Review } from '../types';

interface BookBundle {
  book: BookDetail;
  reviews: Review[];
  myRating: Rating | null;
  inLibrary: boolean;
  status: ReadingStatus | null;
}

const STATUS_BUTTONS: { label: string; value: ReadingStatus }[] = [
  { label: 'Want to Read', value: 'WANT_TO_READ' },
  { label: 'Reading', value: 'CURRENTLY_READING' },
  { label: 'Read', value: 'READ' },
  { label: 'DNF', value: 'DNF' }
];

export default function BookDetails() {
  const { id } = useParams();

  const bundleState = useAsync<BookBundle>(async () => {
    if (!id) throw new Error('No book specified');
    const [book, reviews, myRating, library] = await Promise.all([
      bookService.getById(id),
      bookService.getReviews(id),
      ratingService.getMine(id),
      libraryService.getLibrary({ size: 200 })
    ]);
    const entry = library.content.find((ub) => ub.book.id === Number(id));
    return { book, reviews, myRating, inLibrary: !!entry, status: entry?.status ?? null };
  }, [id]);

  // Local, mutable copies seeded from the bundle so mutations reflect at once.
  const [bookDetail, setBookDetail] = useState<BookDetail | null>(null);
  const [reviews, setReviews] = useState<Review[]>([]);
  const [myRating, setMyRating] = useState<Rating | null>(null);
  const [status, setStatus] = useState<ReadingStatus | null>(null);
  const [inLibrary, setInLibrary] = useState(false);
  const [reviewText, setReviewText] = useState('');
  const [actionError, setActionError] = useState<string | null>(null);
  const [savingReview, setSavingReview] = useState(false);

  useEffect(() => {
    if (bundleState.data) {
      setBookDetail(bundleState.data.book);
      setReviews(bundleState.data.reviews);
      setMyRating(bundleState.data.myRating);
      setStatus(bundleState.data.status);
      setInLibrary(bundleState.data.inLibrary);
    }
  }, [bundleState.data]);

  async function handleSetStatus(newStatus: ReadingStatus) {
    if (!id) return;
    setActionError(null);
    try {
      if (inLibrary) {
        await libraryService.updateStatus(id, newStatus);
      } else {
        await libraryService.addBook(Number(id), newStatus);
        setInLibrary(true);
      }
      setStatus(newStatus);
    } catch (err) {
      if (hasStatus(err, 409)) {
        try {
          await libraryService.updateStatus(id, newStatus);
          setInLibrary(true);
          setStatus(newStatus);
          return;
        } catch (retryErr) {
          setActionError(getApiErrorMessage(retryErr, 'Could not update this book.'));
          return;
        }
      }
      setActionError(getApiErrorMessage(err, 'Could not update your library.'));
    }
  }

  async function handleRate(stars: number) {
    if (!id) return;
    setActionError(null);
    try {
      setMyRating(await ratingService.rate(id, stars));
      try {
        setBookDetail(await bookService.getById(id));
      } catch {
        /* keep the previous aggregate if the refresh fails */
      }
    } catch (err) {
      setActionError(getApiErrorMessage(err, 'Could not save your rating.'));
    }
  }

  async function handleSubmitReview(e: React.FormEvent) {
    e.preventDefault();
    const content = reviewText.trim();
    if (!id || !content) return;
    setSavingReview(true);
    setActionError(null);
    try {
      const created = await reviewService.create(id, content);
      setReviews([created, ...reviews]);
      setReviewText('');
    } catch (err) {
      setActionError(getApiErrorMessage(err, 'Could not post your review.'));
    } finally {
      setSavingReview(false);
    }
  }

  return (
    <div className="page book-details">
      <AsyncSection state={bundleState} loadingLabel="Finding this book…">
        {({ book: bundleBook }) => {
          const book = bookDetail ?? bundleBook;
          return (
            <>
              <div className="book-hero">
                <div className="book-hero-cover">
                  <BookCover src={book.coverImageUrl} title={book.title} />
                </div>

                <div className="book-hero-info">
                  <h1>{book.title}</h1>
                  <p className="book-byline">{book.authors.map((a) => a.name).join(', ')}</p>

                  {book.genres.length > 0 && (
                    <div className="book-genres">
                      {book.genres.map((g) => (
                        <span key={g.id} className="chip chip-static">{g.name}</span>
                      ))}
                    </div>
                  )}

                  {book.averageRating != null && (
                    <p className="book-aggregate">
                      <StarRating value={Math.round(book.averageRating)} readOnly />
                      <span>{book.averageRating.toFixed(1)} · {book.ratingCount} {book.ratingCount === 1 ? 'rating' : 'ratings'}</span>
                    </p>
                  )}

                  {book.description && <p className="book-description measure">{book.description}</p>}

                  <div className="book-actions">
                    <span className="book-actions-label">On your shelf</span>
                    <div className="segmented" role="group" aria-label="Set reading status">
                      {STATUS_BUTTONS.map((b) => (
                        <button
                          key={b.value}
                          className={status === b.value ? 'active' : ''}
                          aria-pressed={status === b.value}
                          onClick={() => handleSetStatus(b.value)}
                        >
                          {b.label}
                        </button>
                      ))}
                    </div>
                  </div>

                  <div className="book-rate">
                    <span id="your-rating-label" className="book-actions-label">Your rating</span>
                    <StarRating value={myRating?.stars || 0} onChange={handleRate} />
                  </div>

                  {actionError && <p className="form-error" role="alert">{actionError}</p>}
                </div>
              </div>

              <section className="reviews-section">
                <h2>Margin notes</h2>
                <form onSubmit={handleSubmitReview} className="review-form card card-pad">
                  <label htmlFor="review-text" className="visually-hidden">Write a review</label>
                  <textarea
                    id="review-text"
                    placeholder="Share your thoughts on this one…"
                    value={reviewText}
                    onChange={(e) => setReviewText(e.target.value)}
                    maxLength={5000}
                  />
                  <button type="submit" disabled={savingReview || !reviewText.trim()}>
                    {savingReview ? 'Posting…' : 'Post review'}
                  </button>
                </form>

                {reviews.length > 0 ? (
                  <div className="review-list">
                    {reviews.map((review) => (
                      <article key={review.id} className="review-card">
                        <p className="review-content">{review.content}</p>
                        <p className="review-author hand">— {review.displayName}</p>
                      </article>
                    ))}
                  </div>
                ) : (
                  <p className="subtle">No notes in the margins yet. Be the first to share your thoughts.</p>
                )}
              </section>
            </>
          );
        }}
      </AsyncSection>
    </div>
  );
}
