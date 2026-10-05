import React from 'react';

interface EmptyStateProps {
  title?: string;
  message: React.ReactNode;
  /** Optional spot illustration shown above the text. */
  illustration?: React.ReactNode;
  /** Optional call-to-action rendered below the message. */
  action?: React.ReactNode;
}

/** Consistent, warm placeholder shown when a successful load returns no items. */
export function EmptyState({ title, message, illustration, action }: EmptyStateProps) {
  return (
    <div className="empty-state">
      {illustration}
      {title && <h3>{title}</h3>}
      <p className="subtle">{message}</p>
      {action}
    </div>
  );
}
