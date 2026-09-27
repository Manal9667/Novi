import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { getApiErrorMessage, recommendationService } from '../services';
import type { FeedbackType, RecommendationResponse } from '../types';

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
    <div className="recommendation-card">
      <Link to={`/books/${book.id}`} className="rec-cover">
        {book.coverImageUrl ? (
          <img src={book.coverImageUrl} alt={book.title} />
        ) : (
          <div className="book-cover-placeholder">{book.title[0]}</div>
        )}
      </Link>
      <div className="rec-body">
        <div className="rec-header">
          <Link to={`/books/${book.id}`}>
            <h3>{book.title}</h3>
          </Link>
          <span className="match-badge">{recommendation.matchPercent}% Match</span>
        </div>
        <p className="subtle">{book.authorNames.join(', ')}</p>

        <div className="rec-reasons">
          <strong>Why:</strong>
          <ul>
            {recommendation.reasons.map((reason, i) => (
              <li key={i}>{reason}</li>
            ))}
          </ul>
          {recommendation.potentialDownside && (
            <p className="rec-downside">
              <strong>Potential downside:</strong> {recommendation.potentialDownside}
            </p>
          )}
        </div>

        {feedbackGiven ? (
          <p className="subtle">Thanks for the feedback!</p>
        ) : (
          <div className="rec-feedback">
            <button onClick={() => sendFeedback('INTERESTED')} disabled={pending}>
              👍 Interested
            </button>
            <button onClick={() => setShowReasons(!showReasons)} disabled={pending}>
              👎 Not for me
            </button>
            <button onClick={() => sendFeedback('ADDED_TO_WANT_TO_READ')} disabled={pending}>
              📚 Add to Want to Read
            </button>
          </div>
        )}

        {showReasons && !feedbackGiven && (
          <div className="rec-negative-reasons">
            {NEGATIVE_REASONS.map((reason) => (
              <button key={reason} onClick={() => sendFeedback('NOT_FOR_ME', reason)} disabled={pending}>
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
    </div>
  );
}
