import React from 'react';

interface EmptyStateProps {
  title?: string;
  message: React.ReactNode;
}

/** Consistent placeholder shown when a successful load returns no items. */
export function EmptyState({ title, message }: EmptyStateProps) {
  return (
    <div className="empty-state">
      {title && <h3>{title}</h3>}
      <p className="subtle">{message}</p>
    </div>
  );
}
