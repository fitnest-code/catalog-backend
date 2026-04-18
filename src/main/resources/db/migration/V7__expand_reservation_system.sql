-- Expansion for GymLessonType
ALTER TABLE gym_lesson_types ADD COLUMN category_id BIGINT;
ALTER TABLE gym_lesson_types ADD COLUMN status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE gym_lesson_types ADD COLUMN sort_order INTEGER DEFAULT 0;

-- Add constraint for category link
ALTER TABLE gym_lesson_types ADD CONSTRAINT fk_glt_category FOREIGN KEY (category_id) REFERENCES categories(id);

-- Expansion for TrainerReservationDate (conceptually reservation_session)
ALTER TABLE trainer_reservation_dates ADD COLUMN gym_id BIGINT;
ALTER TABLE trainer_reservation_dates ADD COLUMN class_type_id BIGINT;
ALTER TABLE trainer_reservation_dates ADD COLUMN status VARCHAR(20) DEFAULT 'OPEN';

-- Add constraints for TrainerReservationDate
ALTER TABLE trainer_reservation_dates ADD CONSTRAINT fk_trd_gym FOREIGN KEY (gym_id) REFERENCES gyms(id);
ALTER TABLE trainer_reservation_dates ADD CONSTRAINT fk_trd_class_type FOREIGN KEY (class_type_id) REFERENCES gym_lesson_types(id);

-- Expansion for Reservation (conceptually customer_reservation)
ALTER TABLE reservations ADD COLUMN category_id BIGINT;
ALTER TABLE reservations ADD COLUMN class_type_id BIGINT;
ALTER TABLE reservations ADD COLUMN approved_at TIMESTAMP;
ALTER TABLE reservations ADD COLUMN cancelled_at TIMESTAMP;
ALTER TABLE reservations ADD COLUMN cancel_reason_code VARCHAR(50);
ALTER TABLE reservations ADD COLUMN cancel_reason_text TEXT;
ALTER TABLE reservations ADD COLUMN cancel_additional_note TEXT;

-- Add constraints for Reservation
ALTER TABLE reservations ADD CONSTRAINT fk_res_category FOREIGN KEY (category_id) REFERENCES categories(id);
ALTER TABLE reservations ADD CONSTRAINT fk_res_class_type FOREIGN KEY (class_type_id) REFERENCES gym_lesson_types(id);

-- New table: Reservation Rules
CREATE TABLE reservation_rules (
    id BIGSERIAL PRIMARY KEY,
    gym_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    sort_order INTEGER DEFAULT 0,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rules_gym FOREIGN KEY (gym_id) REFERENCES gyms(id),
    CONSTRAINT fk_rules_category FOREIGN KEY (category_id) REFERENCES categories(id)
);

-- New table: Cancellation Reasons
CREATE TABLE reservation_cancel_reasons (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    label VARCHAR(255) NOT NULL,
    requires_comment BOOLEAN DEFAULT FALSE,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- New table: Audit Logs
CREATE TABLE reservation_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    reservation_id BIGINT,
    user_id BIGINT,
    operation VARCHAR(50),
    old_status VARCHAR(20),
    new_status VARCHAR(20),
    reason TEXT,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Initial Cancellation Reasons
INSERT INTO reservation_cancel_reasons (code, label, requires_comment) VALUES
('PLANS_CHANGED', 'Plans changed', false),
('WRONG_RESERVATION', 'Wrong reservation', false),
('HEALTH_RELATED', 'Health related', false),
('URGENT_WORK', 'Urgent work', false),
('OTHER', 'Other', true);
