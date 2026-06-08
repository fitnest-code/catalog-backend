-- V13__gym_multiple_categories.sql
CREATE TABLE gym_categories (
    gym_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (gym_id, category_id),
    CONSTRAINT fk_gym_categories_gym FOREIGN KEY (gym_id) REFERENCES gyms(id) ON DELETE CASCADE,
    CONSTRAINT fk_gym_categories_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
);

-- Copy existing category assignments to the new table
INSERT INTO gym_categories (gym_id, category_id)
SELECT id, category_id FROM gyms WHERE category_id IS NOT NULL ON CONFLICT DO NOTHING;

-- Add category_id to gym_rooms
ALTER TABLE gym_rooms ADD COLUMN category_id BIGINT;
ALTER TABLE gym_rooms ADD CONSTRAINT fk_gym_rooms_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL;

-- Add category_id to gym_images
ALTER TABLE gym_images ADD COLUMN category_id BIGINT;
ALTER TABLE gym_images ADD CONSTRAINT fk_gym_images_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL;

-- Add category_id to gym_subscriptions
ALTER TABLE gym_subscriptions ADD COLUMN category_id BIGINT;
ALTER TABLE gym_subscriptions ADD CONSTRAINT fk_gym_subscriptions_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL;
