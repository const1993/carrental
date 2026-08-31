package com.acme.carrental.fleet.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "vehicle_model")
public class VehicleModel {
    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_type_id", nullable = false)
    private VehicleType type;

    @Column(nullable = false)
    private String make;

    @Column(nullable = false)
    private String model;

    @Column(name = "tank_capacity_liters", nullable = false, precision = 7, scale = 2)
    private BigDecimal tankCapacityLiters;

    @Column(name = "consumption_l_per_100km", nullable = false, precision = 6, scale = 2)
    private BigDecimal consumptionLitersPer100Km;

    protected VehicleModel() {}

    public VehicleModel(
        UUID id,
        UUID companyId,
        VehicleType type,
        String make,
        String model,
        BigDecimal tankCapacityLiters,
        BigDecimal consumptionLitersPer100Km
    ) {
        this.id = id;
        this.companyId = companyId;
        this.type = type;
        this.make = make;
        this.model = model;
        this.tankCapacityLiters = tankCapacityLiters;
        this.consumptionLitersPer100Km = consumptionLitersPer100Km;
    }

    public UUID getId() { return id; }
    public VehicleType getType() { return type; }
    public String getMake() { return make; }
    public String getModel() { return model; }
    public BigDecimal getTankCapacityLiters() { return tankCapacityLiters; }
    public BigDecimal getConsumptionLitersPer100Km() { return consumptionLitersPer100Km; }
}
