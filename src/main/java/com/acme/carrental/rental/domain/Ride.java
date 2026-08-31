package com.acme.carrental.rental.domain;

import com.acme.carrental.fleet.domain.RentalLocation;
import com.acme.carrental.fleet.domain.Vehicle;
import com.acme.carrental.shared.domain.Currency;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ride")
public class Ride {
    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false, unique = true)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pickup_location_id", nullable = false)
    private RentalLocation pickupLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_location_id")
    private RentalLocation returnLocation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RideStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "billed_days")
    private Integer billedDays;

    @Column(name = "final_amount", precision = 12, scale = 2)
    private BigDecimal finalAmount;

    @Enumerated(EnumType.STRING)
    @Column(length = 3)
    private Currency currency;

    protected Ride() {}

    public Ride(
        UUID id,
        UUID companyId,
        Reservation reservation,
        Vehicle vehicle,
        UUID customerId,
        Instant startedAt
    ) {
        this.id = id;
        this.companyId = companyId;
        this.reservation = reservation;
        this.vehicle = vehicle;
        this.customerId = customerId;
        this.pickupLocation = vehicle.getRentalLocation();
        this.status = RideStatus.ACTIVE;
        this.startedAt = startedAt;
    }

    public UUID getId() { return id; }
    public Reservation getReservation() { return reservation; }
    public Vehicle getVehicle() { return vehicle; }
    public UUID getCustomerId() { return customerId; }
    public RideStatus getStatus() { return status; }
    public RentalLocation getPickupLocation() { return pickupLocation; }
    public RentalLocation getReturnLocation() { return returnLocation; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public Integer getBilledDays() { return billedDays; }
    public BigDecimal getFinalAmount() { return finalAmount; }
    public Currency getCurrency() { return currency; }

    public void finish(
        Instant at,
        int billedDays,
        BigDecimal amount,
        Currency currency,
        RentalLocation returnLocation
    ) {
        this.status = RideStatus.FINISHED;
        this.finishedAt = at;
        this.billedDays = billedDays;
        this.finalAmount = amount;
        this.currency = currency;
        this.returnLocation = returnLocation;
    }

}
