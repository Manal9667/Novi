import { useCallback, useEffect, useRef, useState } from 'react';
import { getApiErrorMessage } from '../services';

export type AsyncStatus = 'loading' | 'success' | 'error';

export interface AsyncState<T> {
  data: T | null;
  status: AsyncStatus;
  error: string | null;
  /** Re-run the async function (e.g. from a "Try again" button). */
  reload: () => void;
  /** Imperatively replace the loaded data (e.g. after an optimistic mutation). */
  setData: (updater: T | ((prev: T | null) => T)) => void;
}

/**
 * Runs an async loader on mount (and whenever `deps` change) and exposes a
 * single loading/success/error state machine, so every data page handles the
 * three states consistently instead of ad-hoc `useState` plumbing.
 *
 * Results from a stale run (deps changed, or the component unmounted before the
 * request resolved) are ignored, which prevents both React state-update
 * warnings and the classic race where an older request overwrites a newer one.
 */
export function useAsync<T>(
  loader: () => Promise<T>,
  deps: React.DependencyList
): AsyncState<T> {
  const [data, setDataState] = useState<T | null>(null);
  const [status, setStatus] = useState<AsyncStatus>('loading');
  const [error, setError] = useState<string | null>(null);
  const [nonce, setNonce] = useState(0);

  // Track the latest run so resolved stale runs can be discarded.
  const runIdRef = useRef(0);

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const stableLoader = useCallback(loader, deps);

  useEffect(() => {
    const runId = ++runIdRef.current;
    setStatus('loading');
    setError(null);

    stableLoader()
      .then((result) => {
        if (runId !== runIdRef.current) return;
        setDataState(result);
        setStatus('success');
      })
      .catch((err) => {
        if (runId !== runIdRef.current) return;
        setError(getApiErrorMessage(err));
        setStatus('error');
      });

    return () => {
      // Invalidate this run; a late resolution will be ignored.
      runIdRef.current++;
    };
  }, [stableLoader, nonce]);

  const reload = useCallback(() => setNonce((n) => n + 1), []);

  const setData = useCallback((updater: T | ((prev: T | null) => T)) => {
    setDataState((prev) =>
      typeof updater === 'function' ? (updater as (p: T | null) => T)(prev) : updater
    );
  }, []);

  return { data, status, error, reload, setData };
}
