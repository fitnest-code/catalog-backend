-- Add structured rayon (district) to gym and store addresses
ALTER TABLE gyms ADD COLUMN IF NOT EXISTS rayon VARCHAR(120);
ALTER TABLE stores ADD COLUMN IF NOT EXISTS rayon VARCHAR(120);
