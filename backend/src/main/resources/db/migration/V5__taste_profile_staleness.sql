-- Make taste-profile recomputation event-driven rather than run on every read.
--
-- Previously the recommendation and reading-personality read paths called
-- TasteProfileService.recompute() unconditionally, which deletes and reinserts
-- every affinity row (plus recomputes the taste vector) on each request - even
-- when nothing changed. This flag lets writes that affect taste (ratings,
-- library changes, reviews, recommendation feedback) mark the profile stale,
-- so a read only pays for a recompute when there is actually new signal.
--
-- Defaults to TRUE so every existing user recomputes once on their next read.
ALTER TABLE users
    ADD COLUMN taste_profile_stale BOOLEAN NOT NULL DEFAULT TRUE;
