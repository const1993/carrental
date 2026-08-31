CREATE TABLE company (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL UNIQUE
);

CREATE TABLE rental_location (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES company(id),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    address VARCHAR(500),
    latitude NUMERIC(9,6) NOT NULL CHECK (latitude BETWEEN -90 AND 90),
    longitude NUMERIC(9,6) NOT NULL CHECK (longitude BETWEEN -180 AND 180),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_rental_location_company_code UNIQUE (company_id, code)
);

CREATE TABLE vehicle_type (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES company(id),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    daily_rate NUMERIC(12,2) NOT NULL CHECK (daily_rate >= 0),
    currency VARCHAR(3) NOT NULL,
    CONSTRAINT uk_vehicle_type_company_code UNIQUE (company_id, code)
);

CREATE TABLE vehicle_model (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES company(id),
    vehicle_type_id UUID NOT NULL REFERENCES vehicle_type(id),
    make VARCHAR(100) NOT NULL,
    model VARCHAR(100) NOT NULL,
    tank_capacity_liters NUMERIC(7,2) NOT NULL CHECK (tank_capacity_liters > 0),
    consumption_l_per_100km NUMERIC(6,2) NOT NULL CHECK (consumption_l_per_100km > 0),
    CONSTRAINT uk_vehicle_model_company_make_model_type
        UNIQUE (company_id, vehicle_type_id, make, model)
);

CREATE TABLE vehicle (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES company(id),
    vehicle_model_id UUID NOT NULL REFERENCES vehicle_model(id),
    rental_location_id UUID NOT NULL REFERENCES rental_location(id),
    vin VARCHAR(17) NOT NULL UNIQUE,
    registration_number VARCHAR(50) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL CHECK (status IN ('AVAILABLE','IN_RIDE','MAINTENANCE','RETIRED')),
    latitude NUMERIC(9,6) NOT NULL CHECK (latitude BETWEEN -90 AND 90),
    longitude NUMERIC(9,6) NOT NULL CHECK (longitude BETWEEN -180 AND 180),
    odometer_km NUMERIC(12,1) NOT NULL CHECK (odometer_km >= 0),
    remaining_range_km NUMERIC(10,1) NOT NULL CHECK (remaining_range_km >= 0),
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_vehicle_company_status ON vehicle(company_id, status);
CREATE INDEX idx_vehicle_location ON vehicle(rental_location_id);
CREATE INDEX idx_vehicle_model ON vehicle(vehicle_model_id);

CREATE TABLE customer (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE auth_user (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(30) NOT NULL CHECK (role IN ('ADMIN','VEHICLE_DEVICE','CUSTOMER')),
    subject_ref UUID,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE reservation (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES company(id),
    vehicle_id UUID NOT NULL REFERENCES vehicle(id),
    customer_id UUID NOT NULL REFERENCES customer(id),
    status VARCHAR(30) NOT NULL CHECK (status IN ('HELD','CONVERTED','CANCELLED','EXPIRED')),
    idempotency_key VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_reservation_expiry CHECK (expires_at > created_at),
    CONSTRAINT uk_reservation_customer_idempotency UNIQUE (customer_id, idempotency_key)
);

CREATE INDEX idx_reservation_active_vehicle
    ON reservation(vehicle_id, expires_at)
    WHERE status = 'HELD';

CREATE TABLE ride (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES company(id),
    reservation_id UUID NOT NULL UNIQUE REFERENCES reservation(id),
    vehicle_id UUID NOT NULL REFERENCES vehicle(id),
    customer_id UUID NOT NULL REFERENCES customer(id),
    pickup_location_id UUID NOT NULL REFERENCES rental_location(id),
    return_location_id UUID REFERENCES rental_location(id),
    status VARCHAR(30) NOT NULL CHECK (status IN ('ACTIVE','FINISHED','CANCELLED')),
    started_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ,
    billed_days INTEGER CHECK (billed_days >= 0),
    final_amount NUMERIC(12,2) CHECK (final_amount >= 0),
    currency VARCHAR(3)
);

CREATE UNIQUE INDEX uk_one_active_ride_per_vehicle
    ON ride(vehicle_id)
    WHERE status = 'ACTIVE';

CREATE INDEX idx_ride_customer ON ride(customer_id, started_at DESC);

CREATE TABLE payment (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES company(id),
    ride_id UUID NOT NULL UNIQUE REFERENCES ride(id),
    customer_id UUID NOT NULL REFERENCES customer(id),
    amount NUMERIC(12,2) NOT NULL CHECK (amount >= 0),
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(30) NOT NULL CHECK (status IN ('RECORDED')),
    reference VARCHAR(100),
    recorded_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE vehicle_inspection (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES company(id),
    vehicle_id UUID NOT NULL REFERENCES vehicle(id),
    ride_id UUID REFERENCES ride(id),
    inspection_type VARCHAR(30) NOT NULL CHECK (inspection_type IN ('PICKUP','RETURN','MAINTENANCE')),
    odometer_km NUMERIC(12,1),
    fuel_percent NUMERIC(5,2),
    damage_notes TEXT,
    created_at TIMESTAMPTZ NOT NULL
);

COMMENT ON TABLE vehicle_inspection IS 'Optional V1 audit extension; no public API yet.';
