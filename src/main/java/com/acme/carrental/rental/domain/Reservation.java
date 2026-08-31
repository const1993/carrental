package com.acme.carrental.rental.domain;

import com.acme.carrental.fleet.domain.Vehicle;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "reservation",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_reservation_customer_idempotency",
        columnNames = {"customer_id", "idempotency_key"}
    )
)
public class Reservation {
    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(name = "number_of_days", nullable = false)
    private int numberOfDays;

    protected Reservation() {}

    public Reservation(
        UUID id,
        UUID companyId,
        Vehicle vehicle,
        UUID customerId,
        String idempotencyKey,
        Instant createdAt,
        Instant startAt,
        Instant endAt,
        int numberOfDays
    ) {
        this.id = id;
        this.companyId = companyId;
        this.vehicle = vehicle;
        this.customerId = customerId;
        this.status = ReservationStatus.HELD;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
        this.startAt = startAt;
        this.endAt = endAt;
        this.numberOfDays = numberOfDays;
    }

    public UUID getId() { return id; }
    public Vehicle getVehicle() { return vehicle; }
    public UUID getCustomerId() { return customerId; }
    public ReservationStatus getStatus() { return status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getStartAt() { return startAt; }
    public Instant getEndAt() { return endAt; }
    public int getNumberOfDays() { return numberOfDays; }

    public boolean isExpiredAt(Instant now) {
        return !endAt.isAfter(now);
    }

    public void expire() {
        if (status == ReservationStatus.HELD) {
            status = ReservationStatus.EXPIRED;
        }
    }

    public void cancel() {
        if (status == ReservationStatus.HELD) {
            status = ReservationStatus.CANCELLED;
        }
    }

    public void convert() {
        if (status == ReservationStatus.HELD) {
            status = ReservationStatus.CONVERTED;
        }
    }
}
