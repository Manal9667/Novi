import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { BookCover } from '../components/BookCover';
import { RecommendationCard } from '../components/RecommendationCard';
import { AsyncSection } from '../components/states/AsyncSection';
import { SparkleIcon, BookmarkIcon } from '../components/icons';
import { useAsync } from '../hooks/useAsync';
import { recommendationService } from '../services';
import type { WrappedResponse } from '../types';

export default function Wrapped() {
  const wrapped = useAsync<WrappedResponse>(() => recommendationService.getWrapped(), []);
  return (
    <div className="page wrapped">
      <AsyncSection state={wrapped} loadingLabel="Wrapping up your reading year…">
        {(w) => <WrappedStory w={w} />}
      </AsyncSection>
    </div>
  );
}

/** A scroll-snapped deck of story cards with a progress bar + keyboard arrows. */
function WrappedDeck({ slides }: { slides: React.ReactNode[] }) {
  const deckRef = useRef<HTMLDivElement>(null);
  const slideRefs = useRef<HTMLDivElement[]>([]);
  const [active, setActive] = useState(0);

  const go = useCallback((i: number) => {
    const clamped = Math.max(0, Math.min(slides.length - 1, i));
    slideRefs.current[clamped]?.scrollIntoView({
      behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth',
      block: 'nearest'
    });
  }, [slides.length]);

  useEffect(() => {
    const deck = deckRef.current;
    if (!deck) return;
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((e) => {
          if (e.isIntersecting) {
            const idx = slideRefs.current.indexOf(e.target as HTMLDivElement);
            if (idx >= 0) setActive(idx);
          }
        });
      },
      { root: deck, threshold: 0.6 }
    );
    slideRefs.current.forEach((el) => el && observer.observe(el));
    return () => observer.disconnect();
  }, [slides.length]);

  function onKeyDown(e: React.KeyboardEvent) {
    if (['ArrowDown', 'ArrowRight', 'PageDown', ' '].includes(e.key)) { e.preventDefault(); go(active + 1); }
    else if (['ArrowUp', 'ArrowLeft', 'PageUp'].includes(e.key)) { e.preventDefault(); go(active - 1); }
    else if (e.key === 'Home') { e.preventDefault(); go(0); }
    else if (e.key === 'End') { e.preventDefault(); go(slides.length - 1); }
  }

  return (
    <div className="wrapped-stage">
      <div className="wrapped-progress" role="tablist" aria-label="Wrapped progress">
        {slides.map((_, i) => (
          <button
            key={i}
            type="button"
            className={i <= active ? 'on' : ''}
            aria-label={`Card ${i + 1} of ${slides.length}`}
            aria-selected={i === active}
            onClick={() => go(i)}
          />
        ))}
      </div>
      <div
        className="wrapped-deck"
        ref={deckRef}
        tabIndex={0}
        onKeyDown={onKeyDown}
        aria-label="Novi Wrapped. Use arrow keys to move between cards."
      >
        {slides.map((slide, i) => (
          <section
            key={i}
            className="wrap-slide"
            ref={(el) => { if (el) slideRefs.current[i] = el; }}
          >
            <div className="wrap-slide-inner">{slide}</div>
          </section>
        ))}
      </div>
    </div>
  );
}

