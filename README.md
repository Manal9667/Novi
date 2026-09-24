# Novi — AI-Powered Social Reading Platform

Novi is a full-stack reading platform that goes beyond simple book tracking: it learns each user's individual reading taste from their ratings, reviews, and reading history, and uses that profile to generate personalized, explainable book recommendations — rather than just recommending books similar to whatever the user is currently viewing.

Users can also build their library the easy way: by taking a photo of a bookshelf and letting Novi's computer-vision pipeline identify and import the books automatically.

---

## Why Novi

Most recommendation features on reading platforms are shallow — "you looked at Dune, here's more sci-fi." Novi is built around a different idea:

> Recommendations should come from a **taste profile** built out of what a user has actually rated, reviewed, finished, and abandoned — not just the book on screen.

A Novi recommendation looks like this:

```
The Secret History — 94% Match

Why:
• You rated If We Were Villains 5★
• You frequently enjoy dark academic settings
• You tend to rate psychological fiction highly
• You prefer complex character relationships

Potential downside:
The pacing is slower than most books you rate highly.
```

---

## Features

### Core Platform
- Secure registration and login (no email required — username + password only)
- Book search backed by a real external metadata source
- Personal library with reading statuses: `WANT_TO_READ`, `CURRENTLY_READING`, `READ`, `DNF`
- Ratings (1–5 stars) and written reviews
- Custom shelves (e.g. "Favorites," "2026 Reads," "Sci-Fi")
- Reading history tracking (added, started, finished, status changes)
- User profiles

