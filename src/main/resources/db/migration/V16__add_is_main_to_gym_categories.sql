-- Mövcud data üçün DEFAULT ilə əlavə et,
-- sonra constraint-i update et.
ALTER TABLE gym_categories
    ADD COLUMN IF NOT EXISTS is_main BOOLEAN DEFAULT FALSE;

-- Mövcud null dəyərləri false ilə doldur
UPDATE gym_categories SET is_main = FALSE WHERE is_main IS NULL;

-- İndi NOT NULL constraint əlavə et
ALTER TABLE gym_categories
    ALTER COLUMN is_main SET NOT NULL;

-- İndeks
CREATE INDEX IF NOT EXISTS idx_gym_categories_is_main
    ON gym_categories (gym_id, is_main);