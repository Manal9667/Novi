import React from 'react';
import { Link } from 'react-router-dom';
import { BookCard } from '../components/BookCard';
import { RecommendationCard } from '../components/RecommendationCard';
import { AsyncSection } from '../components/states/AsyncSection';
import { EmptyState } from '../components/states/EmptyState';
import { useAuth } from '../context/AuthContext';
import { useAsync } from '../hooks/useAsync';
import { libraryService, recommendationService } from '../services';
import type { RecommendationResponse, UserBook } from '../types';

interface HomeData {
  currentlyReading: UserBook[];
  wantToRead: UserBook[];
  recentlyFinished: UserBook[];
}

export default function Home() {
  const { user } = useAuth();

  const library = useAsync<HomeData>(async () => {
    const [reading, want, read] = await Promise.all([
      libraryService.getLibrary({ status: 'CURRENTLY_READING' }),
      libraryService.getLibrary({ status: 'WANT_TO_READ' }),
      libraryService.getLibrary({ status: 'READ' })
    ]);
    return {
      currentlyReading: reading.content,
      wantToRead: want.content,
      recentlyFinished: read.content.slice(0, 6)
    };
  }, []);

  // Recommendations are best-effort: the AI layer may be unconfigured or the
  // reader may be too new for picks. A failure here must never block the home page.
  const recs = useAsync<RecommendationResponse[]>(
    () => recommendationService.getPersonalized().then((r) => r.slice(0, 3)).catch(() => []),
    []
  );

  return (
    <div className="page">
      <h1>Welcome back{user ? `, ${user.displayName}` : ''}</h1>

      {recs.status === 'success' && recs.data && recs.data.length > 0 && (
        <section>
          <div className="section-header">
            <h2>Recommended for You</h2>
            <Link to="/recommendations">See all →</Link>
          </div>
          <div className="recommendation-list">
            {recs.data.map((rec) => (
              <RecommendationCard key={rec.id} recommendation={rec} />
            ))}
          </div>
        </section>
      )}

      <AsyncSection state={library} loadingLabel="Loading your library…">
        {(data) => (
          <>
            <section>
              <h2>Currently Reading</h2>
              {data.currentlyReading.length > 0 ? (
                <div className="book-grid">
                  {data.currentlyReading.map((ub) => (
                    <BookCard key={ub.id} book={ub.book} />
                  ))}
                </div>
              ) : (
                <EmptyState
                  message={
                    <>
                      Nothing here yet — <Link to="/search">find a book</Link> to start.
                    </>
                  }
                />
              )}
            </section>

            <section>
              <h2>Recently Finished</h2>
              {data.recentlyFinished.length > 0 ? (
                <div className="book-grid">
                  {data.recentlyFinished.map((ub) => (
                    <BookCard key={ub.id} book={ub.book} />
                  ))}
                </div>
              ) : (
                <EmptyState message="Books you mark as read will show up here." />
              )}
            </section>

            <section>
              <h2>Want to Read</h2>
              {data.wantToRead.length > 0 ? (
                <div className="book-grid">
                  {data.wantToRead.map((ub) => (
                    <BookCard key={ub.id} book={ub.book} />
                  ))}
                </div>
              ) : (
                <EmptyState message="Your reading wishlist is empty for now." />
              )}
            </section>
          </>
        )}
      </AsyncSection>
    </div>
  );
}
