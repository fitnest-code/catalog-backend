-- gym_categories cədvəlinə is_main column əlavə et.
-- DEFAULT false — mövcud qeydlər (V1 migration-dan gələnlər) subkateqoriya sayılacaq,
-- lakin legacy fallback logic onları main kimi göstərəcək (sadəcə UI üçün).
ALTER TABLE gym_categories
    ADD COLUMN IF NOT EXISTS is_main BOOLEAN NOT NULL DEFAULT FALSE;

-- Mövcud V1 zalları üçün: hər zalın tək kateqoriyası varsa onu main et.
-- Bu data migration optional-dır, lakin clean data üçün tövsiyə olunur.
UPDATE gym_categories gc
SET is_main = TRUE
WHERE (
          SELECT COUNT(*)
          FROM gym_categories gc2
          WHERE gc2.gym_id = gc.gym_id
      ) = 1;

-- Index — main kateqoriyaları tez filter etmək üçün
CREATE INDEX IF NOT EXISTS idx_gym_categories_is_main
    ON gym_categories (gym_id, is_main);