package com.acme.carrental.fleet.infrastructure;

import com.acme.carrental.fleet.domain.VehicleType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleTypeRepository extends JpaRepository<VehicleType, UUID> {
    Optional<VehicleType> findByCompanyIdAndCodeIgnoreCase(UUID companyId, String code);
}
