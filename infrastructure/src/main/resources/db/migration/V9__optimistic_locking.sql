-- Flyway migration: Add optimistic-locking counters to aggregate roots.
-- The repositories check-and-increment this column on every write; a mismatch
-- signals a concurrent modification. Existing rows start at opt_lock 0.

ALTER TABLE tables
    ADD COLUMN opt_lock BIGINT NOT NULL DEFAULT 0;

ALTER TABLE rows
    ADD COLUMN opt_lock BIGINT NOT NULL DEFAULT 0;
