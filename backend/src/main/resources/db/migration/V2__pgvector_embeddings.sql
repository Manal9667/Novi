-- Native pgvector-backed semantic retrieval.
--
-- Book embeddings are produced by Gemini's gemini-embedding-001 model with an
-- output dimensionality of 768 (a Matryoshka-truncated tier Google recommends
-- for a good quality/size trade-off). The app stores each embedding as a JSON
-- float-array text in books.embedding (the source of truth) and mirrors it into
-- the native vector column below so candidate retrieval is an index-backed
-- approximate-nearest-neighbour search over cosine distance.
--
-- This migration must never hard-fail startup on a database without pgvector:
-- it tries to enable the extension and, if it can't, skips the vector column and
-- index. Retrieval then degrades gracefully to the lexical (keyword) path. Use a
-- pgvector-enabled image (docker-compose and the Testcontainers tests both use
-- pgvector/pgvector:pg16) to get the index-backed path.
--
-- NOTE: if you switch GEMINI_EMBEDDING_MODEL / NOVI_EMBEDDING_DIMENSIONS to a
-- different output dimension, change vector(768) here to match.
DO $$
BEGIN
    BEGIN
        CREATE EXTENSION IF NOT EXISTS vector;
    EXCEPTION WHEN OTHERS THEN
        RAISE NOTICE 'pgvector extension unavailable (%); skipping native vector column and index. '
                     'Retrieval will use the lexical fallback.', SQLERRM;
        RETURN;
    END;

    ALTER TABLE books ADD COLUMN IF NOT EXISTS embedding_vec vector(768);

    -- Backfill any embeddings written before this column existed. The JSON
    -- float-array text (e.g. [0.12,-0.03,...]) is exactly pgvector's input format.
    UPDATE books SET embedding_vec = embedding::vector(768)
        WHERE embedding IS NOT NULL AND embedding_vec IS NULL;

    -- HNSW index for fast approximate-nearest-neighbour cosine search (<=>).
    CREATE INDEX IF NOT EXISTS idx_books_embedding_vec_hnsw
        ON books USING hnsw (embedding_vec vector_cosine_ops);
END$$;
