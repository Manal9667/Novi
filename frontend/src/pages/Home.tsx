import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { apiClient } from '../api/client';
import { BookCard } from '../components/BookCard';
import { RecommendationCard } from '../components/RecommendationCard';
import { useAuth } from '../context/AuthContext';
import type { Page, RecommendationResponse, UserBook } from '../types';

export default function Home() {
  const { user } = useAuth();
  const [currentlyReading, setCurrentlyReading] = useState<UserBook[]>([]);
  const [wantToRead, setWantToRead] = useState<UserBook[]>([]);
  const [recentlyFinished, setRecentlyFinished] = useState<UserBook[]>([]);
  const [recommendations, setRecommendations] = useState<RecommendationResponse[]>([]);

  useEffect(() => {
    apiClient.get<Page<UserBook>>('/library', { params: { status: 'CURRENTLY_READING' } })
      .then((res) => setCurrentlyReading(res.data.content));
    apiClient.get<Page<UserBook>>('/library', { params: { status: 'WANT_TO_READ' } })
      .then((res) => setWantToRead(res.data.content));
    apiClient.get<Page<UserBook>>('/library', { params: { status: 'READ' } })
      .then((res) => setRecentlyFinished(res.data.content.slice(0, 6)));
    apiClient.get<RecommendationResponse[]>('/recommendations')
      .then((res) => setRecommendations(res.data.slice(0, 3)))
      .catch(() => setRecommendations([]));
  }, []);

  return (
    <div className="page">
      <h1>Welcome back{user ? `, ${user.displayName}` : ''}</h1>

      {recommendations.length > 0 && (
        <section>
          <div className="section-header">
            <h2>Recommended for You</h2>
            <Link to="/recommendations">See all →</Link>
          </div>
          <div className="recommendation-list">
            {recommendations.map((rec) => <RecommendationCard key={rec.id} recommendation={rec} />)}
          </div>
        </section>
      )}

      <section>
        <h2>Currently Reading</h2>
        <div className="book-grid">
          {currentlyReading.map((ub) => <BookCard key={ub.id} book={ub.book} />)}
        </div>
        {currentlyReading.length === 0 && <p className="subtle">Nothing here yet - <Link to="/search">find a book</Link> to start.</p>}
      </section>

      <section>
        <h2>Recently Finished</h2>
        <div className="book-grid">
          {recentlyFinished.map((ub) => <BookCard key={ub.id} book={ub.book} />)}
        </div>
      </section>

      <section>
        <h2>Want to Read</h2>
        <div className="book-grid">
          {wantToRead.map((ub) => <BookCard key={ub.id} book={ub.book} />)}
        </div>
      </section>
    </div>
  );
}
