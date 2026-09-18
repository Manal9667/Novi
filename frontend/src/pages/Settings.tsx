import React from 'react';
import { useAuth } from '../context/AuthContext';

export default function Settings() {
  const { user } = useAuth();

  return (
    <div className="page">
      <h1>Settings</h1>
      <p>Username: <strong>{user?.username}</strong></p>
      <p>Display name: <strong>{user?.displayName}</strong></p>
      <p className="subtle">
        Novi never required an email to sign up. Account recovery via an optional
        email address is planned for a future release.
      </p>
    </div>
  );
}
