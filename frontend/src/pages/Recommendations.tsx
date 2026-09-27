import React, { useState } from 'react';
import { RecommendationCard } from '../components/RecommendationCard';
import { ErrorState } from '../components/states/ErrorState';
import { EmptyState } from '../components/states/EmptyState';
import { Spinner } from '../components/states/Spinner';
import { useAsync } from '../hooks/useAsync';
import { getApiErrorMessage, recommendationService } from '../services';
import type { ReadingPersonalityResponse, RecommendationResponse } from '../types';

export default function Recommendations() {
  const [recommendations, setRecommendations] = useState<RecommendationResponse[]>([]);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState('');
  const [asking, setAsking] = useState(false);
  const [askError, setAskError] = useState<string | null>(null);

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

  // Load once on mount.
  React.useEffect(() => {
    void loadRecommendations();
  }, []);

  async function handleAsk(e: React.FormEvent) {
    e.preventDefault();
    const trimmed = query.trim();
    if (!trimmed) return;
    setAsking(true);
    setAskError(null);
    try {
      setRecommendations(await recommendationService.ask(trimmed));
    } catch (err) {
      setAskError(getApiErrorMessage(err, 'Could not answer that request. Please try again.'));
    } finally {
      setAsking(false);
    }
  }

  return (
    <div className="page">
      <h1>Recommended for you</h1>

      {personality.status === 'success' && personality.data && (
        <section className="reading-personality">
          <h2>Your Reading Personality</h2>
          <p>{personality.data.summary}</p>
          <div className="affinity-columns">
            <div>
              <h3>Genres</h3>
              {personality.data.genreAffinities.map((a) => (
                <div key={a.name} className="affinity-bar">
                  <span>{a.name}</span>
                  <div className="bar-track">
                    <div className="bar-fill" style={{ width: `${a.score * 100}%` }} />
                  </div>
                </div>
              ))}
            </div>
            <div>
              <h3>Themes</h3>
              {personality.data.themeAffinities.map((a) => (
                <div key={a.name} className="affinity-bar">
                  <span>{a.name}</span>
                  <div className="bar-track">
                    <div className="bar-fill" style={{ width: `${a.score * 100}%` }} />
                  </div>
                </div>
              ))}
            </div>
          </div>
        </section>
      )}

      <form onSubmit={handleAsk} className="ask-form">
        <label htmlFor="ask-input" className="visually-hidden">
          Describe what you want to read
        </label>
        <input
          id="ask-input"
          placeholder='Ask Novi anything, e.g. "something like Dune but shorter"'
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <button type="submit" disabled={asking}>
          {asking ? 'Thinking…' : 'Ask'}
        </button>
        <button type="button" className="secondary" onClick={loadRecommendations} disabled={loading}>
          Reset to my usual picks
        </button>
      </form>

      {askError && <ErrorState message={askError} />}

      {loading && <Spinner label="Building your recommendations…" />}

      {!loading && loadError && <ErrorState message={loadError} onRetry={loadRecommendations} />}

      {!loading && !loadError && (
        <>
          <div className="recommendation-list">
            {recommendations.map((rec) => (
              <RecommendationCard key={rec.id} recommendation={rec} />
            ))}
          </div>
          {recommendations.length === 0 && (
            <EmptyState message="Rate a few books in your library first, then check back here." />
          )}
        </>
      )}
    </div>
  );
}
