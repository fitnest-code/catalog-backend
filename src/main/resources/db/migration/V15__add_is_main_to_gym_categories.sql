-- V15__add_is_main_to_gym_categories.sql
ALTER TABLE gym_categories
    ADD COLUMN IF NOT EXISTS is_main BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE gym_categories gc
SET is_main = TRUE
WHERE (
          SELECT COUNT(*) FROM gym_categories gc2
          WHERE gc2.gym_id = gc.gym_id
      ) = 1;

CREATE INDEX IF NOT EXISTS idx_gym_categories_is_main
    ON gym_categories (gym_id, is_main);