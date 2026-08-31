# Rental state flow

```mermaid
stateDiagram-v2
    direction LR

    state "Reservation" as Reservation {
        [*] --> HELD: reserve(type, start, days)
        HELD --> CANCELLED: cancel before ride starts
        HELD --> EXPIRED: reserved period ends
        HELD --> CONVERTED: start ride
        CANCELLED --> [*]
        EXPIRED --> [*]
    }

    state "Ride" as Ride {
        [*] --> ACTIVE: reservation converted
        ACTIVE --> FINISHED: finish ride / payment recorded
        FINISHED --> [*]
    }

    CONVERTED --> ACTIVE
```

Rules:

- A customer may cancel only while the reservation is `HELD`; no ride or payment exists yet.
- Starting converts the reservation to `CONVERTED` and creates an `ACTIVE` ride.
- There is no ride-cancellation endpoint: a ride exists only after it has started and must then be finished.
- Finishing an `ACTIVE` ride changes it to `FINISHED`, returns the vehicle, calculates billed days, and records payment atomically.
- The reservation cleanup job changes elapsed `HELD` reservations to `EXPIRED`.
