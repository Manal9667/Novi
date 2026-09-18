import React from 'react';

interface StarRatingProps {
  value: number;
  onChange?: (stars: number) => void;
  readOnly?: boolean;
}

export function StarRating({ value, onChange, readOnly }: StarRatingProps) {
  const stars = [1, 2, 3, 4, 5];
  return (
    <div className="star-rating">
      {stars.map((s) => (
        <span
          key={s}
          className={`star ${s <= value ? 'filled' : ''} ${readOnly ? '' : 'interactive'}`}
          onClick={() => !readOnly && onChange?.(s)}
        >
          ★
        </span>
      ))}
    </div>
  );
}
