package com.acme.carrental.fleet.infrastructure;

import com.acme.carrental.fleet.domain.RentalLocation;
import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RentalLocationRepository extends JpaRepository<RentalLocation, UUID> {
    List<RentalLocation> findByCompanyIdAndActiveTrue(UUID companyId);
}
