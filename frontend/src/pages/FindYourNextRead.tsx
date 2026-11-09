import React, { useState } from 'react';
import { RecommendationCard } from '../components/RecommendationCard';
import { ErrorState } from '../components/states/ErrorState';
import { EmptyState } from '../components/states/EmptyState';
import { NookIllustration, CompassIcon } from '../components/icons';
import { getApiErrorMessage, recommendationService } from '../services';
import type { Recommendation } from '../types';

const EXAMPLES = [
  'A psychological thriller with an unreliable narrator, like The Silent Patient',
  'A short, cozy mystery I can finish this weekend',
  'Literary sci-fi about memory and what makes us human',
  'An epic fantasy with intricate magic and political intrigue',
  'A sweeping historical family saga across generations',
  'Something like Dune but less dense'
];

/**
 * Novi's home: the natural-language book discovery experience. The query box is
 * the central element; the reader describes what they want in plain words and
 * the RAG pipeline returns ranked, explained recommendations.
 */
export default function FindYourNextRead() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<Recommendation[] | null>(null);
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
      <section className="find-hero">
        <NookIllustration className="find-illo" />
        <p className="eyebrow">AI-powered book discovery</p>
        <h1>Tell Novi what you want to read.</h1>
        <p className="find-sub hand">A genre, a mood, an author, a vibe — describe it however you like.</p>

        <form onSubmit={handleSubmit} className="find-form">
          <label htmlFor="find-input" className="visually-hidden">Describe the book you want</label>
          <textarea
            id="find-input"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder='e.g. "a beautifully written but hopeful novel about grief and starting over"'
            rows={3}
            maxLength={500}
          />
          <button type="submit" disabled={loading || !query.trim()} className="btn-accent find-submit">
            <CompassIcon size={18} /> {loading ? 'Finding books…' : 'Find my next read'}
          </button>
        </form>

        <div className="find-examples" aria-label="Example requests">
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
          <p className="hand">Novi is understanding your request and searching the shelves…</p>
        </div>
      )}

      {!loading && !error && results && (
        <section className="results">
          <div className="section-header">
            <h2>Books for “{lastQuery}”</h2>
          </div>
          {results.length > 0 ? (
            <div className="recommendation-list">
              {results.map((rec) => (
                <RecommendationCard key={rec.bookId} rec={rec} />
              ))}
            </div>
          ) : (
            <EmptyState
              title="Nothing quite matched"
              message={`Novi couldn't find a strong fit for "${lastQuery}". Try describing the feeling you're after, or a book you loved.`}
            />
          )}
        </section>
      )}
    </div>
  );
}
