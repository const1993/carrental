INSERT INTO company (id, name)
VALUES ('00000000-0000-0000-0000-000000000001', 'Default Car Rental Company');

INSERT INTO rental_location (id, company_id, code, name, address, latitude, longitude, active)
VALUES
('00000000-0000-0000-0000-000000000201',
 '00000000-0000-0000-0000-000000000001',
 'WAW-CENTER', 'Warsaw Center', 'Warsaw, Poland', 52.229700, 21.012200, TRUE),
('00000000-0000-0000-0000-000000000202',
 '00000000-0000-0000-0000-000000000001',
 'WAW-AIRPORT', 'Warsaw Chopin Airport', 'Warsaw Chopin Airport, Poland', 52.165700, 20.967100, TRUE);

INSERT INTO vehicle_type (id, company_id, code, name, daily_rate, currency)
VALUES
('00000000-0000-0000-0000-000000000301',
 '00000000-0000-0000-0000-000000000001',
 'ECONOMY', 'Economy', 149.00, 'PLN'),
('00000000-0000-0000-0000-000000000302',
 '00000000-0000-0000-0000-000000000001',
 'SUV', 'SUV', 279.00, 'PLN'),
('00000000-0000-0000-0000-000000000303',
 '00000000-0000-0000-0000-000000000001',
 'VAN', 'Van', 329.00, 'PLN');

INSERT INTO vehicle_model (
    id, company_id, vehicle_type_id, make, model,
    tank_capacity_liters, consumption_l_per_100km
)
VALUES (
    '00000000-0000-0000-0000-000000000401',
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000301',
    'Toyota', 'Yaris', 42.00, 5.20
);

INSERT INTO vehicle (
    id, company_id, vehicle_model_id, rental_location_id,
    vin, registration_number, status, latitude, longitude,
    odometer_km, remaining_range_km, updated_at, version
)
VALUES (
    '00000000-0000-0000-0000-000000000501',
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000401',
    '00000000-0000-0000-0000-000000000201',
    'JTDBR32E720000001', 'WX-DEMO-1', 'AVAILABLE',
    52.229700, 21.012200, 10000.0, 605.8, CURRENT_TIMESTAMP, 0
);

INSERT INTO vehicle (
    id, company_id, vehicle_model_id, rental_location_id,
    vin, registration_number, status, latitude, longitude,
    odometer_km, remaining_range_km, updated_at, version
)
VALUES (
    '00000000-0000-0000-0000-000000000502',
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000401',
    '00000000-0000-0000-0000-000000000202',
    'JTDBR32E720000002', 'WX-DEMO-2', 'AVAILABLE',
    52.165700, 20.967100, 22000.0, 323.1, CURRENT_TIMESTAMP, 0
);
