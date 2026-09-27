import { describe, expect, it, vi } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import { AxiosError } from 'axios';
import { useAsync } from './useAsync';

describe('useAsync', () => {
  it('starts in loading and resolves to success with data', async () => {
    const { result } = renderHook(() => useAsync(() => Promise.resolve(['a', 'b']), []));

    expect(result.current.status).toBe('loading');

    await waitFor(() => expect(result.current.status).toBe('success'));
    expect(result.current.data).toEqual(['a', 'b']);
    expect(result.current.error).toBeNull();
  });

  it('captures a friendly error message on failure', async () => {
    const err = new AxiosError('failed');
    err.response = {
      status: 500,
      data: { message: 'Server exploded' },
      statusText: '',
      headers: {},
      config: {} as never
    };
    const { result } = renderHook(() => useAsync(() => Promise.reject(err), []));

    await waitFor(() => expect(result.current.status).toBe('error'));
    expect(result.current.error).toBe('Server exploded');
    expect(result.current.data).toBeNull();
  });

  it('re-runs the loader when reload is called', async () => {
    const loader = vi.fn().mockResolvedValueOnce('first').mockResolvedValueOnce('second');
    const { result } = renderHook(() => useAsync(loader, []));

    await waitFor(() => expect(result.current.status).toBe('success'));
    expect(result.current.data).toBe('first');

    act(() => result.current.reload());

    await waitFor(() => expect(result.current.data).toBe('second'));
    expect(loader).toHaveBeenCalledTimes(2);
  });
});
