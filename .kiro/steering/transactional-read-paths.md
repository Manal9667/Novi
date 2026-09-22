# Read paths that map entities must be @Transactional

This backend runs with `spring.jpa.open-in-view: false` (see `application.yml`),
and every entity association is `LAZY`. That combination is deliberate — it keeps
web threads from holding a DB session open for the whole request — but it has a
sharp edge:

**Any code that reads an entity and then touches a lazy association (authors,
genres, themes, book, user, …) MUST run inside an active transaction.** If it
doesn't, Hibernate throws `LazyInitializationException`, which surfaces to the
client as a 500.

## The rule

- Service methods that load an entity and map it to a DTO (touching lazy
  associations during the mapping) must be annotated `@Transactional`
  (use `readOnly = true` for pure reads).
- Do the entity → DTO mapping *inside* the transactional service method, not in
  the controller after the transaction has closed.
- Prefer fetching to-one associations with `@EntityGraph` on the repository
  method, and rely on `@BatchSize` (already on `Book`'s collections) for
  to-many associations, to avoid N+1 queries.

## Why this matters

A read endpoint that forgets `@Transactional` compiles fine and looks correct,
then 500s the first time it maps a lazy field. This has bitten the project
before. When you add a new read endpoint, check that its service method is
transactional and that the mapping happens within it.