### AI Recommendation Engine
- Per-user taste profiles built from ratings, reviews, genres, authors, and reading history, with signal strength weighted by rating (5★ = strong positive, DNF = negative, etc.)
- Book semantic embeddings stored in a native **`pgvector`** column with an **HNSW** index; candidate retrieval is an index-backed approximate-nearest-neighbour search (cosine distance) rather than a full-catalog scan
- Candidate set narrowed from the full catalog down to ~100 books before ranking
- LLM-based reranking of candidates against the user's taste profile
- Retrieval quality is measured with an offline **evaluation harness** (Precision@K, Recall@K, MRR, nDCG@K) — see [Evaluation](#evaluation)
- Explainable recommendations — every suggestion states *why*, grounded in the user's actual data
- Natural-language recommendation requests (e.g. "something like Harry Potter but darker")
- Recommendation feedback (👍 / 👎 / add to Want to Read) that feeds back into future recommendations
- "Your Reading Personality" — a visual + natural-language summary of what Novi has learned about the user

### Computer Vision Book Scanner
- Single-book scanning: photograph a book, extract title/author via OCR/vision, confirm and add
- Bookshelf scanning: photograph a shelf, detect individual spines, extract text, match against book metadata, and bulk-import with confidence scoring
- Low-confidence matches are never silently added — the user always confirms, corrects, or skips

---

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React, TypeScript |
| Backend | Java 21+, Spring Boot, Spring Security, Spring Data JPA, Hibernate |
| Database | PostgreSQL + `pgvector` (HNSW-indexed vector similarity search) |
| AI / Recommendations | Vector embeddings, pgvector ANN retrieval, LLM-based reranking, offline evaluation harness |
| Computer Vision | OCR / vision-language model for spine detection and text extraction |
| Testing | JUnit, Mockito, Spring Boot Test, Testcontainers |
| Infrastructure | Docker, Docker Compose |

---

## Architecture

```
                         ┌───────────────┐
                         │    React      │
                         │   Frontend    │
                         └───────┬───────┘
                                 │
                                 ↓
                    ┌──────────────────────┐
                    │   Spring Boot / Java │
                    │      Backend         │
                    └──────────┬───────────┘
                               │
              ┌────────────────┼────────────────┐
              ↓                ↓                ↓
        PostgreSQL        AI Services       CV Services
                           │                     │
                     Embeddings              OCR/Vision
                     Taste Profile
                     Retrieval + LLM Ranking
```

- Controllers stay thin — business logic lives in the service layer, persistence in the repository layer, and API contracts are defined through DTOs (JPA entities are never exposed directly).
- The recommendation pipeline is retrieval-first: the full catalog is never sent to the LLM. A vector-similarity retrieval step narrows the field before any AI ranking happens.
- Uncertain computer-vision matches are surfaced with a confidence score and require explicit user confirmation before anything is added to a library.

---

## Getting Started

### Prerequisites
- Java 21+
- Node.js
- Docker & Docker Compose
- PostgreSQL (or run via Docker Compose)


## API Overview

```
POST   /api/auth/register
POST   /api/auth/login
POST   /api/auth/logout

GET    /api/books
GET    /api/books/{id}

GET    /api/library
POST   /api/library/books
DELETE /api/library/books/{bookId}
PATCH  /api/library/books/{bookId}/status

POST   /api/books/{bookId}/ratings
POST   /api/books/{bookId}/reviews

GET    /api/reading-history

GET    /api/shelves
POST   /api/shelves

GET    /api/recommendations
POST   /api/recommendations/ask
POST   /api/recommendations/{id}/feedback
GET    /api/recommendations/reading-personality

POST   /api/scan/book              # scan a single physical book (multipart image)
POST   /api/scan/shelf             # scan a whole bookshelf (multipart image)
GET    /api/scan/sessions/{id}     # retrieve a previous scan
POST   /api/scan/sessions/{id}/confirm   # add the confirmed books to the library
```

All endpoints follow REST conventions with proper status codes, request validation, and centralized error handling.

---

## Project Structure

```
novi/
├── backend/          # Spring Boot API
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   ├── dto/
│   ├── mapper/
│   ├── security/
│   └── config/
├── frontend/          # React + TypeScript client
│   └── src/
│       ├── components/
│       ├── pages/
│       ├── hooks/
│       ├── api/
│       └── types/
├── docker-compose.yml
└── README.md
```

---

## Design Decisions Worth Noting

- **No email requirement.** Reading history can be personal, so onboarding only requires a username and password, minimizing unnecessary data collection.
- **Retrieval before ranking.** Recommendations use embeddings + metadata to shrink the candidate pool before any LLM involvement, keeping the AI layer fast and cheap rather than brute-forcing similarity over the whole catalog.
- **Explainability is mandatory.** Every recommendation ships with a reason tied to real user data — no generic "you might also like" text.
- **Confidence over automation.** The book scanner never silently populates a library; low-confidence matches always require user confirmation. A scan is persisted as a session plus one candidate row per detected book, and a combined score (vision confidence × metadata-match confidence) is surfaced so uncertain reads are flagged rather than guessed.
- **Graceful AI degradation.** The Voyage (embeddings) and Anthropic (reranking, explanations, vision) integrations are all optional. Without keys, recommendations fall back to deterministic genre/author-overlap ranking so Phases 1–2 work end-to-end; the Phase 3 scanner, which genuinely needs a vision model, returns a clear `503` explaining it must be enabled.
- **pgvector-backed retrieval.** Book embeddings live in a native `pgvector` column (`books.embedding_vec`, `vector(1024)`) with an HNSW index over cosine distance, so candidate retrieval is an index-backed nearest-neighbour query (`embedding_vec <=> :taste`) instead of scanning the whole catalog. The JSON float-array column remains the app-facing source of truth and is mirrored into the vector column on write (the JSON array text is already valid `pgvector` input); if a database has no vector extension or a candidate lacks an embedding, retrieval degrades gracefully to a bounded genre-affinity scan with app-layer cosine, so recommendations still work. Requires a `pgvector`-enabled Postgres (the bundled `docker-compose.yml` and the Testcontainers integration tests both use `pgvector/pgvector:pg16`).

---

## Evaluation

Retrieval quality is measured, not assumed. The `com.novi.eval` package provides a small, dependency-free evaluation toolkit:

- **`RetrievalMetrics`** — Precision@K, Recall@K, Reciprocal Rank (→ MRR), and nDCG@K over ranked results against a labeled relevant set (binary relevance).
- **`RetrievalEvaluationHarness`** — runs any `Retriever` over a set of labeled queries and aggregates the metrics at a cutoff `k`.
- **`VectorRetriever`** — an in-memory retriever with pluggable scoring strategies (`DOT_PRODUCT`, `COSINE`, `CENTERED_COSINE`) used to compare embedding-representation choices.
- **`SyntheticEvaluationData`** — deterministically generates a labeled, clustered dataset from a fixed seed, so experiments are fully reproducible with no API key or database.
- **`EmbeddingRepresentationExperiment`** — runs every strategy through the harness and selects the best by nDCG@K.

### Reproducible experiment

```bash
cd backend
mvn -q compile
java -cp target/classes com.novi.eval.RetrievalEvaluationRunner
```

Example output (deterministic for the fixed seed):

```
Novi retrieval-quality evaluation (seed=42, dim=64, topics=8, k=10)
-------------------------------------------------------------
DOT_PRODUCT         P@10=0.619  R@10=0.773  MRR=0.984  nDCG@10=0.815
COSINE              P@10=0.772  R@10=0.965  MRR=0.984  nDCG@10=0.965
CENTERED_COSINE     P@10=0.741  R@10=0.926  MRR=0.964  nDCG@10=0.931
-------------------------------------------------------------
Best strategy by nDCG@10: COSINE (nDCG=0.965)
```

The experiment quantifies a real design choice: raw dot-product retrieval scores markedly worse (nDCG 0.815) than L2-normalized cosine (0.965) on this embedding space, because vector magnitude carries no topic signal — which is exactly why retrieval uses cosine distance. The metrics and the experiment are covered by unit tests (`RetrievalMetricsTest`, `RetrievalEvaluationHarnessTest`), so the workflow runs in CI.

---

## Future Improvements

- Optional email-based account recovery
- Import support for external reading-data exports (e.g. Goodreads)
- Expanded natural-language recommendation interface
- Public profiles / social following