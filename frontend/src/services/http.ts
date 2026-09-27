import { AxiosError } from 'axios';
import { apiClient } from '../api/client';

/**
 * The single axios instance every service builds on. Re-exported here so
 * services import their transport from the service layer rather than reaching
 * into `../api/client` directly, keeping the HTTP concern in one place.
 */
export { apiClient };

/**
 * Normalizes any thrown value from an axios call into a human-readable message.
 * The backend's GlobalExceptionHandler returns a structured `ApiError` with a
 * `message` field, so we surface that when present and fall back to a friendly
 * default otherwise (network error, unexpected shape, non-axios throw).
 */
export function getApiErrorMessage(
  err: unknown,
  fallback = 'Something went wrong. Please try again.'
): string {
  const axiosError = err as AxiosError<{ message?: string }>;
  const serverMessage = axiosError?.response?.data?.message;
  if (typeof serverMessage === 'string' && serverMessage.trim().length > 0) {
    return serverMessage;
  }
  if (axiosError?.code === 'ERR_NETWORK') {
    return 'Cannot reach the server. Check your connection and try again.';
  }
  return fallback;
}

/** True when the error is an axios error carrying the given HTTP status. */
export function hasStatus(err: unknown, status: number): boolean {
  return (err as AxiosError)?.response?.status === status;
}
