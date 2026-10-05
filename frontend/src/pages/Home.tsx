import React from 'react';
import { Link } from 'react-router-dom';
import { Shelf } from '../components/Shelf';
import { RecommendationCard } from '../components/RecommendationCard';
import { AsyncSection } from '../components/states/AsyncSection';
import { EmptyState } from '../components/states/EmptyState';
import { BookGridSkeleton } from '../components/states/Skeleton';
import { NookIllustration, CompassIcon, SparkleIcon } from '../components/icons';
import { useAuth } from '../context/AuthContext';
import { useAsync } from '../hooks/useAsync';
import { libraryService, recommendationService } from '../services';
import type { RecommendationResponse, UserBook } from '../types';

interface HomeData {
  currentlyReading: UserBook[];
  wantToRead: UserBook[];
  recentlyFinished: UserBook[];
}

function greeting() {
  const h = new Date().getHours();
  if (h < 5) return 'Still up reading';
  if (h < 12) return 'Good morning';
  if (h < 17) return 'Good afternoon';
  if (h < 22) return 'Good evening';
  return 'Winding down';
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
      recentlyFinished: read.content.slice(0, 10)
    };
  }, []);

  const recs = useAsync<RecommendationResponse[]>(
    () => recommendationService.getPersonalized().then((r) => r.slice(0, 3)).catch(() => []),
    []
  );

  return (
    <div className="page home">
      <header className="home-greeting">
        <h1>{greeting()}{user ? `, ${user.displayName}` : ''}.</h1>
        <p className="hand">What are we reading today?</p>
      </header>

      <div className="home-entrypoints">
        <Link to="/find" className="entry-card entry-find">
          <span className="entry-icon"><CompassIcon size={26} /></span>
          <span>
            <strong>Find your next read</strong>
            <em>Ask for anything — a mood, a vibe, a bestseller.</em>
          </span>
        </Link>
        <Link to="/recommendations" className="entry-card entry-foryou">
          <span className="entry-icon"><SparkleIcon size={26} /></span>
          <span>
            <strong>Recommended for you</strong>
            <em>Picked from the books you've loved.</em>
          </span>
        </Link>
        <Link to="/wrapped" className="entry-card entry-wrapped">
          <span className="entry-icon"><SparkleIcon size={26} /></span>
          <span>
            <strong>Novi Wrapped</strong>
            <em>Your reading year, wrapped up.</em>
          </span>
        </Link>
      </div>

      <AsyncSection state={library} loadingLabel="Opening your study…">
        {(data) => (
          <>
            <section className="nightstand">
              <div className="section-header">
                <h2>On your nightstand</h2>
                {data.currentlyReading.length > 0 && <Link to="/library">Your library →</Link>}
              </div>
              <Shelf
                books={data.currentlyReading.map((ub) => ub.book)}
                empty={
                  <EmptyState
                    illustration={<NookIllustration className="illo" />}
                    title="Your nightstand is empty"
                    message="Nothing on the go right now. Let's find something to curl up with."
                    action={<Link to="/find" className="button-link">Find a book</Link>}
                  />
                }
              />
            </section>

            {recs.status === 'success' && recs.data && recs.data.length > 0 && (
              <section>
                <div className="section-header">
                  <h2>A few you might love</h2>
                  <Link to="/recommendations">See all →</Link>
                </div>
                <div className="recommendation-list">
                  {recs.data.map((rec) => (
                    <RecommendationCard key={rec.id} recommendation={rec} />
                  ))}
                </div>
              </section>
            )}

            <section>
              <div className="section-header"><h2>Recently finished</h2></div>
              <Shelf
                books={data.recentlyFinished.map((ub) => ub.book)}
                empty={<EmptyState message="Books you mark as read will line up here like old friends." />}
              />
            </section>

            <section>
              <div className="section-header"><h2>Want to read</h2></div>
              <Shelf
                books={data.wantToRead.map((ub) => ub.book)}
                empty={<EmptyState message="Your to-be-read pile is empty — for now." />}
              />
            </section>
          </>
        )}
      </AsyncSection>
    </div>
  );
}
