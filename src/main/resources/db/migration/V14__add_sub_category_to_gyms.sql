-- V14__add_sub_category_to_gyms.sql

ALTER TABLE gyms ADD COLUMN sub_category_id BIGINT;
ALTER TABLE gyms ADD CONSTRAINT fk_gyms_sub_category FOREIGN KEY (sub_category_id) REFERENCES categories(id) ON DELETE SET NULL;

-- Ensure category_id (main category) is not null. Set fallback if any exist
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM categories LIMIT 1) THEN
        UPDATE gyms SET category_id = (SELECT id FROM categories LIMIT 1) WHERE category_id IS NULL;
    END IF;
END $$;

ALTER TABLE gyms ALTER COLUMN category_id SET NOT NULL;
