-- Fix stale check constraint for reservation status
ALTER TABLE reservations DROP CONSTRAINT IF EXISTS reservations_status_check;

ALTER TABLE reservations ADD CONSTRAINT reservations_status_check
    CHECK (status IN ('PENDING', 'APPROVED', 'CANCELLED', 'REJECTED', 'EXPIRED'));
