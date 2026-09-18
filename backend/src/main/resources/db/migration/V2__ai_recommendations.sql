-- Novi Phase 2 schema additions: book embeddings, themes, taste profile,
-- recommendations and recommendation feedback.
--
-- Embeddings are stored as TEXT holding a JSON float array rather than a
-- native pgvector column. This is a deliberate simplification: Novi's
-- catalog is built on demand from search (not a bulk import of millions of
-- books), so candidate retrieval can compute cosine similarity in the
-- application layer without the extra operational dependency of the
-- pgvector extension. See README for the pgvector upgrade path.

ALTER TABLE books
    ADD COLUMN embedding             TEXT,
    ADD COLUMN embedding_model       VARCHAR(50),
    ADD COLUMN embedding_updated_at  TIMESTAMPTZ;

ALTER TABLE users
    ADD COLUMN taste_vector             TEXT,
    ADD COLUMN taste_vector_updated_at  TIMESTAMPTZ;

CREATE TABLE themes (
    id      BIGSERIAL PRIMARY KEY,
    name    VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE book_themes (
    book_id     BIGINT NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    theme_id    BIGINT NOT NULL REFERENCES themes(id) ON DELETE CASCADE,
    PRIMARY KEY (book_id, theme_id)
);

-- Interpretable per-user affinity scores, one row per (user, genre/theme).
-- Recomputed by TasteProfileService whenever a rating/status/review/feedback
-- signal changes, rather than kept live-updated on every write.
CREATE TABLE user_genre_affinity (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    genre_id    BIGINT NOT NULL REFERENCES genres(id) ON DELETE CASCADE,
    score       DOUBLE PRECISION NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_user_genre_affinity UNIQUE (user_id, genre_id)
);

CREATE TABLE user_theme_affinity (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    theme_id    BIGINT NOT NULL REFERENCES themes(id) ON DELETE CASCADE,
    score       DOUBLE PRECISION NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_user_theme_affinity UNIQUE (user_id, theme_id)
);

-- A persisted record of every recommendation Novi has shown a user, so
-- feedback can reference a specific recommendation and so "why did I see
-- this" is always answerable.
CREATE TABLE recommendations (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    book_id             BIGINT NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    match_score         DOUBLE PRECISION NOT NULL,
    reasons             TEXT NOT NULL,           -- JSON array of short strings
    potential_downside  TEXT,
    source              VARCHAR(20) NOT NULL,    -- PROFILE | NATURAL_LANGUAGE
    query_text          VARCHAR(500),            -- populated when source = NATURAL_LANGUAGE
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_recommendations_user ON recommendations (user_id);

CREATE TABLE recommendation_feedback (
    id                  BIGSERIAL PRIMARY KEY,
    recommendation_id   BIGINT NOT NULL REFERENCES recommendations(id) ON DELETE CASCADE,
    user_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    feedback_type       VARCHAR(30) NOT NULL,    -- INTERESTED | NOT_FOR_ME | ADDED_TO_WANT_TO_READ
    reason              VARCHAR(50),             -- optional, only meaningful for NOT_FOR_ME
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_recommendation_feedback UNIQUE (recommendation_id)
);
