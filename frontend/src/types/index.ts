export type ReadingStatus = 'SAVED' | 'WANT_TO_READ' | 'CURRENTLY_READING' | 'READ' | 'DNF';

/** Pagination envelope returned by collection endpoints (see backend PageResponse). */
export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface AuthResponse {
  token: string;
  userId: number;
  username: string;
  displayName: string;
}

export interface Author {
  id: number;
  name: string;
}

export interface Genre {
  id: number;
  name: string;
}

/** Compact book shape used in the saved-books list. */
export interface BookSummary {
  id: number;
  title: string;
  coverImageUrl: string | null;
  authorNames: string[];
}

/** Full catalog detail from GET /api/books/{id}. */
export interface BookDetail {
  id: number;
  title: string;
  description: string | null;
  coverImageUrl: string | null;
  isbn: string | null;
  publicationDate: string | null;
  authors: Author[];
  genres: Genre[];
  themes: string[];
}

/** A user's saved book entry. */
export interface UserBook {
  id: number;
  book: BookSummary;
  status: ReadingStatus;
  startedAt: string | null;
  finishedAt: string | null;
  createdAt: string;
}

/** A ranked, explained recommendation from POST /api/recommendations/ask. */
export interface Recommendation {
  bookId: number;
  title: string;
  authors: string[];
  coverImageUrl: string | null;
  description: string | null;
  genres: string[];
  themes: string[];
  relevanceScore: number;
  explanation: string;
}
