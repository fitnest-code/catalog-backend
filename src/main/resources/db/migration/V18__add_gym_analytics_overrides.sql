-- Admin manual overrides for the gym analytics summary cards.
-- NULL means "use the value computed from gym entrance history".
ALTER TABLE gyms ADD COLUMN IF NOT EXISTS analytics_profit_override DOUBLE PRECISION;
ALTER TABLE gyms ADD COLUMN IF NOT EXISTS analytics_successful_override BIGINT;
ALTER TABLE gyms ADD COLUMN IF NOT EXISTS analytics_failed_override BIGINT;
