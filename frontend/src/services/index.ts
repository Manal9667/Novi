/**
 * Central barrel for the API service layer. Pages and components import domain
 * services from here rather than constructing raw HTTP calls, so endpoint
 * paths, params, and response typing live in one place per domain.
 */
export { apiClient, getApiErrorMessage, hasStatus } from './http';
export { authService, type RegisterPayload } from './authService';
export { bookService } from './bookService';
export { libraryService, type LibraryQuery } from './libraryService';
export { recommendationService } from './recommendationService';
