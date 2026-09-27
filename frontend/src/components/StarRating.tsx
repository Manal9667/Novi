import React from 'react';

interface StarRatingProps {
  value: number;
  onChange?: (stars: number) => void;
  readOnly?: boolean;
}

const STARS = [1, 2, 3, 4, 5];

/**
 * Accessible star rating. When interactive it renders real <button> elements,
 * so stars are keyboard-focusable and activatable with Enter/Space, and each
 * carries an aria-label. When read-only it renders non-interactive text marked
 * aria-hidden with a single screen-reader summary.
 */
export function StarRating({ value, onChange, readOnly }: StarRatingProps) {
  if (readOnly || !onChange) {
    return (
      <div className="star-rating" role="img" aria-label={`Rated ${value} out of 5 stars`}>
        {STARS.map((s) => (
          <span key={s} className={`star ${s <= value ? 'filled' : ''}`} aria-hidden="true">
            ★
          </span>
        ))}
      </div>
    );
  }

  return (
    <div className="star-rating" role="group" aria-label="Your rating">
      {STARS.map((s) => (
        <button
          key={s}
          type="button"
          className={`star interactive ${s <= value ? 'filled' : ''}`}
          aria-label={`Rate ${s} star${s === 1 ? '' : 's'}`}
          aria-pressed={s <= value}
          onClick={() => onChange(s)}
        >
          ★
        </button>
      ))}
    </div>
  );
}
