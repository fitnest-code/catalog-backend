-- V17__add_gym_descriptions.sql

-- 1. Create gym_descriptions table
CREATE TABLE IF NOT EXISTS gym_descriptions (
    id BIGSERIAL PRIMARY KEY,
    gym_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    description TEXT,
    phone VARCHAR(255),
    CONSTRAINT fk_gym_descriptions_gym FOREIGN KEY (gym_id) REFERENCES gyms(id) ON DELETE CASCADE,
    CONSTRAINT fk_gym_descriptions_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
);

-- 2. Populate the table with existing gym descriptions (mapping to their main categories)
INSERT INTO gym_descriptions (gym_id, category_id, description, phone)
SELECT g.id, gmc.category_id, g.description, g.phone
FROM gyms g
JOIN gym_main_categories gmc ON g.id = gmc.gym_id
WHERE g.description IS NOT NULL OR g.phone IS NOT NULL;
