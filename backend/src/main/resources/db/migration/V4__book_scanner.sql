-- Novi Phase 3 schema additions: the computer-vision book scanner.
--
-- A scan is persisted as a session plus one candidate row per detected book,
-- so the "scan then confirm" flow has something concrete to reference and so
-- nothing is ever added to a library without an explicit user decision. Both
-- the vision model's own confidence and the metadata-match confidence are
-- kept, alongside the combined score shown to the user.

CREATE TABLE scan_sessions (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type            VARCHAR(20) NOT NULL,   -- SINGLE_BOOK | SHELF
    status          VARCHAR(30) NOT NULL,   -- AWAITING_CONFIRMATION | COMPLETED
    detected_count  INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_scan_sessions_user ON scan_sessions (user_id);

CREATE TABLE scan_candidates (
    id                  BIGSERIAL PRIMARY KEY,
    session_id          BIGINT NOT NULL REFERENCES scan_sessions(id) ON DELETE CASCADE,
    detected_title      VARCHAR(500),
    detected_author     VARCHAR(300),
    vision_confidence   DOUBLE PRECISION NOT NULL DEFAULT 0,
    match_confidence    DOUBLE PRECISION NOT NULL DEFAULT 0,
    confidence          DOUBLE PRECISION NOT NULL DEFAULT 0,
    matched_book_id     BIGINT REFERENCES books(id) ON DELETE SET NULL,
    status              VARCHAR(20) NOT NULL,   -- PENDING | CONFIRMED | SKIPPED
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_scan_candidates_session ON scan_candidates (session_id);
