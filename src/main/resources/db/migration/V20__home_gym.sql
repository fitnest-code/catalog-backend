-- Home gym (spec R1/R14/R15/R17/R18)

-- Master switch: can the gym be selected as a home gym.
ALTER TABLE gyms ADD COLUMN IF NOT EXISTS home_eligible BOOLEAN NOT NULL DEFAULT TRUE;

-- Versioned per-package pricing. A change inserts a new row for the 1st day of the next month.
CREATE TABLE IF NOT EXISTS gym_package_price (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT DEFAULT 0,
    created_date TIMESTAMP,
    last_modified_date TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    gym_id BIGINT NOT NULL,
    package_id BIGINT NOT NULL,
    included BOOLEAN DEFAULT TRUE,
    home_eligible BOOLEAN,
    single_price DOUBLE PRECISION,
    unlimited_price DOUBLE PRECISION,
    effective_from DATE NOT NULL,
    CONSTRAINT fk_gpp_gym FOREIGN KEY (gym_id) REFERENCES gyms(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_gym_package_price_version
    ON gym_package_price (gym_id, package_id, effective_from);

-- Optional time-window rules. No rule at all means "gym working hours".
CREATE TABLE IF NOT EXISTS gym_time_slot_rule (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT DEFAULT 0,
    created_date TIMESTAMP,
    last_modified_date TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    gym_id BIGINT NOT NULL,
    weekdays VARCHAR(128),
    start_time TIME,
    end_time TIME,
    price_package_id BIGINT,
    effective_from DATE NOT NULL,
    effective_to DATE,
    CONSTRAINT fk_gtsr_gym FOREIGN KEY (gym_id) REFERENCES gyms(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS gym_time_slot_rule_packages (
    rule_id BIGINT NOT NULL,
    package_id BIGINT NOT NULL,
    CONSTRAINT fk_gtsrp_rule FOREIGN KEY (rule_id) REFERENCES gym_time_slot_rule(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_gym_time_slot_rule_gym ON gym_time_slot_rule (gym_id);
CREATE INDEX IF NOT EXISTS ix_gym_package_price_gym ON gym_package_price (gym_id);
