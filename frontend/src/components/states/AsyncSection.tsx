import React from 'react';
import type { AsyncState } from '../../hooks/useAsync';
import { Spinner } from './Spinner';
import { ErrorState } from './ErrorState';

interface AsyncSectionProps<T> {
  state: AsyncState<T>;
  loadingLabel?: string;
  children: (data: T) => React.ReactNode;
}

/**
 * Renders the standard loading/error states for a `useAsync` result and hands
 * the loaded data to `children` on success. Keeps every data page's three-state
 * handling identical without repeating the boilerplate.
 */
export function AsyncSection<T>({ state, loadingLabel, children }: AsyncSectionProps<T>) {
  if (state.status === 'loading') {
    return <Spinner label={loadingLabel} />;
  }
  if (state.status === 'error') {
    return <ErrorState message={state.error ?? 'Something went wrong.'} onRetry={state.reload} />;
  }
  if (state.data == null) {
    return null;
  }
  return <>{children(state.data)}</>;
}
