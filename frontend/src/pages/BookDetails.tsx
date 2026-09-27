import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { StarRating } from '../components/StarRating';
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
  { label: 'Currently Reading', value: 'CURRENTLY_READING' },
  { label: 'Mark as Read', value: 'READ' },
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
    return {
      book,
      reviews,
      myRating,
      inLibrary: !!entry,
      status: entry?.status ?? null
    };
  }, [id]);

  // Local, mutable copies seeded from the loaded bundle so mutations reflect
  // immediately without a full refetch.
  const [reviews, setReviews] = useState<Review[]>([]);
  const [myRating, setMyRating] = useState<Rating | null>(null);
  const [status, setStatus] = useState<ReadingStatus | null>(null);
  const [inLibrary, setInLibrary] = useState(false);
  const [reviewText, setReviewText] = useState('');
  const [actionError, setActionError] = useState<string | null>(null);
  const [savingReview, setSavingReview] = useState(false);

  useEffect(() => {
    if (bundleState.data) {
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
      // The book may already be in the library (e.g. added in another tab):
      // fall back to a status update rather than failing on the duplicate.
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
      <AsyncSection state={bundleState} loadingLabel="Loading book…">
        {({ book }) => (
          <>
            <div className="book-details-header">
              {book.coverImageUrl && (
                <img src={book.coverImageUrl} alt={book.title} className="book-details-cover" />
              )}
              <div>
                <h1>{book.title}</h1>
                <p className="subtle">{book.authors.map((a) => a.name).join(', ')}</p>
                <p>{book.genres.map((g) => g.name).join(' · ')}</p>
                {book.averageRating != null && (
                  <p>
                    Average rating: {book.averageRating.toFixed(1)} ({book.ratingCount} ratings)
                  </p>
                )}
                {book.description && <p>{book.description}</p>}

                <div className="library-controls">
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

                <div>
                  <p id="your-rating-label">Your rating:</p>
                  <StarRating value={myRating?.stars || 0} onChange={handleRate} />
                </div>

                {actionError && (
                  <p className="form-error" role="alert">
                    {actionError}
                  </p>
                )}
              </div>
            </div>

            <section className="reviews-section">
              <h2>Reviews</h2>
              <form onSubmit={handleSubmitReview} className="review-form">
                <label htmlFor="review-text" className="visually-hidden">
                  Write a review
                </label>
                <textarea
                  id="review-text"
                  placeholder="Write a review…"
                  value={reviewText}
                  onChange={(e) => setReviewText(e.target.value)}
                  maxLength={5000}
                />
                <button type="submit" disabled={savingReview || !reviewText.trim()}>
                  {savingReview ? 'Posting…' : 'Post review'}
                </button>
              </form>
              {reviews.length > 0 ? (
                reviews.map((review) => (
                  <div key={review.id} className="review">
                    <strong>{review.displayName}</strong>
                    <p>{review.content}</p>
                  </div>
                ))
              ) : (
                <p className="subtle">No reviews yet. Be the first to share your thoughts.</p>
              )}
            </section>
          </>
        )}
      </AsyncSection>
    </div>
  );
}