function WrappedStory({ w }: { w: WrappedResponse }) {
  const y = w.yearInBooks;
  const headlineCount = y.booksReadThisYear ?? y.booksRead;
  const hasAnyData =
    y.booksRead > 0 || y.genresExplored > 0 || (y.ratingsCount ?? 0) > 0 || (y.reviewsCount ?? 0) > 0;

  if (!hasAnyData) {
    return (
      <div className="wrapped-stage">
        <div className="wrapped-deck single">
          <section className="wrap-slide v-cover">
            <div className="wrap-slide-inner">
              <SparkleIcon size={40} />
              <h1>Your Novi Wrapped</h1>
              <p>Your reading story is just getting started. Add books, mark a few as read, and rate them — then come back to watch your Wrapped come to life.</p>
              <Link to="/find" className="wrapped-cta">Find your next read</Link>
            </div>
          </section>
        </div>
      </div>
    );
  }

  const slides: React.ReactNode[] = [];

  slides.push(
    <>
      <p className="wrap-eyebrow">{w.year} · in books</p>
      <SparkleIcon size={40} />
      <h1>Your Novi Wrapped</h1>
      <p className="wrap-lead">A cozy look back at your reading year.</p>
      <p className="hand wrap-hint">use the arrows →</p>
    </>
  );

  slides.push(
    <>
      <p className="wrap-eyebrow">This year you finished</p>
      <p className="wrap-big">{headlineCount}</p>
      <p className="wrap-lead">{headlineCount === 1 ? 'book' : 'books'}</p>
    </>
  );

  slides.push(
    <>
      <h2>You explored…</h2>
      <div className="wrap-stats">
        <div><span className="wrap-stat-num">{y.genresExplored}</span><span>genres</span></div>
        <div><span className="wrap-stat-num">{y.authorsRead}</span><span>authors</span></div>
        {y.ratingsCount != null && <div><span className="wrap-stat-num">{y.ratingsCount}</span><span>rated</span></div>}
        {y.averageRating != null && <div><span className="wrap-stat-num">{y.averageRating}★</span><span>avg rating</span></div>}
        {y.reviewsCount != null && <div><span className="wrap-stat-num">{y.reviewsCount}</span><span>reviews</span></div>}
      </div>
    </>
  );

  if (w.topGenres.length > 0) {
    slides.push(
      <>
        <h2>Your top genres</h2>
        <ol className="wrap-rank">
          {w.topGenres.map((g, i) => (
            <li key={g.name}><span className="wrap-rank-num">{i + 1}</span><span className="wrap-rank-name">{g.name}</span><span className="wrap-rank-count">{g.count}</span></li>
          ))}
        </ol>
      </>
    );
  }

  if (w.topAuthors.length > 0) {
    slides.push(
      <>
        <h2>Most-read authors</h2>
        <ol className="wrap-rank">
          {w.topAuthors.map((a, i) => (
            <li key={a.name}><span className="wrap-rank-num">{i + 1}</span><span className="wrap-rank-name">{a.name}</span><span className="wrap-rank-count">{a.count}</span></li>
          ))}
        </ol>
      </>
    );
  }

  slides.push(
    <>
      <p className="wrap-eyebrow">Your reading personality</p>
      <h1 className="wrap-personality">{w.personality.title}</h1>
      <p className="wrap-lead">{w.personality.summary}</p>
      <p className="wrap-fine">A playful profile — for fun, not a personality test.</p>
    </>
  );

  if (w.topThemes.length > 0) {
    slides.push(
      <>
        <h2>Themes you returned to</h2>
        <div className="wrap-chips">
          {w.topThemes.map((t) => <span key={t} className="chip chip-static">{t}</span>)}
        </div>
      </>
    );
  }

  if (w.mostMemorable) {
    slides.push(
      <>
        <p className="wrap-eyebrow"><BookmarkIcon size={16} /> Most memorable</p>
        <Link to={`/books/${w.mostMemorable.book.id}`} className="wrap-memorable-cover">
          <BookCover src={w.mostMemorable.book.coverImageUrl} title={w.mostMemorable.book.title} />
        </Link>
        <h2>{w.mostMemorable.book.title}</h2>
        <p className="subtle">{w.mostMemorable.book.authorNames.join(', ')}</p>
        <p className="wrap-lead">{w.mostMemorable.note}</p>
      </>
    );
  }

  if (w.achievements.length > 0) {
    slides.push(
      <>
        <h2>Badges earned</h2>
        <div className="wrap-badges">
          {w.achievements.map((a) => (
            <div key={a.title} className="wrap-badge"><strong>{a.title}</strong><span>{a.description}</span></div>
          ))}
        </div>
      </>
    );
  }

  if (w.narrative) {
    slides.push(
      <>
        <p className="wrap-eyebrow">Your reading story</p>
        <p className="wrap-narrative">{w.narrative}</p>
      </>
    );
  }

  slides.push(
    <>
      <h2>Your next chapter</h2>
      {w.nextChapter.length > 0 ? (
        <div className="wrap-next-list">
          {w.nextChapter.slice(0, 3).map((rec) => <RecommendationCard key={rec.id} recommendation={rec} />)}
        </div>
      ) : (
        <p className="wrap-lead">Rate a few more books and Novi will line up what's next.</p>
      )}
      <Link to="/find" className="wrapped-cta">Find your next read</Link>
    </>
  );

  return <WrappedDeck slides={slides} />;
}
