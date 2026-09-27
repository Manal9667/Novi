import React from 'react';

interface ErrorStateProps {
  message: string;
  onRetry?: () => void;
}

/** Consistent inline error panel with an optional retry action. */
export function ErrorState({ message, onRetry }: ErrorStateProps) {
  return (
    <div className="error-state" role="alert">
      <p className="form-error">{message}</p>
      {onRetry && (
        <button type="button" className="secondary" onClick={onRetry}>
          Try again
        </button>
      )}
    </div>
  );
}
