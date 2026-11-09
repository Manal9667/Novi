import React from 'react';
import { Link } from 'react-router-dom';
import { CompassIcon, SparkleIcon, BookmarkIcon } from '../components/icons';

const FEATURES = [
  {
    icon: <CompassIcon size={24} />,
    title: 'Describe it in plain words',
    body: 'Skip the filters and genre menus. Tell Novi the mood, themes, and vibe you want — the way you’d ask a friend who reads everything.'
  },
  {
    icon: <SparkleIcon size={24} />,
    title: 'Understand the request',
    body: 'Novi interprets your request, searches a curated catalog by meaning (not just keywords), and ranks the best matches.'
  },
  {
    icon: <BookmarkIcon size={24} />,
    title: 'Know why each pick fits',
    body: 'Every recommendation comes with a short, honest explanation grounded in the book itself. Save the ones that speak to you.'
  }
];

/** Public landing page for signed-out visitors. */
export default function Landing() {
  return (
    <div className="page landing">
      <section className="landing-hero">
        <p className="eyebrow">AI-powered book discovery</p>
        <h1>Find your next read by describing it.</h1>
        <p className="landing-subtitle measure">
          Novi is a book curator that understands what you actually want to read — a psychological
          thriller with an unreliable narrator, a cozy mystery for the weekend, literary sci-fi about
          memory. Describe it, and Novi finds it.
        </p>
        <div className="landing-cta">
          <Link to="/register" className="button-link">Get started — it’s free</Link>
          <Link to="/login" className="button-link secondary">Log in</Link>
        </div>
        <p className="subtle">No email required. Just a username and password.</p>
      </section>

      <section className="landing-features">
        {FEATURES.map((f) => (
          <div key={f.title} className="landing-feature">
            <span className="feature-icon" aria-hidden="true">{f.icon}</span>
            <h2>{f.title}</h2>
            <p>{f.body}</p>
          </div>
        ))}
      </section>
    </div>
  );
}
