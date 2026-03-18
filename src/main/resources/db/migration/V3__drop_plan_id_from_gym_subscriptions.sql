-- Migration to drop plan_id column from gym_subscriptions table
ALTER TABLE gym_subscriptions DROP COLUMN plan_id;
