-- Novi schema: a focused AI book recommendation engine.
--
-- Entities: users (auth), a curated book catalog (books + authors/genres/themes
-- many-to-many), per-book semantic embeddings, and each user's saved books.

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

CREATE TABLE themes (
    id      BIGSERIAL PRIMARY KEY,
    name    VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE books (
    id                      BIGSERIAL PRIMARY KEY,
    title                   VARCHAR(500)  NOT NULL,
    description             VARCHAR(4000),
    cover_image_url         VARCHAR(1000),
    isbn                    VARCHAR(20),
    publication_date        DATE,
    -- Semantic embedding of the book, stored as a JSON float-array text (the
    -- app-facing source of truth). Mirrored into the native vector column added
    -- by V2 when pgvector is available. Null until embedded.
    embedding               TEXT,
    embedding_model         VARCHAR(50),
    embedding_updated_at    TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_books_title ON books (title);
CREATE INDEX idx_books_isbn ON books (isbn);

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

CREATE TABLE book_themes (
    book_id     BIGINT NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    theme_id    BIGINT NOT NULL REFERENCES themes(id) ON DELETE CASCADE,
    PRIMARY KEY (book_id, theme_id)
);

-- A user's saved books. The (user, book) pair is unique so a book can't be
-- saved twice. status defaults to SAVED; the other values exist for a possible
-- future lightweight shelf, but the product treats this as a saved list.
CREATE TABLE user_books (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    book_id         BIGINT NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    status          VARCHAR(20) NOT NULL DEFAULT 'SAVED',
    started_at      TIMESTAMPTZ,
    finished_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_user_books_user_book UNIQUE (user_id, book_id)
);

CREATE INDEX idx_user_books_user ON user_books (user_id);
