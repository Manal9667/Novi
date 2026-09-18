-- Keep the persisted rating type aligned with the Java Integer entity field.
ALTER TABLE ratings
    ALTER COLUMN stars TYPE INTEGER;