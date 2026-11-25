-- Per-category (main or subcategory) offerings: prices, work hours, services.
-- Additive only — does not alter gym_package_price unique key (V4 sync / home-gym stay intact).
-- gym_entrance_history.category_id is nullable so historical rows remain valid.

ALTER TABLE gym_entrance_history
    ADD COLUMN IF NOT EXISTS category_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_geh_category'
    ) THEN
        ALTER TABLE gym_entrance_history
            ADD CONSTRAINT fk_geh_category
            FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS ix_geh_gym_category ON gym_entrance_history (gym_id, category_id);

-- Subcategory (or any category) package prices parallel to gym_package_price.
CREATE TABLE IF NOT EXISTS gym_category_package_price (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT DEFAULT 0,
    created_date TIMESTAMP,
    last_modified_date TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    gym_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    package_id BIGINT NOT NULL,
    single_price DOUBLE PRECISION,
    unlimited_price DOUBLE PRECISION,
    effective_from DATE NOT NULL,
    CONSTRAINT fk_gcpp_gym FOREIGN KEY (gym_id) REFERENCES gyms(id) ON DELETE CASCADE,
    CONSTRAINT fk_gcpp_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_gcpp_version
    ON gym_category_package_price (gym_id, category_id, package_id, effective_from);

CREATE INDEX IF NOT EXISTS ix_gcpp_gym_category
    ON gym_category_package_price (gym_id, category_id);

-- Per-category work hours (optional; null → fall back to gym general hours in API).
CREATE TABLE IF NOT EXISTS gym_category_work_hours (
    id BIGSERIAL PRIMARY KEY,
    gym_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    period INT NOT NULL,
    from_time TIME,
    to_time TIME,
    CONSTRAINT fk_gcwh_gym FOREIGN KEY (gym_id) REFERENCES gyms(id) ON DELETE CASCADE,
    CONSTRAINT fk_gcwh_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_gcwh_gym_category
    ON gym_category_work_hours (gym_id, category_id);

-- Per-category amenity/service links.
CREATE TABLE IF NOT EXISTS gym_category_services (
    gym_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    supported_service_id BIGINT NOT NULL,
    PRIMARY KEY (gym_id, category_id, supported_service_id),
    CONSTRAINT fk_gcs_gym FOREIGN KEY (gym_id) REFERENCES gyms(id) ON DELETE CASCADE,
    CONSTRAINT fk_gcs_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE,
    CONSTRAINT fk_gcs_service FOREIGN KEY (supported_service_id) REFERENCES supported_services(id) ON DELETE CASCADE
);
