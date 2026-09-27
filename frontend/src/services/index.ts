/**
 * Central barrel for the API service layer. Components and pages import domain
 * services from here (e.g. `import { libraryService } from '../services'`)
 * rather than constructing raw HTTP calls, so endpoint paths, params, and
 * response typing live in one place per domain.
 */
export { apiClient, getApiErrorMessage, hasStatus } from './http';
export { authService, type RegisterPayload } from './authService';
export { bookService } from './bookService';
export { libraryService, type LibraryQuery } from './libraryService';
export { ratingService } from './ratingService';
export { reviewService } from './reviewService';
export { recommendationService } from './recommendationService';
export { shelfService } from './shelfService';
export { readingHistoryService } from './readingHistoryService';
export { userService } from './userService';
export { scanService, type ScanDecision } from './scanService';
