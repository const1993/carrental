-- Ride records are created only when a rental starts, so cancellation belongs
-- to the pre-start reservation lifecycle rather than the ride lifecycle.
UPDATE ride SET status = 'FINISHED' WHERE status = 'CANCELLED';

ALTER TABLE ride DROP CONSTRAINT ride_status_check;
ALTER TABLE ride ADD CONSTRAINT ride_status_check CHECK (status IN ('ACTIVE', 'FINISHED'));
