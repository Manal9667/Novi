import React, { useEffect, useState } from 'react';
import { apiClient } from '../api/client';
import { RecommendationCard } from '../components/RecommendationCard';
import type { ReadingPersonalityResponse, RecommendationResponse } from '../types';

export default function Recommendations() {
  const [recommendations, setRecommendations] = useState<RecommendationResponse[]>([]);
  const [personality, setPersonality] = useState<ReadingPersonalityResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState('');
  const [asking, setAsking] = useState(false);

  function loadRecommendations() {
    setLoading(true);
    apiClient.get<RecommendationResponse[]>('/recommendations')
      .finally(() => setLoading(false))
      .then((res) => setRecommendations(res.data));
  }

  useEffect(() => {
    loadRecommendations();
    apiClient.get<ReadingPersonalityResponse>('/recommendations/reading-personality')
      .then((res) => setPersonality(res.data));
  }, []);

  async function handleAsk(e: React.FormEvent) {
    e.preventDefault();
    if (!query.trim()) return;
    setAsking(true);
    try {
      const { data } = await apiClient.post<RecommendationResponse[]>('/recommendations/ask', { query });
      setRecommendations(data);
    } finally {
      setAsking(false);
    }
  }

  return (
    <div className="page">
      <h1>Recommended for you</h1>

      {personality && (
        <section className="reading-personality">
          <h2>Your Reading Personality</h2>
          <p>{personality.summary}</p>
          <div className="affinity-columns">
            <div>
              <h3>Genres</h3>
              {personality.genreAffinities.map((a) => (
                <div key={a.name} className="affinity-bar">
                  <span>{a.name}</span>
                  <div className="bar-track"><div className="bar-fill" style={{ width: `${a.score * 100}%` }} /></div>
                </div>
              ))}
            </div>
            <div>
              <h3>Themes</h3>
              {personality.themeAffinities.map((a) => (
                <div key={a.name} className="affinity-bar">
                  <span>{a.name}</span>
                  <div className="bar-track"><div className="bar-fill" style={{ width: `${a.score * 100}%` }} /></div>
                </div>
              ))}
            </div>
          </div>
        </section>
      )}

      <form onSubmit={handleAsk} className="ask-form">
        <input
          placeholder='Ask Novi anything, e.g. "something like Dune but shorter"'
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <button type="submit" disabled={asking}>{asking ? 'Thinking...' : 'Ask'}</button>
        <button type="button" className="secondary" onClick={loadRecommendations}>Reset to my usual picks</button>
      </form>

      {loading && <p className="subtle">Building your recommendations...</p>}

      <div className="recommendation-list">
        {recommendations.map((rec) => <RecommendationCard key={rec.id} recommendation={rec} />)}
      </div>

      {!loading && recommendations.length === 0 && (
        <p className="subtle">Rate a few books in your library first, then check back here.</p>
      )}
    </div>
  );
}
