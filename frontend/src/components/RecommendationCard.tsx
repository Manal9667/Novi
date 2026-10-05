import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { getApiErrorMessage, recommendationService } from '../services';
import type { FeedbackType, RecommendationResponse } from '../types';
import { BookCover } from './BookCover';

const NEGATIVE_REASONS = [
  'Too slow',
  'Too much romance',
  'Already read it',
  "Don't like the genre",
  'Not interested'
];

export function RecommendationCard({ recommendation }: { recommendation: RecommendationResponse }) {
  const [feedbackGiven, setFeedbackGiven] = useState<FeedbackType | null>(null);
  const [showReasons, setShowReasons] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  async function sendFeedback(feedbackType: FeedbackType, reason?: string) {
    setPending(true);
    setError(null);
    try {
      await recommendationService.sendFeedback(recommendation.id, feedbackType, reason);
      setFeedbackGiven(feedbackType);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Could not record your feedback.'));
    } finally {
      setPending(false);
    }
  }

  const { book } = recommendation;

  return (
    <article className="rec-card">
      <Link to={`/books/${book.id}`} className="rec-cover" aria-label={book.title}>
        <BookCover src={book.coverImageUrl} title={book.title} />
        <span className="match-ribbon" aria-label={`${recommendation.matchPercent}% match`}>
          {recommendation.matchPercent}%
        </span>
      </Link>

      <div className="rec-body">
        <div>
          <Link to={`/books/${book.id}`} className="rec-title-link">
            <h3>{book.title}</h3>
          </Link>
          <p className="subtle rec-authors">{book.authorNames.join(', ')}</p>
        </div>

        {recommendation.reasons.length > 0 && (
          <div className="rec-note">
            <span className="rec-note-label hand">Why you'll love it</span>
            <ul>
              {recommendation.reasons.map((reason, i) => (
                <li key={i}>{reason}</li>
              ))}
            </ul>
          </div>
        )}

        {recommendation.potentialDownside && (
          <p className="rec-downside">
            <strong>One heads-up:</strong> {recommendation.potentialDownside}
          </p>
        )}

        {feedbackGiven ? (
          <p className="subtle rec-thanks">Thanks — noted for next time. 📖</p>
        ) : (
          <div className="rec-feedback">
            <button className="chip btn-sm" onClick={() => sendFeedback('INTERESTED')} disabled={pending}>
              👍 Interested
            </button>
            <button className="chip btn-sm" onClick={() => setShowReasons((s) => !s)} disabled={pending} aria-expanded={showReasons}>
              👎 Not for me
            </button>
            <button className="chip btn-sm" onClick={() => sendFeedback('ADDED_TO_WANT_TO_READ')} disabled={pending}>
              📚 Want to read
            </button>
          </div>
        )}

        {showReasons && !feedbackGiven && (
          <div className="rec-negative-reasons">
            {NEGATIVE_REASONS.map((reason) => (
              <button key={reason} className="chip chip-sm" onClick={() => sendFeedback('NOT_FOR_ME', reason)} disabled={pending}>
                {reason}
              </button>
            ))}
          </div>
        )}

        {error && (
          <p className="form-error" role="alert">
            {error}
          </p>
        )}
      </div>
    </article>
  );
}
