ALTER TABLE reservation DROP CONSTRAINT ck_reservation_expiry;
ALTER TABLE reservation RENAME COLUMN expires_at TO end_at;
ALTER TABLE reservation ADD COLUMN start_at TIMESTAMPTZ;
ALTER TABLE reservation ADD COLUMN number_of_days INTEGER;

UPDATE reservation
SET start_at = created_at,
    number_of_days = GREATEST(1, CEIL(EXTRACT(EPOCH FROM (end_at - created_at)) / 86400.0)::INTEGER);

ALTER TABLE reservation ALTER COLUMN start_at SET NOT NULL;
ALTER TABLE reservation ALTER COLUMN number_of_days SET NOT NULL;
ALTER TABLE reservation ADD CONSTRAINT ck_reservation_period CHECK (end_at > start_at);
ALTER TABLE reservation ADD CONSTRAINT ck_reservation_days CHECK (number_of_days BETWEEN 1 AND 365);

DROP INDEX idx_reservation_active_vehicle;
CREATE INDEX idx_reservation_active_vehicle
    ON reservation(vehicle_id, start_at, end_at)
    WHERE status IN ('HELD', 'CONVERTED');

UPDATE vehicle_type SET code = 'SEDAN', name = 'Sedan' WHERE code = 'ECONOMY';

INSERT INTO vehicle_model (
    id, company_id, vehicle_type_id, make, model,
    tank_capacity_liters, consumption_l_per_100km
) VALUES
('00000000-0000-0000-0000-000000000402', '00000000-0000-0000-0000-000000000001',
 '00000000-0000-0000-0000-000000000302', 'Toyota', 'RAV4', 55.00, 7.20),
('00000000-0000-0000-0000-000000000403', '00000000-0000-0000-0000-000000000001',
 '00000000-0000-0000-0000-000000000303', 'Ford', 'Transit', 70.00, 9.50);

INSERT INTO vehicle (
    id, company_id, vehicle_model_id, rental_location_id,
    vin, registration_number, status, latitude, longitude,
    odometer_km, remaining_range_km, updated_at, version
) VALUES
('00000000-0000-0000-0000-000000000503', '00000000-0000-0000-0000-000000000001',
 '00000000-0000-0000-0000-000000000402', '00000000-0000-0000-0000-000000000201',
 'JTMBR32V720000003', 'WX-SUV-1', 'AVAILABLE', 52.229700, 21.012200, 15000.0, 500.0, CURRENT_TIMESTAMP, 0),
('00000000-0000-0000-0000-000000000504', '00000000-0000-0000-0000-000000000001',
 '00000000-0000-0000-0000-000000000403', '00000000-0000-0000-0000-000000000201',
 'WF0BR32V720000004', 'WX-VAN-1', 'AVAILABLE', 52.229700, 21.012200, 18000.0, 600.0, CURRENT_TIMESTAMP, 0);
