import React from 'react';
import { Link } from 'react-router-dom';
import type { Recommendation } from '../types';
import { BookCover } from './BookCover';
import { SaveButton } from './SaveButton';

/** How Novi presents one ranked recommendation: cover, metadata, relevance, grounded reason, and actions. */
export function RecommendationCard({ rec }: { rec: Recommendation }) {
  const detailTo = {
    pathname: `/books/${rec.bookId}`,
    // Carry the explanation so the detail page can show "Why Novi recommended it".
  };
  const detailState = { explanation: rec.explanation, relevanceScore: rec.relevanceScore };

  return (
    <article className="rec-card">
      <Link to={detailTo} state={detailState} className="rec-cover" aria-label={rec.title}>
        <BookCover src={rec.coverImageUrl} title={rec.title} />
        <span className="match-ribbon" title="How closely this matches your request">
          {rec.relevanceScore}% match
        </span>
      </Link>

      <div className="rec-body">
        <div className="rec-head">
          <Link to={detailTo} state={detailState} className="rec-title-link">
            <h3>{rec.title}</h3>
          </Link>
          <p className="subtle rec-authors">{rec.authors.join(', ')}</p>
        </div>

        {rec.genres.length > 0 && (
          <div className="tag-row">
            {rec.genres.slice(0, 3).map((g) => (
              <span key={g} className="chip chip-static chip-sm">{g}</span>
            ))}
          </div>
        )}

        {rec.explanation && (
          <div className="rec-note">
            <span className="rec-note-label hand">Why Novi picked this</span>
            <p>{rec.explanation}</p>
          </div>
        )}

        <div className="rec-actions">
          <SaveButton bookId={rec.bookId} />
          <Link to={detailTo} state={detailState} className="link-button">View details →</Link>
        </div>
      </div>
    </article>
  );
}
