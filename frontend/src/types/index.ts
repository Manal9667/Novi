export type ReadingStatus = 'WANT_TO_READ' | 'CURRENTLY_READING' | 'READ' | 'DNF';

export interface AuthResponse {
  token: string;
  userId: number;
  username: string;
  displayName: string;
}

export interface BookSummary {
  id: number;
  title: string;
  coverImageUrl: string | null;
  authorNames: string[];
}

export interface Author {
  id: number;
  name: string;
}

export interface Genre {
  id: number;
  name: string;
}

export interface BookDetail {
  id: number;
  title: string;
  description: string | null;
  coverImageUrl: string | null;
  isbn: string | null;
  publicationDate: string | null;
  authors: Author[];
  genres: Genre[];
  averageRating: number | null;
  ratingCount: number;
}

export interface UserBook {
  id: number;
  book: BookSummary;
  status: ReadingStatus;
  startedAt: string | null;
  finishedAt: string | null;
  createdAt: string;
}

export interface Rating {
  id: number;
  bookId: number;
  stars: number;
  createdAt: string;
  updatedAt: string;
}

export interface Review {
  id: number;
  bookId: number;
  userId: number;
  username: string;
  displayName: string;
  content: string;
  createdAt: string;
  updatedAt: string;
}

export interface Shelf {
  id: number;
  name: string;
  createdAt: string;
  books: BookSummary[];
}

export interface ReadingHistoryEntry {
  id: number;
  bookId: number;
  bookTitle: string;
  eventType: string;
  occurredAt: string;
}

export interface UserProfile {
  id: number;
  username: string;
  displayName: string;
  bio: string | null;
  createdAt: string;
  booksRead: number;
  currentlyReading: number;
  wantToRead: number;
  reviewCount: number;
}

export interface RecommendationResponse {
  id: number;
  book: BookSummary;
  matchPercent: number;
  reasons: string[];
  potentialDownside: string | null;
  createdAt: string;
}

export type FeedbackType = 'INTERESTED' | 'NOT_FOR_ME' | 'ADDED_TO_WANT_TO_READ';

export interface AffinityEntry {
  name: string;
  score: number;
}

export interface ReadingPersonalityResponse {
  genreAffinities: AffinityEntry[];
  themeAffinities: AffinityEntry[];
  summary: string;
}


// Phase 3: computer-vision book scanner

export type ScanType = 'SINGLE_BOOK' | 'SHELF';
export type ScanSessionStatus = 'AWAITING_CONFIRMATION' | 'COMPLETED';
export type ScanCandidateStatus = 'PENDING' | 'CONFIRMED' | 'SKIPPED';

export interface ScanCandidate {
  id: number;
  detectedTitle: string;
  detectedAuthor: string | null;
  confidence: number;
  matchedBook: BookSummary | null;
  status: ScanCandidateStatus;
}

export interface ScanResult {
  sessionId: number;
  type: ScanType;
  detectedCount: number;
  status: ScanSessionStatus;
  candidates: ScanCandidate[];
}

export interface ConfirmScanResponse {
  addedCount: number;
  skippedCount: number;
  alreadyInLibraryCount: number;
  added: UserBook[];
}
