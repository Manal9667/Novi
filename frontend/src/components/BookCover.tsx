import React, { useEffect, useState } from 'react';

interface BookCoverProps {
  /** Remote cover URL. May be null/undefined, or point at an image that fails to load. */
  src?: string | null;
  /** Book title; drives the alt text and the single-letter fallback. */
  title: string;
  /** Optional extra class on the wrapper. */
  className?: string;
}

/**
 * A book cover rendered as a physical object (spine shading + soft shadow, see
 * CSS). Falls back to a title-initial "spine" when there is no URL *and* when a
 * present URL fails to load, so a broken remote image never shows the browser's
 * broken-image icon or distorts the layout.
 */
export function BookCover({ src, title, className }: BookCoverProps) {
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    setFailed(false);
  }, [src]);

  const letter = title?.trim().charAt(0).toUpperCase() || '?';
  const wrapperClass = ['book-cover', className].filter(Boolean).join(' ');

  return (
    <div className={wrapperClass}>
      {!src || failed ? (
        <div className="book-cover-fallback" aria-hidden="true">
          {letter}
        </div>
      ) : (
        <img
          className="book-cover-img"
          src={src}
          alt={title}
          loading="lazy"
          onError={() => setFailed(true)}
        />
      )}
    </div>
  );
}
