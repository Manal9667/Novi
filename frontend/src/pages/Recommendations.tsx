import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { RecommendationCard } from '../components/RecommendationCard';
import { ErrorState } from '../components/states/ErrorState';
import { EmptyState } from '../components/states/EmptyState';
import { Spinner } from '../components/states/Spinner';
import { useAsync } from '../hooks/useAsync';
import { getApiErrorMessage, recommendationService } from '../services';
import type { AffinityEntry, ReadingPersonalityResponse, RecommendationResponse } from '../types';

function AffinityBars({ title, entries, empty }: { title: string; entries: AffinityEntry[]; empty: string }) {
  return (
    <div className="affinity-col">
      <h3>{title}</h3>
      {entries.length > 0 ? (
        <ul className="spine-bars">
          {entries.map((a) => (
            <li key={a.name}>
              <span className="spine" style={{ height: `${Math.max(18, Math.round(a.score * 100))}%` }} aria-hidden="true" />
              <span className="spine-label">{a.name}</span>
            </li>
          ))}
        </ul>
      ) : (
        <p className="subtle">{empty}</p>
      )}
    </div>
  );
}

export default function Recommendations() {
  const [recommendations, setRecommendations] = useState<RecommendationResponse[]>([]);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const personality = useAsync<ReadingPersonalityResponse>(
    () => recommendationService.getReadingPersonality(),
    []
  );

  async function loadRecommendations() {
    setLoading(true);
    setLoadError(null);
    try {
      setRecommendations(await recommendationService.getPersonalized());
    } catch (err) {
      setLoadError(getApiErrorMessage(err, 'Could not load your recommendations.'));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { void loadRecommendations(); }, []);

  return (
    <div className="page">
      <div className="section-header">
        <h1>Recommended for you</h1>
        <Link to="/find" className="button-link secondary btn-sm">Looking for something specific?</Link>
      </div>
      <p className="subtle measure">Chosen from the books, ratings and reviews in your library — the more you read and rate, the sharper these get.</p>

      {loading && <Spinner label="Pulling books off the shelf for you…" />}
      {!loading && loadError && <ErrorState message={loadError} onRetry={loadRecommendations} />}

      {!loading && !loadError && (
        recommendations.length > 0 ? (
          <div className="recommendation-list">
            {recommendations.map((rec) => (
              <RecommendationCard key={rec.id} recommendation={rec} />
            ))}
          </div>
        ) : (
          <EmptyState
            title="Not enough to go on yet"
            message={<>Add and rate a few books so Novi can learn your taste — or <Link to="/find">tell Novi what you're in the mood for</Link>.</>}
          />
        )
      )}

      {personality.status === 'success' && personality.data && (
        <section className="personality card card-pad">
          <h2>Your reading personality</h2>
          <p className="measure personality-summary">{personality.data.summary}</p>
          <div className="affinity-columns">
            <AffinityBars title="Favourite genres" entries={personality.data.genreAffinities} empty="Rate books to reveal your genres." />
            <AffinityBars title="Recurring themes" entries={personality.data.themeAffinities} empty="Themes appear as you read more." />
          </div>
        </section>
      )}
    </div>
  );
}
