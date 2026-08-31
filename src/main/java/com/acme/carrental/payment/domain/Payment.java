package com.acme.carrental.payment.domain;

import com.acme.carrental.rental.domain.Ride;
import com.acme.carrental.shared.domain.Currency;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment")
public class Payment {
    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ride_id", nullable = false, unique = true)
    private Ride ride;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    private String reference;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected Payment() {}

    public Payment(
        UUID id,
        UUID companyId,
        Ride ride,
        UUID customerId,
        BigDecimal amount,
        Currency currency,
        String reference,
        Instant recordedAt
    ) {
        this.id = id;
        this.companyId = companyId;
        this.ride = ride;
        this.customerId = customerId;
        this.amount = amount;
        this.currency = currency;
        this.status = PaymentStatus.RECORDED;
        this.reference = reference;
        this.recordedAt = recordedAt;
    }

    public UUID getId() { return id; }
    public BigDecimal getAmount() { return amount; }
    public Currency getCurrency() { return currency; }
    public PaymentStatus getStatus() { return status; }
    public String getReference() { return reference; }
    public Instant getRecordedAt() { return recordedAt; }
}
