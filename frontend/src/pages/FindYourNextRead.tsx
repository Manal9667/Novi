import React, { useState } from 'react';
import { RecommendationCard } from '../components/RecommendationCard';
import { ErrorState } from '../components/states/ErrorState';
import { EmptyState } from '../components/states/EmptyState';
import { NookIllustration, CompassIcon } from '../components/icons';
import { getApiErrorMessage, recommendationService } from '../services';
import type { RecommendationResponse } from '../types';

const EXAMPLES = [
  'Chinese historical fiction about family',
  'A short mystery I can finish this weekend',
  'Something like Dune but less dense',
  'A best seller romance with great banter',
  'A cozy mystery set in a small town'
];

/**
 * "Find Your Next Read" — the agentic, natural-language recommender. The user's
 * request is the primary signal, so it works for brand-new readers too.
 */
export default function FindYourNextRead() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<RecommendationResponse[] | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [lastQuery, setLastQuery] = useState('');

  async function runSearch(raw: string) {
    const trimmed = raw.trim();
    if (!trimmed) return;
    setLoading(true);
    setError(null);
    try {
      const r = await recommendationService.findBooks(trimmed);
      setResults(r);
      setLastQuery(trimmed);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Novi couldn’t finish browsing just now. Please try again.'));
    } finally {
      setLoading(false);
    }
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    void runSearch(query);
  }

  return (
    <div className="page find">
      <section className="find-hero card card-pad">
        <NookIllustration className="find-illo" />
        <h1>What are you in the mood to read?</h1>
        <p className="hand find-sub">Tell me a genre, a mood, an author, a vibe — anything.</p>
        <form onSubmit={handleSubmit} className="find-form">
          <label htmlFor="find-input" className="visually-hidden">Describe what you want to read</label>
          <textarea
            id="find-input"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder='e.g. "a beautifully written but hopeful novel about grief"'
            rows={2}
            maxLength={500}
          />
          <button type="submit" disabled={loading || !query.trim()} className="btn-accent">
            <CompassIcon size={18} /> {loading ? 'Browsing the shelves…' : 'Find books'}
          </button>
        </form>
        <div className="find-examples">
          {EXAMPLES.map((ex) => (
            <button key={ex} type="button" className="sticky" disabled={loading} onClick={() => { setQuery(ex); void runSearch(ex); }}>
              {ex}
            </button>
          ))}
        </div>
      </section>

      {error && <ErrorState message={error} onRetry={lastQuery ? () => runSearch(lastQuery) : undefined} />}

      {loading && (
        <div className="find-loading" role="status" aria-live="polite">
          <span className="spinner" aria-hidden="true" />
          <p className="hand">Novi is browsing the shelves for you…</p>
        </div>
      )}

      {!loading && !error && results && (
        <section>
          <div className="section-header"><h2>Books Novi found for you</h2></div>
          {results.length > 0 ? (
            <div className="recommendation-list">
              {results.map((rec) => (
                <RecommendationCard key={rec.id} recommendation={rec} />
              ))}
            </div>
          ) : (
            <EmptyState
              title="Nothing quite matched"
              message={`I couldn't find a good fit for "${lastQuery}". Try describing the feeling you're after, or a book you loved.`}
            />
          )}
        </section>
      )}
    </div>
  );
}
