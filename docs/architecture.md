# Architecture

The application is a **modular monolith**: one deployable image and one PostgreSQL database, with package boundaries aligned to future services.

```mermaid
flowchart LR
    Client --> API[Spring MVC API]

    subgraph Monolith[Car Rental Modular Monolith]
      API --> Identity[identity]
      API --> Fleet[fleet]
      API --> Rental[rental]
      API --> Payment[payment]

      Rental --> Fleet
      Rental --> Payment
      Identity -. JWT subject .-> Rental
      Identity -. JWT subject .-> Fleet
    end

    Identity --> DB[(PostgreSQL)]
    Fleet --> DB
    Rental --> DB
    Payment --> DB
```

Future extraction target:

```mermaid
flowchart LR
    Gateway --> UserService[User / Identity Service]
    Gateway --> CarService[Fleet / Car Service]
    Gateway --> RentalService[Reservation / Rental Service]
    RentalService --> PaymentService[Payment Service]

    UserService --> UserDB[(User DB)]
    CarService --> CarDB[(Car DB)]
    RentalService --> RentalDB[(Rental DB)]
    PaymentService --> PaymentDB[(Payment DB)]
```

## Entity relationships

```mermaid
erDiagram
    COMPANY ||--o{ RENTAL_LOCATION : owns
    COMPANY ||--o{ VEHICLE_TYPE : defines
    VEHICLE_TYPE ||--o{ VEHICLE_MODEL : classifies
    VEHICLE_MODEL ||--o{ VEHICLE : instantiated_as
    RENTAL_LOCATION ||--o{ VEHICLE : home_branch

    CUSTOMER ||--o{ RESERVATION : creates
    VEHICLE ||--o{ RESERVATION : held_for
    RESERVATION ||--o| RIDE : converts_to
    VEHICLE ||--o{ RIDE : used_in
    RENTAL_LOCATION ||--o{ RIDE : pickup_at
    RENTAL_LOCATION ||--o{ RIDE : return_at
    CUSTOMER ||--o{ RIDE : takes
    RIDE ||--o| PAYMENT : records

    VEHICLE ||--o{ VEHICLE_INSPECTION : inspected
    RIDE ||--o{ VEHICLE_INSPECTION : may_have
```

## Reservation concurrency

The reservation endpoint requires `Idempotency-Key`.

Within one PostgreSQL transaction:

1. Acquire a transaction-scoped PostgreSQL advisory lock derived from `(customer_id, idempotency_key)`.
2. Replay an existing reservation only when type, start time, and duration match.
3. Find candidate vehicles for the requested `CarType` enum.
4. Lock candidate `vehicle` rows with `SELECT ... FOR UPDATE`.
5. Reject candidates with an overlapping `HELD` or `CONVERTED` reservation.
6. Assign a vehicle and persist the requested start, end, and number of days.

The advisory lock serializes concurrent replays of the same idempotency key. Vehicle row locks serialize competition for finite type inventory. The unique database constraint on `(customer_id, idempotency_key)` remains the final integrity guard.

Period overlap uses half-open intervals, so adjacent reservations can safely reuse one vehicle.


## Ride completion transaction

Finishing a ride locks the ride and vehicle, validates an active return branch, computes the fixed daily charge on the server, records the return branch, moves the vehicle to that branch, and inserts the payment record in one PostgreSQL transaction. This keeps the ride, fleet location and payment record ACID-consistent.

Cancellation is a reservation-only transition. A ride is created in `ACTIVE` state only when a reservation starts; from there its only supported terminal transition is `FINISHED`.
