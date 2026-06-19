-- V15__gym_multi_main_and_sub_categories.sql

-- 1. Create main categories join table
CREATE TABLE IF NOT EXISTS gym_main_categories (
    gym_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (gym_id, category_id),
    CONSTRAINT fk_gym_main_categories_gym FOREIGN KEY (gym_id) REFERENCES gyms(id) ON DELETE CASCADE,
    CONSTRAINT fk_gym_main_categories_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
);

-- 2. Create subcategories join table
CREATE TABLE IF NOT EXISTS gym_sub_categories (
    gym_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (gym_id, category_id),
    CONSTRAINT fk_gym_sub_categories_gym FOREIGN KEY (gym_id) REFERENCES gyms(id) ON DELETE CASCADE,
    CONSTRAINT fk_gym_sub_categories_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
);

-- 3. Copy existing singular relationship assignments to the join tables
INSERT INTO gym_main_categories (gym_id, category_id)
SELECT id, category_id FROM gyms WHERE category_id IS NOT NULL 
ON CONFLICT DO NOTHING;

INSERT INTO gym_sub_categories (gym_id, category_id)
SELECT id, sub_category_id FROM gyms WHERE sub_category_id IS NOT NULL 
ON CONFLICT DO NOTHING;

-- 4. Drop the singular columns on gyms table (which cascades to drop the old constraints)
ALTER TABLE gyms DROP COLUMN IF EXISTS category_id CASCADE;
ALTER TABLE gyms DROP COLUMN IF EXISTS sub_category_id CASCADE;
