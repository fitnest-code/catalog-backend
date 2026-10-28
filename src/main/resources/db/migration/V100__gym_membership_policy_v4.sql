CREATE TABLE gym_membership_policy_v4 (
 gym_id BIGINT PRIMARY KEY REFERENCES gyms(id), version BIGINT NOT NULL DEFAULT 0, payload TEXT NOT NULL
);
CREATE TABLE primary_gym_outbox_v4 (id VARCHAR(36) PRIMARY KEY,payload TEXT NOT NULL,published BOOLEAN NOT NULL DEFAULT FALSE);
CREATE INDEX primary_gym_outbox_v4_pending ON primary_gym_outbox_v4(published) WHERE NOT published;
CREATE TABLE gym_primary_disable_v4 (id BIGSERIAL PRIMARY KEY,gym_id BIGINT NOT NULL REFERENCES gyms(id),package_id BIGINT NOT NULL,disabled_at TIMESTAMP NOT NULL);
CREATE INDEX gym_primary_disable_v4_history ON gym_primary_disable_v4(gym_id,package_id,disabled_at);
