package com.acme.carrental.identity.infrastructure;

import com.acme.carrental.identity.domain.AuthUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthUserRepository extends JpaRepository<AuthUser, UUID> {
    Optional<AuthUser> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
}
