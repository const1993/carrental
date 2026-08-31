package com.acme.carrental.rental.infrastructure;

import com.acme.carrental.rental.domain.Ride;
import com.acme.carrental.rental.domain.RideStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface RideRepository extends JpaRepository<Ride, UUID> {
    boolean existsByVehicle_IdAndStatus(UUID vehicleId, RideStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"vehicle", "vehicle.model", "vehicle.model.type", "reservation", "pickupLocation", "returnLocation"})
    @Query("select r from Ride r where r.id = :id")
    Optional<Ride> findByIdForUpdate(@Param("id") UUID id);
}
