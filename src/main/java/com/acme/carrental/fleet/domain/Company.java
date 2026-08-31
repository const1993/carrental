package com.acme.carrental.fleet.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "company")
public class Company {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    protected Company() {}

    public UUID getId() { return id; }
    public String getName() { return name; }
}
