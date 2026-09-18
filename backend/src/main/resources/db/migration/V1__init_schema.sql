-- Novi Phase 1 core schema

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(32)  NOT NULL UNIQUE,
    display_name    VARCHAR(64)  NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    email           VARCHAR(255),
    bio             VARCHAR(500),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE authors (
    id      BIGSERIAL PRIMARY KEY,
    name    VARCHAR(200) NOT NULL UNIQUE
);

CREATE TABLE genres (
    id      BIGSERIAL PRIMARY KEY,
    name    VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE books (
    id                          BIGSERIAL PRIMARY KEY,
    title                       VARCHAR(500)  NOT NULL,
    description                 VARCHAR(4000),
    cover_image_url             VARCHAR(1000),
    isbn                        VARCHAR(20),
    publication_date            DATE,
    external_metadata_id        VARCHAR(100),
    external_metadata_source    VARCHAR(50),
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_books_title ON books (title);
CREATE INDEX idx_books_isbn ON books (isbn);
CREATE UNIQUE INDEX uk_books_external ON books (external_metadata_source, external_metadata_id)
    WHERE external_metadata_source IS NOT NULL;

CREATE TABLE book_authors (
    book_id     BIGINT NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    author_id   BIGINT NOT NULL REFERENCES authors(id) ON DELETE CASCADE,
    PRIMARY KEY (book_id, author_id)
);

CREATE TABLE book_genres (
    book_id     BIGINT NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    genre_id    BIGINT NOT NULL REFERENCES genres(id) ON DELETE CASCADE,
    PRIMARY KEY (book_id, genre_id)
);

CREATE TABLE user_books (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    book_id         BIGINT NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    status          VARCHAR(20) NOT NULL,
    started_at      TIMESTAMPTZ,
    finished_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_user_books_user_book UNIQUE (user_id, book_id)
);

CREATE INDEX idx_user_books_user ON user_books (user_id);
CREATE INDEX idx_user_books_status ON user_books (status);

CREATE TABLE ratings (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    book_id         BIGINT NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    stars           INTEGER NOT NULL CHECK (stars BETWEEN 1 AND 5),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_ratings_user_book UNIQUE (user_id, book_id)
);

CREATE INDEX idx_ratings_book ON ratings (book_id);

CREATE TABLE reviews (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    book_id         BIGINT NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    content         VARCHAR(5000) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_reviews_book ON reviews (book_id);
CREATE INDEX idx_reviews_user ON reviews (user_id);

CREATE TABLE reading_history (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    book_id         BIGINT NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    event_type      VARCHAR(30) NOT NULL,
    occurred_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_reading_history_user ON reading_history (user_id);
CREATE INDEX idx_reading_history_occurred_at ON reading_history (occurred_at);

CREATE TABLE shelves (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_shelves_user_name UNIQUE (user_id, name)
);

CREATE TABLE shelf_books (
    id              BIGSERIAL PRIMARY KEY,
    shelf_id        BIGINT NOT NULL REFERENCES shelves(id) ON DELETE CASCADE,
    book_id         BIGINT NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    added_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_shelf_books_shelf_book UNIQUE (shelf_id, book_id)
);
