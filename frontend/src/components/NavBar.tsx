import React, { useEffect, useRef, useState } from 'react';
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import {
  BookIcon, CompassIcon, SparkleIcon, ShelfIcon, HomeIcon,
  MoonIcon, SunIcon, MenuIcon, SearchIcon, CameraIcon, ClockIcon, UserIcon, GearIcon
} from './icons';

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
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  // Close the menu on navigation and on Escape / outside click.
  useEffect(() => setMenuOpen(false), [location.pathname]);
  useEffect(() => {
    if (!menuOpen) return;
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && setMenuOpen(false);
    const onClick = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) setMenuOpen(false);
    };
    document.addEventListener('keydown', onKey);
    document.addEventListener('mousedown', onClick);
    return () => {
      document.removeEventListener('keydown', onKey);
      document.removeEventListener('mousedown', onClick);
    };
  }, [menuOpen]);

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
          <NavLink to="/" end className={navClass}>Home</NavLink>
          <NavLink to="/find" className={navClass}>Find</NavLink>
          <NavLink to="/recommendations" className={navClass}>For You</NavLink>
          <NavLink to="/library" className={navClass}>Library</NavLink>
          <NavLink to="/shelves" className={navClass}>Shelves</NavLink>
        </div>

        <div className="nav-right">
          <ThemeToggle />
          <div className="nav-menu" ref={menuRef}>
            <button
              type="button"
              className="icon-button"
              aria-haspopup="menu"
              aria-expanded={menuOpen}
              aria-label="More"
              onClick={() => setMenuOpen((o) => !o)}
            >
              <MenuIcon size={22} />
            </button>
            {menuOpen && (
              <div className="nav-menu-panel" role="menu">
                <NavLink to="/wrapped" role="menuitem"><SparkleIcon size={18} /> Novi Wrapped</NavLink>
                <NavLink to="/search" role="menuitem"><SearchIcon size={18} /> Search</NavLink>
                <NavLink to="/scan" role="menuitem"><CameraIcon size={18} /> Scan a shelf</NavLink>
                <NavLink to="/history" role="menuitem"><ClockIcon size={18} /> Reading history</NavLink>
                <NavLink to={`/profile/${user?.username}`} role="menuitem"><UserIcon size={18} /> Profile</NavLink>
                <NavLink to="/settings" role="menuitem"><GearIcon size={18} /> Settings</NavLink>
                <div className="nav-menu-sep" />
                <button type="button" role="menuitem" className="unstyled" onClick={handleLogout}>Log out</button>
              </div>
            )}
          </div>
        </div>
      </nav>

      {/* Mobile bottom tab bar */}
      <nav className="tabbar mobile-only" aria-label="Primary">
        <NavLink to="/" end className={tabClass}><HomeIcon size={22} /><span>Home</span></NavLink>
        <NavLink to="/find" className={tabClass}><CompassIcon size={22} /><span>Find</span></NavLink>
        <NavLink to="/recommendations" className={tabClass}><SparkleIcon size={22} /><span>For You</span></NavLink>
        <NavLink to="/library" className={tabClass}><BookIcon size={22} /><span>Library</span></NavLink>
        <NavLink to="/shelves" className={tabClass}><ShelfIcon size={22} /><span>Shelves</span></NavLink>
      </nav>
    </>
  );
}
