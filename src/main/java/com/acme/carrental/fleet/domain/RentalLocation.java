package com.acme.carrental.fleet.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "rental_location")
public class RentalLocation {
    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    private String address;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(nullable = false)
    private boolean active;

    protected RentalLocation() {}

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public boolean isActive() { return active; }
}
