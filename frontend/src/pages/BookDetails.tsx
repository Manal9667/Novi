import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { apiClient } from '../api/client';
import { StarRating } from '../components/StarRating';
import type { BookDetail, Rating, Review, ReadingStatus } from '../types';

export default function BookDetails() {
  const { id } = useParams();
  const [book, setBook] = useState<BookDetail | null>(null);
  const [myRating, setMyRating] = useState<Rating | null>(null);
  const [reviews, setReviews] = useState<Review[]>([]);
  const [reviewText, setReviewText] = useState('');
  const [status, setStatus] = useState<ReadingStatus | null>(null);
  const [inLibrary, setInLibrary] = useState(false);

  useEffect(() => {
    if (!id) return;
    apiClient.get<BookDetail>(`/books/${id}`).then((res) => setBook(res.data));
    apiClient.get<Review[]>(`/books/${id}/reviews`).then((res) => setReviews(res.data));
    apiClient.get<Rating>(`/books/${id}/ratings/mine`).then((res) => {
      if (res.status === 200) setMyRating(res.data);
    }).catch(() => {});
  }, [id]);

  async function handleAddToLibrary(newStatus: ReadingStatus) {
    if (!id) return;
    if (!inLibrary) {
      await apiClient.post('/library/books', { bookId: Number(id), status: newStatus });
      setInLibrary(true);
    } else {
      await apiClient.patch(`/library/books/${id}/status`, { status: newStatus });
    }
    setStatus(newStatus);
  }

  async function handleRate(stars: number) {
    if (!id) return;
    const { data } = await apiClient.post<Rating>(`/books/${id}/ratings`, { stars });
    setMyRating(data);
  }

  async function handleSubmitReview(e: React.FormEvent) {
    e.preventDefault();
    if (!id || !reviewText.trim()) return;
    const { data } = await apiClient.post<Review>(`/books/${id}/reviews`, { content: reviewText });
    setReviews([data, ...reviews]);
    setReviewText('');
  }

  if (!book) return <div className="page">Loading...</div>;

  return (
    <div className="page book-details">
      <div className="book-details-header">
        {book.coverImageUrl && <img src={book.coverImageUrl} alt={book.title} className="book-details-cover" />}
        <div>
          <h1>{book.title}</h1>
          <p className="subtle">{book.authors.map((a) => a.name).join(', ')}</p>
          <p>{book.genres.map((g) => g.name).join(' · ')}</p>
          {book.averageRating != null && (
            <p>Average rating: {book.averageRating.toFixed(1)} ({book.ratingCount} ratings)</p>
          )}
          <p>{book.description}</p>

          <div className="library-controls">
            <button onClick={() => handleAddToLibrary('WANT_TO_READ')}>Want to Read</button>
            <button onClick={() => handleAddToLibrary('CURRENTLY_READING')}>Currently Reading</button>
            <button onClick={() => handleAddToLibrary('READ')}>Mark as Read</button>
            <button onClick={() => handleAddToLibrary('DNF')}>DNF</button>
          </div>

          <div>
            <p>Your rating:</p>
            <StarRating value={myRating?.stars || 0} onChange={handleRate} />
          </div>
        </div>
      </div>

      <section className="reviews-section">
        <h2>Reviews</h2>
        <form onSubmit={handleSubmitReview} className="review-form">
          <textarea
            placeholder="Write a review..."
            value={reviewText}
            onChange={(e) => setReviewText(e.target.value)}
            maxLength={5000}
          />
          <button type="submit">Post review</button>
        </form>
        {reviews.map((review) => (
          <div key={review.id} className="review">
            <strong>{review.displayName}</strong>
            <p>{review.content}</p>
          </div>
        ))}
      </section>
    </div>
  );
}
