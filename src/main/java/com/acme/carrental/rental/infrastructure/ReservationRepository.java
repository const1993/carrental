package com.acme.carrental.rental.infrastructure;

import com.acme.carrental.rental.domain.Reservation;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    Optional<Reservation> findByCustomerIdAndIdempotencyKey(UUID customerId, String idempotencyKey);

    @Query("""
        select count(r) > 0 from Reservation r
        where r.vehicle.id = :vehicleId
          and r.status in (com.acme.carrental.rental.domain.ReservationStatus.HELD,
                           com.acme.carrental.rental.domain.ReservationStatus.CONVERTED)
          and r.startAt < :endAt
          and r.endAt > :startAt
        """)
    boolean existsOverlappingReservation(
        @Param("vehicleId") UUID vehicleId,
        @Param("startAt") Instant startAt,
        @Param("endAt") Instant endAt
    );

    @Query("""
        select distinct r.vehicle.id from Reservation r
        where r.vehicle.id in :vehicleIds
          and r.status in (com.acme.carrental.rental.domain.ReservationStatus.HELD,
                           com.acme.carrental.rental.domain.ReservationStatus.CONVERTED)
          and r.startAt < :endAt
          and r.endAt > :startAt
        """)
    Set<UUID> findVehicleIdsReservedDuring(
        @Param("vehicleIds") java.util.Collection<UUID> vehicleIds,
        @Param("startAt") Instant startAt,
        @Param("endAt") Instant endAt
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"vehicle", "vehicle.model", "vehicle.model.type"})
    @Query("select r from Reservation r where r.id = :id")
    Optional<Reservation> findByIdForUpdate(@Param("id") UUID id);

    @Modifying
    @Query("""
        update Reservation r
           set r.status = com.acme.carrental.rental.domain.ReservationStatus.EXPIRED
         where r.status = com.acme.carrental.rental.domain.ReservationStatus.HELD
           and r.endAt <= :now
        """)
    int expireStaleHolds(@Param("now") Instant now);
}
