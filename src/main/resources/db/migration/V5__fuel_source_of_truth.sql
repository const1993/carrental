ALTER TABLE vehicle ADD COLUMN fuel_liters NUMERIC(7,2);

UPDATE vehicle v
SET fuel_liters = ROUND(
    v.remaining_range_km * m.consumption_l_per_100km / 100.0,
    2
)
FROM vehicle_model m
WHERE m.id = v.vehicle_model_id;

ALTER TABLE vehicle ALTER COLUMN fuel_liters SET NOT NULL;
ALTER TABLE vehicle ADD CONSTRAINT ck_vehicle_fuel_non_negative CHECK (fuel_liters >= 0);
