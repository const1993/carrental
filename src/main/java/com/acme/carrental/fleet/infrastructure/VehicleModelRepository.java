package com.acme.carrental.fleet.infrastructure;

import com.acme.carrental.fleet.domain.VehicleModel;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleModelRepository extends JpaRepository<VehicleModel, UUID> {
    Optional<VehicleModel> findByCompanyIdAndType_IdAndMakeIgnoreCaseAndModelIgnoreCase(
        UUID companyId, UUID typeId, String make, String model);
}
