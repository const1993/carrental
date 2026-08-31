package com.acme.carrental.fleet.infrastructure;

import com.acme.carrental.fleet.domain.Vehicle;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vehicle v where v.id = :id")
    Optional<Vehicle> findByIdForUpdate(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"model", "model.type", "rentalLocation"})
    @Query("""
        select v from Vehicle v
        where v.companyId = :companyId
          and v.status = com.acme.carrental.fleet.domain.VehicleStatus.AVAILABLE
          and v.model.type.code = :typeCode
        order by v.registrationNumber
        """)
    List<Vehicle> findReservableByType(
        @Param("companyId") UUID companyId,
        @Param("typeCode") String typeCode
    );

    @EntityGraph(attributePaths = {"model", "model.type", "rentalLocation"})
    @Query("""
        select v from Vehicle v
        where v.companyId = :companyId
          and v.status = com.acme.carrental.fleet.domain.VehicleStatus.AVAILABLE
          and (:typeCode is null or v.model.type.code = :typeCode)
        order by v.model.type.code, v.registrationNumber
        """)
    List<Vehicle> searchCandidates(
        @Param("companyId") UUID companyId,
        @Param("typeCode") String typeCode
    );
}
