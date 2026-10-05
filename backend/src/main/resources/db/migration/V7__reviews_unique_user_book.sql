-- Enforce one review per user per book at the database level.
--
-- Previously this invariant was guarded only in application code
-- (ReviewService.create does a find-then-insert), which is a TOCTOU race: two
-- concurrent POSTs could both pass the check and insert duplicate rows. The
-- ratings table already has an equivalent UNIQUE (user_id, book_id); reviews
-- should match.

-- Collapse any pre-existing duplicates first so the constraint can be added
-- cleanly. Keep the earliest review (lowest id) for each (user, book) pair.
DELETE FROM reviews r
USING reviews dup
WHERE r.user_id = dup.user_id
  AND r.book_id = dup.book_id
  AND r.id > dup.id;

ALTER TABLE reviews
    ADD CONSTRAINT uk_reviews_user_book UNIQUE (user_id, book_id);
