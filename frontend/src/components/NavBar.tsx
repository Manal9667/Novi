import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export function NavBar() {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate('/login');
  }

  return (
    <nav className="navbar">
      <Link to="/" className="brand">Novi</Link>
      {isAuthenticated ? (
        <div className="nav-links">
          <Link to="/">Home</Link>
          <Link to="/recommendations">For You</Link>
          <Link to="/search">Search</Link>
          <Link to="/library">My Library</Link>
          <Link to="/shelves">My Shelves</Link>
          <Link to="/history">History</Link>
          <Link to={`/profile/${user?.username}`}>Profile</Link>
          <Link to="/settings">Settings</Link>
          <button onClick={handleLogout} className="link-button">Log out</button>
        </div>
      ) : (
        <div className="nav-links">
          <Link to="/login">Log in</Link>
          <Link to="/register">Sign up</Link>
        </div>
      )}
    </nav>
  );
}
