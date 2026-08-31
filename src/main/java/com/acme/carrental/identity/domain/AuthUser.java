package com.acme.carrental.identity.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_user")
public class AuthUser {
    @Id
    private UUID id;
    @Column(nullable = false, unique = true)
    private String email;
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;
    @Column(name = "subject_ref")
    private UUID subjectRef;
    @Column(nullable = false)
    private boolean enabled;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuthUser() {}

    public AuthUser(UUID id, String email, String passwordHash, UserRole role, UUID subjectRef, boolean enabled, Instant createdAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.subjectRef = subjectRef;
        this.enabled = enabled;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public UserRole getRole() { return role; }
    public UUID getSubjectRef() { return subjectRef; }
    public boolean isEnabled() { return enabled; }
}
