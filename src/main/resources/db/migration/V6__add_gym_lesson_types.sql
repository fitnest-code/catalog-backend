-- Gym lesson types
CREATE TABLE gym_lesson_types (
    id SERIAL PRIMARY KEY,
    created_date TIMESTAMP,
    updated_date TIMESTAMP,
    gym_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    CONSTRAINT fk_glt_gym FOREIGN KEY (gym_id) REFERENCES gyms(id) ON DELETE CASCADE,
    CONSTRAINT uq_gym_lesson_type UNIQUE (gym_id, name)
);
