import { describe, expect, it } from 'vitest';
import { AxiosError } from 'axios';
import { getApiErrorMessage, hasStatus } from './http';

function axiosErrorWith(status: number, message?: string): AxiosError {
  const err = new AxiosError('request failed');
  // Minimal response shape matching what the backend's ApiError returns.
  err.response = {
    status,
    data: message ? { message } : {},
    statusText: '',
    headers: {},
    config: {} as never
  };
  return err;
}

describe('getApiErrorMessage', () => {
  it('returns the server-provided message when present', () => {
    const err = axiosErrorWith(400, 'Username is already taken');
    expect(getApiErrorMessage(err)).toBe('Username is already taken');
  });

  it('falls back to the supplied default when there is no server message', () => {
    const err = axiosErrorWith(500);
    expect(getApiErrorMessage(err, 'Custom fallback')).toBe('Custom fallback');
  });

  it('gives a connection-specific message on network errors', () => {
    const err = new AxiosError('Network Error', 'ERR_NETWORK');
    expect(getApiErrorMessage(err)).toMatch(/cannot reach the server/i);
  });

  it('handles non-axios throws with the generic default', () => {
    expect(getApiErrorMessage(new Error('boom'))).toMatch(/something went wrong/i);
  });
});

describe('hasStatus', () => {
  it('matches the axios response status', () => {
    expect(hasStatus(axiosErrorWith(409), 409)).toBe(true);
    expect(hasStatus(axiosErrorWith(404), 409)).toBe(false);
  });

  it('is false for non-axios errors', () => {
    expect(hasStatus(new Error('nope'), 404)).toBe(false);
  });
});
