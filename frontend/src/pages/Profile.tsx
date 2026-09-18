import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { apiClient } from '../api/client';
import type { UserProfile } from '../types';

export default function Profile() {
  const { username } = useParams();
  const [profile, setProfile] = useState<UserProfile | null>(null);

  useEffect(() => {
    if (!username) return;
    apiClient.get<UserProfile>(`/users/${username}`).then((res) => setProfile(res.data));
  }, [username]);

  if (!profile) return <div className="page">Loading...</div>;

  return (
    <div className="page">
      <h1>{profile.displayName}</h1>
      <p className="subtle">@{profile.username}</p>
      {profile.bio && <p>{profile.bio}</p>}
      <div className="stats-grid">
        <div><strong>{profile.booksRead}</strong><span>Books read</span></div>
        <div><strong>{profile.currentlyReading}</strong><span>Currently reading</span></div>
        <div><strong>{profile.wantToRead}</strong><span>Want to read</span></div>
        <div><strong>{profile.reviewCount}</strong><span>Reviews</span></div>
      </div>
    </div>
  );
}
