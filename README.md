# Novi — AI-Powered Book Recommendation Engine

Novi helps you find your next read by describing it in plain language. Instead of
genre menus and keyword filters, you tell Novi what you're in the mood for —

> "A psychological thriller with an unreliable narrator, dark atmosphere, and an
> unexpected ending. Something like *The Silent Patient*."

— and Novi interprets the request, retrieves semantically similar books from a
curated catalog, ranks them with a transparent scoring model, and explains, for
each pick, *why* it fits. Sign in to save the ones you like.

It's a deliberately small product built to do one thing well, and to show a
complete, production-shaped **Retrieval-Augmented Generation (RAG)** pipeline on
a real stack.

---

## Tech stack

| Layer | Technology |
|---|---|
| Frontend | React 18, TypeScript, Vite, React Router |
| Backend | Java 21, Spring Boot 3.3, Spring Security (JWT), Spring Data JPA |
| Database | PostgreSQL + `pgvector` (HNSW index, cosine distance) |
| AI | Google Gemini — `gemini-embedding-001` (embeddings) + a Gemini chat model (query understanding & explanations), via Gemini's OpenAI-compatible API |
| Migrations | Flyway |
| Testing | JUnit 5, Mockito, Testcontainers (backend); Vitest + Testing Library (frontend) |
| Infra | Docker, Docker Compose |

---

## Architecture

```
          React (Vite)                     Spring Boot
   ┌───────────────────────┐        ┌──────────────────────────┐
   │ Discovery page         │  HTTP  │ RecommendationController  │
   │  (natural-language box)│──────▶ │   └ RecommendationService │  ← orchestrates the pipeline
   │ Recommendation cards   │        │       ├ QueryIntentService │  (1) understand
   │ Book detail            │        │       ├ EmbeddingService   │  (2) embed
   │ Saved books (auth)     │        │       ├ CandidateRetrieval │  (2) retrieve (pgvector + lexical)
   └───────────────────────┘        │       ├ RankingService     │  (3) rank (transparent, weighted)
                                     │       └ ExplanationService │  (4) explain (grounded)
                                     ├──────────────────────────┤
                                     │ LibraryController (saved) │
                                     │ AuthController (JWT)       │
                                     └───────────┬──────────────┘
                                                 │ JPA / native pgvector
                                          ┌──────▼───────┐        ┌──────────────┐
                                          │ PostgreSQL   │◀──────▶│ Google Gemini │
                                          │ + pgvector   │        └──────────────┘
                                          └──────────────┘
```

Controllers stay thin; business logic lives in services; persistence in
repositories; API contracts are DTOs (JPA entities are never exposed). Every AI
call has a deterministic fallback, so the app works end-to-end even with no API
key configured.

---

## The recommendation pipeline (RAG)

A single natural-language request flows through five modular steps in
`RecommendationService.recommend(user, query)`:

1. **Query understanding** — `QueryIntentService` asks Gemini to turn the free
   text into a structured intent (genres, themes, cultural context, authors,
   "similar to", moods, keywords). Malformed or missing LLM output falls back to
   deterministic keyword extraction.
2. **Semantic retrieval** — the request is embedded once with
   `gemini-embedding-001` (768-dim) and used for an index-backed
   nearest-neighbour search over `pgvector` (cosine distance), blended with a
   lexical keyword search. More candidates are retrieved than are shown.
3. **Hybrid ranking** — `RankingService` scores each candidate with a
   transparent, configurable weighted formula (no black-box model):

   ```
   relevance = (wSemantic · cosineSimilarity + wMetadata · metadataMatch) / (wSemantic + wMetadata)
   final     = (1 − wPreference) · relevance + wPreference · savedBookAffinity   // preference only when signed in
   ```

   Weights live under `novi.ai.ranking` (defaults 0.6 / 0.25 / 0.15). Duplicate
   books are removed and the list is trimmed to the final count.
4. **Grounded explanation** — `ExplanationService` passes only the top books'
   real metadata to Gemini and asks for a 1–2 sentence reason per book,
   explicitly forbidding invented facts. Any book the model skips gets a
   deterministic, metadata-grounded fallback sentence.
5. **Response** — each recommendation is returned with book id, title, authors,
   cover, description, genres, themes, an interpretable `relevanceScore`
   (0–100), and the explanation.

**Graceful degradation:** with no `GEMINI_API_KEY`, step 1 uses keyword intent,
step 2 uses lexical retrieval only, and step 4 uses the deterministic fallback —
the endpoint still returns sensible, ranked, explained results.

The catalog is **seeded and embedded once**, not fetched from an external API
per request, so recommendations are fast and self-contained.

---

## Database

- **Schema** (Flyway, `backend/src/main/resources/db/migration`):
  - `V1__init_schema.sql` — `users`, `authors`, `genres`, `themes`, `books`
    (+ `book_authors` / `book_genres` / `book_themes` join tables), `user_books`
    (saved books). Primary/foreign keys, unique constraints, indexes.
  - `V2__pgvector_embeddings.sql` — enables `pgvector` (gracefully skipped if
    unavailable), adds `books.embedding_vec vector(768)`, and an **HNSW** index
    over cosine distance.
