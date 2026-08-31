package com.acme.carrental.identity.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customer")
public class Customer {
    @Id
    private UUID id;
    @Column(nullable = false)
    private String email;
    @Column(name = "display_name", nullable = false)
    private String displayName;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Customer() {}

    public Customer(UUID id, String email, String displayName, Instant createdAt) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
}
