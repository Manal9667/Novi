import React from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import { BookIcon, CompassIcon, MoonIcon, SunIcon } from './icons';

/** Brand wordmark with a small bookmark mark. */
function Brand() {
  return (
    <Link to="/" className="brand" aria-label="Novi home">
      <span className="brand-mark" aria-hidden="true"><BookIcon size={22} /></span>
      Novi
    </Link>
  );
}

function ThemeToggle() {
  const { theme, toggle } = useTheme();
  const night = theme === 'night';
  return (
    <button
      type="button"
      className="icon-button"
      onClick={toggle}
      aria-pressed={night}
      aria-label={night ? 'Switch to day reading mode' : 'Switch to night reading mode'}
      title={night ? 'Day mode' : 'Night mode'}
    >
      {night ? <SunIcon size={20} /> : <MoonIcon size={20} />}
    </button>
  );
}

export function NavBar() {
  const { isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate('/login');
  }

  if (!isAuthenticated) {
    return (
      <nav className="navbar">
        <Brand />
        <div className="nav-right">
          <ThemeToggle />
          <NavLink to="/login" className="nav-link">Log in</NavLink>
          <Link to="/register" className="button-link btn-sm">Sign up</Link>
        </div>
      </nav>
    );
  }

  const navClass = ({ isActive }: { isActive: boolean }) => (isActive ? 'nav-link active' : 'nav-link');
  const tabClass = ({ isActive }: { isActive: boolean }) => (isActive ? 'active' : undefined);

  return (
    <>
      <nav className="navbar">
        <Brand />
        <div className="nav-links desktop-only">
          <NavLink to="/" end className={navClass}>Discover</NavLink>
          <NavLink to="/library" className={navClass}>Saved</NavLink>
        </div>
        <div className="nav-right">
          <ThemeToggle />
          <button type="button" className="nav-link unstyled" onClick={handleLogout}>Log out</button>
        </div>
      </nav>

      {/* Mobile bottom tab bar */}
      <nav className="tabbar mobile-only" aria-label="Primary">
        <NavLink to="/" end className={tabClass}><CompassIcon size={22} /><span>Discover</span></NavLink>
        <NavLink to="/library" className={tabClass}><BookIcon size={22} /><span>Saved</span></NavLink>
      </nav>
    </>
  );
}