- **Seeding** — `CatalogSeeder` loads a curated set of ~64 real books from
  `backend/src/main/resources/data/books.json` on startup. It is idempotent
  (dedupes by ISBN / title), so restarts never duplicate rows.
- **Embeddings** — after seeding, any book missing an embedding is embedded once
  via Gemini and mirrored into the `pgvector` column. Existing embeddings are
  never regenerated.

> The embedding dimension (`768`) must match the `vector(768)` column. If you
> change `NOVI_EMBEDDING_DIMENSIONS`, update `V2__pgvector_embeddings.sql` and
> re-embed.

---

## Environment variables

| Variable | Required | Default | Purpose |
|---|---|---|---|
| `JWT_SECRET` | yes (prod) | dev-only placeholder | HMAC secret for JWTs (≥ 32 bytes) |
| `GEMINI_API_KEY` | no | — | Enables Gemini query understanding, embeddings, explanations |
| `GEMINI_MODEL` | no | `gemini-flash-lite-latest` | Chat model for intent + explanations |
| `GEMINI_EMBEDDING_MODEL` | no | `gemini-embedding-001` | Embedding model |
| `NOVI_EMBEDDING_DIMENSIONS` | no | `768` | Embedding size (must match the migration) |
| `CORS_ALLOWED_ORIGINS` | no | `http://localhost:*` | Browser origins allowed to call the API |
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | no | localhost / 5432 / novi | Database connection |

Secrets are read from the environment only — never hard-coded or committed.
`.env` is git-ignored. A TLS-intercepting network (corporate proxy / antivirus)
can break outbound HTTPS to Gemini; drop the intercepting root CA into
`backend/certs/*.cer` and the Docker build trusts it automatically.

---

## Running locally

### With Docker Compose (recommended)

```bash
# A strong secret is required; a Gemini key is optional but enables the AI path.
export JWT_SECRET="$(openssl rand -base64 48)"
export GEMINI_API_KEY="your-key"   # optional

docker compose up --build
```

- Frontend: http://localhost:5173
- Backend API: http://localhost:8080
- Database: `pgvector/pgvector:pg16` on 5432

> If you changed migrations against an existing volume, reset it first:
> `docker compose down -v`.

### Manually

**Database**
```bash
docker compose up -d db
```

**Backend** (Java 21, Maven)
```bash
cd backend
SPRING_PROFILES_ACTIVE=dev GEMINI_API_KEY=your-key mvn spring-boot:run
```
The `dev` profile permits the committed placeholder `JWT_SECRET` for local use.

**Frontend** (Node 18+)
```bash
cd frontend
npm install
npm run dev   # proxies /api -> http://localhost:8080
```

---

## Testing

**Backend** (`cd backend`)
```bash
mvn test      # unit tests (Gemini mocked): query parsing, ranking, explanations,
              # duplicate prevention, saved-book ops, orchestration
mvn verify    # + Testcontainers integration tests (auth, recommendation API)
```
Integration tests spin up a disposable `pgvector` Postgres via Testcontainers.
If your environment can't drive Testcontainers (e.g. Docker Desktop discovery
issues on Windows), point them at a running database instead:
```bash
IT_DB_URL=jdbc:postgresql://localhost:5432/novi IT_DB_USERNAME=novi IT_DB_PASSWORD=novi mvn verify
```
No test makes a live Gemini call — the AI client is mocked (unit) or absent
(integration), so the deterministic fallbacks are exercised.

**Frontend** (`cd frontend`)
```bash
npm run test:run   # Vitest + Testing Library: recommendation form, result
                   # rendering, save/unsave, loading / empty / error states
```

---

## Evaluation

Retrieval quality is measured, not assumed. The dependency-free `com.novi.eval`
toolkit computes Precision@K, Recall@K, MRR, and nDCG@K and compares embedding
representation strategies on a reproducible synthetic dataset:

```bash
cd backend
mvn -q compile
java -cp target/classes com.novi.eval.RetrievalEvaluationRunner
```

It quantifies a real design choice — L2-normalized **cosine** clearly beats raw
dot-product on this embedding space — which is why retrieval uses cosine
distance. The metrics and harness are covered by unit tests.

---

## Project structure

```
novi/
├── backend/                  # Spring Boot API
│   └── src/main/java/com/novi/
│       ├── controller/       # Auth, Book, Library, Recommendation
│       ├── service/          # RAG pipeline: QueryIntent, CandidateRetrieval,
│       │                     #   Ranking, Explanation, GeminiEmbedding, LlmClient,
│       │                     #   RecommendationService, LibraryService, CatalogSeeder
│       ├── repository/ entity/ dto/ security/ config/ exception/
│       └── eval/             # offline retrieval-quality harness
│   └── src/main/resources/
│       ├── db/migration/     # Flyway V1 (schema) + V2 (pgvector)
│       └── data/books.json   # curated seed catalog
├── frontend/                 # React + TypeScript (Vite)
│   └── src/{pages,components,services,context,styles,types}
├── docker-compose.yml
└── README.md
```

---

## Scope, by design

Novi intentionally does **not** include social features, reviews/ratings,
reading-progress tracking, shelves, a photo scanner, or analytics dashboards. It
does one thing — natural-language book discovery with explainable
recommendations — and tries to do it well.
