package com.acme.carrental.fleet.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vehicle")
public class Vehicle {
    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_model_id", nullable = false)
    private VehicleModel model;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rental_location_id", nullable = false)
    private RentalLocation rentalLocation;

    @Column(nullable = false, unique = true, length = 17)
    private String vin;

    @Column(name = "registration_number", nullable = false, unique = true)
    private String registrationNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleStatus status;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "odometer_km", nullable = false, precision = 12, scale = 1)
    private BigDecimal odometerKm;

    @Column(name = "remaining_range_km", nullable = false, precision = 10, scale = 1)
    private BigDecimal remainingRangeKm;

    @Column(name = "fuel_liters", nullable = false, precision = 7, scale = 2)
    private BigDecimal fuelLiters;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Vehicle() {}

    public Vehicle(
        UUID id,
        UUID companyId,
        VehicleModel model,
        RentalLocation rentalLocation,
        String vin,
        String registrationNumber,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal odometerKm,
        BigDecimal fuelLiters,
        Instant updatedAt
    ) {
        this.id = id;
        this.companyId = companyId;
        this.model = model;
        this.rentalLocation = rentalLocation;
        this.vin = vin;
        this.registrationNumber = registrationNumber;
        this.status = VehicleStatus.AVAILABLE;
        this.latitude = latitude;
        this.longitude = longitude;
        this.odometerKm = odometerKm;
        setFuelAndCalculateRange(fuelLiters);
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public VehicleModel getModel() { return model; }
    public RentalLocation getRentalLocation() { return rentalLocation; }
    public String getVin() { return vin; }
    public String getRegistrationNumber() { return registrationNumber; }
    public VehicleStatus getStatus() { return status; }
    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public BigDecimal getOdometerKm() { return odometerKm; }
    public BigDecimal getRemainingRangeKm() { return remainingRangeKm; }
    public BigDecimal getFuelLiters() { return fuelLiters; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void changePosition(BigDecimal latitude, BigDecimal longitude, Instant now) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.updatedAt = now;
    }

    public void updateFuel(BigDecimal fuelLiters, BigDecimal odometerKm, Instant now) {
        setFuelAndCalculateRange(fuelLiters);
        if (odometerKm != null) {
            if (odometerKm.compareTo(this.odometerKm) < 0) {
                throw new IllegalArgumentException("Odometer cannot move backwards");
            }
            this.odometerKm = odometerKm;
        }
        this.updatedAt = now;
    }

    private void setFuelAndCalculateRange(BigDecimal fuelLiters) {
        if (fuelLiters == null || fuelLiters.signum() < 0) {
            throw new IllegalArgumentException("Fuel remaining cannot be negative");
        }
        if (fuelLiters.compareTo(model.getTankCapacityLiters()) > 0) {
            throw new IllegalArgumentException("Fuel remaining cannot exceed tank capacity");
        }
        this.fuelLiters = fuelLiters;
        this.remainingRangeKm = fuelLiters
            .multiply(BigDecimal.valueOf(100))
            .divide(model.getConsumptionLitersPer100Km(), 1, RoundingMode.HALF_UP);
    }

    public void startRide(Instant now) {
        this.status = VehicleStatus.IN_RIDE;
        this.updatedAt = now;
    }

    public void endRideAt(RentalLocation location, Instant now) {
        this.status = VehicleStatus.AVAILABLE;
        this.rentalLocation = location;
        this.latitude = location.getLatitude();
        this.longitude = location.getLongitude();
        this.updatedAt = now;
    }

    public void retire(Instant now) {
        this.status = VehicleStatus.RETIRED;
        this.updatedAt = now;
    }
}
