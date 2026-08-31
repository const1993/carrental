# Car Rental Backend

Java 21 / Spring Boot modular monolith for a single car-rental company, intentionally structured so identity, fleet and payments can later be extracted into services.

## Key decisions

- One deployable application and one PostgreSQL database.
- Internal modules: `identity`, `fleet`, `rental`, `payment`.
- Roles: `ADMIN`, `CUSTOMER`, `VEHICLE_DEVICE`.
  - `VEHICLE_DEVICE` replaces the ambiguous `CAR` role and represents a machine identity for telemetry.
- Scheduled reservation by car type (`SEDAN`, `SUV`, or `VAN`) and number of days.
- Reservation endpoint is idempotent via `Idempotency-Key`.
- A finite fleet vehicle is assigned internally, preventing bookings beyond type capacity.
- Non-overlapping periods can reuse the same vehicle.
- Fixed daily price per vehicle type. `finish ride` computes price server-side, records payment atomically, and supports a different return branch.
- Vehicle removal is logical (`RETIRED`) to preserve rental/payment history.
- Optional `vehicle_inspection` table is included, with no public V1 API yet.
- Flyway owns schema; Hibernate runs with `ddl-auto=validate`.

## Stack

- Java 21
- Spring Boot 4.1.0
- Spring Security JWT
- Spring Data JPA / Hibernate
- PostgreSQL 17
- Flyway
- springdoc-openapi 3.0.3
- Gradle
- Testcontainers 2.0.5
- Docker / Kubernetes

## Local run

```bash
docker compose up -d postgres
gradle bootRun --args='--spring.profiles.active=local'
```

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

Runtime OpenAPI:

```text
http://localhost:8080/v3/api-docs
```

### Demo credentials with `local` profile

| Role | Email | Password |
|---|---|---|
| ADMIN | `admin@local.test` | `admin12345` |
| CUSTOMER | `customer@local.test` | `customer12345` |
| VEHICLE_DEVICE | `vehicle@local.test` | `vehicle12345` |

The seeded device is bound to vehicle `00000000-0000-0000-0000-000000000501`.

## API summary

| Capability | Endpoint | Role |
|---|---|---|
| Register customer | `POST /api/v1/auth/register` | Public |
| Login | `POST /api/v1/auth/login` | Public |
| Find cars | `GET /api/v1/vehicles/search?type=SEDAN&latitude=...&longitude=...` | CUSTOMER, ADMIN |
| Get car state | `GET /api/v1/vehicles/{id}/state` | Authenticated |
| Reserve car type and period | `POST /api/v1/reservations` + `Idempotency-Key` | CUSTOMER |
| Cancel reservation | `POST /api/v1/reservations/{id}/cancel` | CUSTOMER |
| Start ride | `POST /api/v1/rides/start` | CUSTOMER |
| Finish + payment | `POST /api/v1/rides/{id}/finish` | CUSTOMER |
| Get ride | `GET /api/v1/rides/{id}` | CUSTOMER |
| Add car | `POST /api/v1/admin/vehicles` | ADMIN |
| Retire car | `DELETE /api/v1/admin/vehicles/{id}` | ADMIN |
| Update GPS | `PATCH /api/v1/vehicles/{id}/position` | VEHICLE_DEVICE, ADMIN |
| Report fuel remaining | `PATCH /api/v1/vehicles/{id}/fuel` | VEHICLE_DEVICE, ADMIN |

When an admin adds a vehicle, the API derives its rental branch from the supplied latitude and longitude by selecting the nearest active company location; callers do not submit a location ID.

Vehicle devices report `fuelLiters`; the backend validates it against tank capacity and calculates `remainingRangeKm` from the model's average liters-per-100-km consumption.

Vehicle search accepts optional latitude and longitude together. When supplied, it returns vehicles within `VEHICLE_SEARCH_RADIUS_KM` (25 km by default); with neither coordinate, it searches all available vehicles.

## Reservation / ACID behavior

See the [rental state diagram](docs/rental-state-diagram.md) for reservation, cancellation, start, and finish transitions.

The reserve use case executes in one transaction:

1. Acquire a PostgreSQL transaction advisory lock for `(customer_id, idempotency_key)`.
2. Replay an existing reservation only if type, start, and duration match.
3. Find fleet vehicles of the requested enum type.
4. Lock candidate vehicle rows with pessimistic write locking.
5. Reject candidates with an overlapping active reservation.
6. Assign the first available finite-fleet vehicle for the requested period.

The database also has a unique constraint for `(customer_id, idempotency_key)`. Candidate vehicle locks serialize concurrent competition for the finite fleet.

`finish ride` changes the ride state, moves the vehicle to the selected active return branch, calculates the fixed-rate charge and writes the payment record in a single transaction. The pickup and return branches are both stored on the ride.

## Tests

Integration tests use PostgreSQL 17 through Testcontainers:

```bash
gradle test
```

Included integration scenarios:
- exactly `SEDAN`, `SUV`, and `VAN` are exposed;
- requested type, date/time, and number of days are persisted;
- overlapping requests cannot exceed finite type inventory;
- non-overlapping periods can reuse a vehicle;
- same idempotency key returns the same reservation;
- reserve -> start -> finish at another branch -> payment record.

## Container image

Buildpacks:

```bash
gradle bootBuildImage
```

Docker:

```bash
docker build -t car-rental-backend:local .
```

Kubernetes manifests are in `k8s/`, including a rootless BuildKit image-build Job.

## Architecture

See `docs/architecture.md` for Mermaid diagrams and the intended future extraction into:
- identity/customer service;
- fleet/car service;
- reservation/rental service;
- payment service.
