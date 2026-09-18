import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await register(username, displayName, password, confirmPassword);
      navigate('/');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Registration failed');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-page">
      <h1>Create your Novi account</h1>
      <p className="subtle">Just a username and password. Email is never required.</p>
      <form onSubmit={handleSubmit} className="auth-form">
        <label>
          Username
          <input value={username} onChange={(e) => setUsername(e.target.value)} required minLength={3} maxLength={32} />
        </label>
        <label>
          Display name
          <input value={displayName} onChange={(e) => setDisplayName(e.target.value)} required maxLength={64} />
        </label>
        <label>
          Password
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required minLength={8} />
        </label>
        <label>
          Confirm password
          <input type="password" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} required minLength={8} />
        </label>
        {error && <p className="form-error">{error}</p>}
        <button type="submit" disabled={loading}>{loading ? 'Creating account...' : 'Sign up'}</button>
      </form>
      <p>Already have an account? <Link to="/login">Log in</Link></p>
    </div>
  );
}
