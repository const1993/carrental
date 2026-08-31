package com.acme.carrental.fleet.domain;

import com.acme.carrental.shared.domain.Currency;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "vehicle_type")
public class VehicleType {
    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "daily_rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal dailyRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    protected VehicleType() {}

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public BigDecimal getDailyRate() { return dailyRate; }
    public Currency getCurrency() { return currency; }
}
