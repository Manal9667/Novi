import React from 'react';
import { useParams } from 'react-router-dom';
import { AsyncSection } from '../components/states/AsyncSection';
import { useAsync } from '../hooks/useAsync';
import { userService } from '../services';
import type { UserProfile } from '../types';

export default function Profile() {
  const { username } = useParams();

  const profileState = useAsync<UserProfile>(() => {
    if (!username) return Promise.reject(new Error('No user specified'));
    return userService.getProfile(username);
  }, [username]);

  return (
    <div className="page">
      <AsyncSection state={profileState} loadingLabel="Loading profile…">
        {(profile) => (
          <>
            <h1>{profile.displayName}</h1>
            <p className="subtle">@{profile.username}</p>
            {profile.bio && <p>{profile.bio}</p>}
            <div className="stats-grid">
              <div>
                <strong>{profile.booksRead}</strong>
                <span>Books read</span>
              </div>
              <div>
                <strong>{profile.currentlyReading}</strong>
                <span>Currently reading</span>
              </div>
              <div>
                <strong>{profile.wantToRead}</strong>
                <span>Want to read</span>
              </div>
              <div>
                <strong>{profile.reviewCount}</strong>
                <span>Reviews</span>
              </div>
            </div>
          </>
        )}
      </AsyncSection>
    </div>
  );
}
