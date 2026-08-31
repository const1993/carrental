package com.acme.carrental.identity.infrastructure;

import com.acme.carrental.identity.domain.Customer;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
}
