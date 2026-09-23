-- Real pgvector-backed semantic retrieval.
--
-- Novi previously stored embeddings only as a JSON float-array in a TEXT column
-- and computed cosine similarity in the application layer over the whole
-- catalog. This migration adds a native pgvector column and an ANN index so
-- candidate retrieval is done in the database with an index-backed
-- nearest-neighbour search instead of a full scan.
--
-- The JSON TEXT column (books.embedding) remains the app-facing source of
-- truth; embedding_vec is a derived, indexed representation kept in sync by the
-- application (BookEmbeddingService) and backfilled here for existing rows.
--
-- Dimensionality note: voyage-3.5 (the configured embedding model) produces
-- 1024-dimensional embeddings, so the column is vector(1024). A deployment that
-- switches to a model with a different output dimension must adjust this and the
-- backfill cast accordingly.
CREATE EXTENSION IF NOT EXISTS vector;

ALTER TABLE books ADD COLUMN embedding_vec vector(1024);

-- Backfill embeddings imported before this migration. The JSON float-array text
-- (e.g. [0.12,-0.03,...]) is exactly pgvector's input format, so a direct cast
-- works. Only rows that already carry a 1024-dim embedding are affected.
UPDATE books SET embedding_vec = embedding::vector(1024) WHERE embedding IS NOT NULL;

-- Approximate-nearest-neighbour index for cosine distance (the <=> operator).
-- HNSW gives fast, index-backed similarity search that scales past the
-- in-application full-catalog scan.
CREATE INDEX idx_books_embedding_vec_hnsw
    ON books USING hnsw (embedding_vec vector_cosine_ops);
