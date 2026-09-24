-- Real pgvector-backed semantic retrieval - applied only when the pgvector
-- extension is actually available on the server.
--
-- Novi previously stored embeddings only as a JSON float-array in a TEXT column
-- and computed cosine similarity in the application layer over the whole
-- catalog. When pgvector is present this migration adds a native vector column
-- plus an HNSW index so candidate retrieval becomes an index-backed
-- nearest-neighbour search (see CandidateRetrievalService).
--
-- IMPORTANT: this migration must never hard-fail startup on a database without
-- pgvector. It tries to enable the extension; if that isn't possible it logs a
-- notice and skips the vector column/index entirely. Retrieval then falls back
-- to the application-layer / genre-overlap path, so the app still boots and
-- works on any PostgreSQL. Use a pgvector-enabled image (the bundled
-- docker-compose and the Testcontainers tests use pgvector/pgvector:pg16) to
-- get the index-backed path.
--
-- Dimensionality note: voyage-3.5 (the configured embedding model) produces
-- 1024-dimensional embeddings, so the column is vector(1024). A deployment that
-- switches to a model with a different output dimension must adjust this.
DO $$
BEGIN
    -- Enable pgvector; if the extension isn't installed on this server, skip the
    -- rest of the migration rather than failing the whole startup.
    BEGIN
        CREATE EXTENSION IF NOT EXISTS vector;
    EXCEPTION WHEN OTHERS THEN
        RAISE NOTICE 'pgvector extension unavailable (%); skipping native vector column and index. '
                     'Retrieval will use the application-layer fallback.', SQLERRM;
        RETURN;
    END;

    ALTER TABLE books ADD COLUMN IF NOT EXISTS embedding_vec vector(1024);

    -- Backfill embeddings imported before this migration. The JSON float-array
    -- text (e.g. [0.12,-0.03,...]) is exactly pgvector's input format.
    UPDATE books SET embedding_vec = embedding::vector(1024)
        WHERE embedding IS NOT NULL AND embedding_vec IS NULL;

    -- HNSW index for fast approximate-nearest-neighbour cosine search (<=>).
    CREATE INDEX IF NOT EXISTS idx_books_embedding_vec_hnsw
        ON books USING hnsw (embedding_vec vector_cosine_ops);
END$$;
