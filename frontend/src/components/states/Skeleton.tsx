import React from 'react';

/** A single shimmering block. */
export function Skeleton({ className }: { className?: string }) {
  return <div className={['skeleton', className].filter(Boolean).join(' ')} aria-hidden="true" />;
}

/** A book-cover-shaped placeholder with two title lines. */
export function BookCardSkeleton() {
  return (
    <div aria-hidden="true">
      <div className="skeleton skeleton-cover" />
      <div className="skeleton skeleton-line" />
      <div className="skeleton skeleton-line short" />
    </div>
  );
}

/** A grid of book-cover skeletons, used instead of a bare spinner on shelves. */
export function BookGridSkeleton({ count = 6 }: { count?: number }) {
  return (
    <div className="skeleton-grid" role="status" aria-label="Loading books">
      {Array.from({ length: count }).map((_, i) => (
        <BookCardSkeleton key={i} />
      ))}
    </div>
  );
}
