import React from 'react';
import { Link } from 'react-router-dom';
import { EmptyState } from '../components/states/EmptyState';

/** Catch-all for unknown URLs so a mistyped or stale link never shows a blank page. */
export default function NotFound() {
  return (
    <div className="page">
      <EmptyState
        title="Page not found"
        message={
          <>
            We couldn't find the page you were looking for.{' '}
            <Link to="/">Go back home</Link>.
          </>
        }
      />
    </div>
  );
}
