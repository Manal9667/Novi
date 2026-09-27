import React from 'react';
import { Link } from 'react-router-dom';

const FEATURES = [
  {
    title: 'Track every read',
    body: 'Organize your books as Want to Read, Currently Reading, Read, or DNF — and keep a full reading history.'
  },
  {
    title: 'Recommendations that learn you',
    body: 'Novi builds a taste profile from what you rate, review, and finish, then explains why each pick fits you.'
  },
  {
    title: 'Scan a shelf, build a library',
    body: 'Snap a photo of your bookshelf and Novi identifies the spines so you can import them in one go.'
  }
];

/**
 * Public entry point for visitors who aren't signed in. Explains what Novi is
 * and routes to sign up / log in, rather than bouncing straight to the login form.
 */
export default function Landing() {
  return (
    <div className="page landing">
      <section className="landing-hero">
        <h1>Read more. Discover better.</h1>
        <p className="landing-subtitle">
          Novi is a social reading platform with recommendations that actually understand your taste —
          grounded in the books you've loved, not just what's popular.
        </p>
        <div className="landing-cta">
          <Link to="/register" className="button-link">
            Get started — it's free
          </Link>
          <Link to="/login" className="button-link secondary">
            Log in
          </Link>
        </div>
        <p className="subtle">No email required. Just a username and password.</p>
      </section>

      <section className="landing-features">
        {FEATURES.map((f) => (
          <div key={f.title} className="landing-feature">
            <h2>{f.title}</h2>
            <p>{f.body}</p>
          </div>
        ))}
      </section>
    </div>
  );
}
