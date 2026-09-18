import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { apiClient } from '../api/client';
import type { RecommendationResponse } from '../types';

const NEGATIVE_REASONS = ['Too slow', 'Too much romance', 'Already read it', "Don't like the genre", 'Not interested'];

export function RecommendationCard({ recommendation }: { recommendation: RecommendationResponse }) {
  const [feedbackGiven, setFeedbackGiven] = useState<string | null>(null);
  const [showReasons, setShowReasons] = useState(false);

  async function sendFeedback(feedbackType: string, reason?: string) {
    await apiClient.post(`/recommendations/${recommendation.id}/feedback`, { feedbackType, reason });
    setFeedbackGiven(feedbackType);
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
          <Link to={`/books/${book.id}`}><h3>{book.title}</h3></Link>
          <span className="match-badge">{recommendation.matchPercent}% Match</span>
        </div>
        <p className="subtle">{book.authorNames.join(', ')}</p>

        <div className="rec-reasons">
          <strong>Why:</strong>
          <ul>
            {recommendation.reasons.map((reason, i) => <li key={i}>{reason}</li>)}
          </ul>
          {recommendation.potentialDownside && (
            <p className="rec-downside"><strong>Potential downside:</strong> {recommendation.potentialDownside}</p>
          )}
        </div>

        {feedbackGiven ? (
          <p className="subtle">Thanks for the feedback!</p>
        ) : (
          <div className="rec-feedback">
            <button onClick={() => sendFeedback('INTERESTED')}>👍 Interested</button>
            <button onClick={() => setShowReasons(!showReasons)}>👎 Not for me</button>
            <button onClick={() => sendFeedback('ADDED_TO_WANT_TO_READ')}>📚 Add to Want to Read</button>
          </div>
        )}

        {showReasons && !feedbackGiven && (
          <div className="rec-negative-reasons">
            {NEGATIVE_REASONS.map((reason) => (
              <button key={reason} onClick={() => sendFeedback('NOT_FOR_ME', reason)}>{reason}</button>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
