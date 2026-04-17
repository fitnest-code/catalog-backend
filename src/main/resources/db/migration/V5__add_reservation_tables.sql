-- Add reservation toggle fields
ALTER TABLE gyms ADD COLUMN is_reservation_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE trainers ADD COLUMN is_reservation_enabled BOOLEAN DEFAULT FALSE;

-- Trainer reservation dates
CREATE TABLE trainer_reservation_dates (
    id SERIAL PRIMARY KEY,
    created_date TIMESTAMP,
    updated_date TIMESTAMP,
    trainer_id BIGINT NOT NULL,
    reservation_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    empty_spaces INTEGER NOT NULL,
    CONSTRAINT fk_trd_trainer FOREIGN KEY (trainer_id) REFERENCES trainers(id) ON DELETE CASCADE
);

-- Reservations
CREATE TABLE reservations (
    id SERIAL PRIMARY KEY,
    created_date TIMESTAMP,
    updated_date TIMESTAMP,
    user_id BIGINT NOT NULL,
    gym_id BIGINT NOT NULL,
    trainer_id BIGINT NOT NULL,
    trainer_reservation_date_id BIGINT NOT NULL,
    lesson_type VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    CONSTRAINT fk_res_gym FOREIGN KEY (gym_id) REFERENCES gyms(id) ON DELETE CASCADE,
    CONSTRAINT fk_res_trainer FOREIGN KEY (trainer_id) REFERENCES trainers(id) ON DELETE CASCADE,
    CONSTRAINT fk_res_trd FOREIGN KEY (trainer_reservation_date_id) REFERENCES trainer_reservation_dates(id) ON DELETE CASCADE
);
