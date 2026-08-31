package com.acme.carrental.rental.application;

import com.acme.carrental.config.AppProperties;
import com.acme.carrental.fleet.domain.Vehicle;
import com.acme.carrental.fleet.domain.CarType;
import com.acme.carrental.fleet.domain.VehicleStatus;
import com.acme.carrental.fleet.infrastructure.VehicleRepository;
import com.acme.carrental.rental.domain.*;
import com.acme.carrental.rental.infrastructure.*;
import com.acme.carrental.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationService {
    private final VehicleRepository vehicles;
    private final ReservationRepository reservations;
    private final IdempotencyLock idempotencyLock;
    private final AppProperties properties;
    private final Clock clock;

    public ReservationService(
        VehicleRepository vehicles,
        ReservationRepository reservations,
        IdempotencyLock idempotencyLock,
        AppProperties properties,
        Clock clock
    ) {
        this.vehicles = vehicles;
        this.reservations = reservations;
        this.idempotencyLock = idempotencyLock;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public ReservationView reserve(
        UUID customerId,
        CarType carType,
        Instant startAt,
        int numberOfDays,
        String idempotencyKey
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
            throw ApiException.badRequest(
                "INVALID_IDEMPOTENCY_KEY",
                "Idempotency-Key header is required and must be at most 100 characters"
            );
        }
        if (carType == null || startAt == null) {
            throw ApiException.badRequest("INVALID_RESERVATION", "Car type and start date/time are required");
        }
        if (numberOfDays < 1 || numberOfDays > 365) {
            throw ApiException.badRequest("INVALID_RENTAL_DURATION", "Number of days must be between 1 and 365");
        }

        Instant now = clock.instant();
        if (startAt.isBefore(now.minusSeconds(1))) {
            throw ApiException.badRequest("INVALID_START_TIME", "Reservation start date/time cannot be in the past");
        }
        Instant endAt = startAt.plusSeconds(Math.multiplyExact(numberOfDays, 86_400L));

        idempotencyLock.lock(customerId, idempotencyKey);

        var previous = reservations.findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey);
        if (previous.isPresent()) {
            ensureSameRequest(previous.get(), carType, startAt, numberOfDays);
            return view(previous.get());
        }

        Vehicle vehicle = allocateVehicle(carType, startAt, endAt);

        Reservation reservation = reservations.save(new Reservation(
            UUID.randomUUID(),
            properties.getCompanyId(),
            vehicle,
            customerId,
            idempotencyKey,
            now,
            startAt,
            endAt,
            numberOfDays
        ));
        return view(reservation);
    }

    @Transactional
    public ReservationView cancel(UUID customerId, UUID reservationId) {
        Reservation reservation = reservations.findByIdForUpdate(reservationId)
            .orElseThrow(() -> ApiException.notFound("RESERVATION_NOT_FOUND", "Reservation not found"));

        if (!reservation.getCustomerId().equals(customerId)) {
            throw ApiException.forbidden("NOT_RESERVATION_OWNER", "Reservation belongs to another customer");
        }
        if (reservation.getStatus() != ReservationStatus.HELD) {
            throw ApiException.conflict("RESERVATION_NOT_CANCELLABLE", "Only a held reservation can be cancelled");
        }

        reservation.cancel();
        return view(reservation);
    }

    private Vehicle allocateVehicle(CarType carType, Instant startAt, Instant endAt) {
        for (Vehicle candidate : vehicles.findReservableByType(properties.getCompanyId(), carType.name())) {
            Vehicle locked = vehicles.findByIdForUpdate(candidate.getId()).orElse(null);
            if (locked != null
                && locked.getStatus() == VehicleStatus.AVAILABLE
                && !reservations.existsOverlappingReservation(locked.getId(), startAt, endAt)) {
                return locked;
            }
        }
        throw ApiException.conflict(
            "CAR_TYPE_NOT_AVAILABLE",
            "No " + carType + " car is available for the requested period"
        );
    }

    private void ensureSameRequest(
        Reservation reservation,
        CarType requestedType,
        Instant requestedStart,
        int requestedDays
    ) {
        CarType reservedType = CarType.valueOf(reservation.getVehicle().getModel().getType().getCode());
        if (reservedType != requestedType
            || !reservation.getStartAt().equals(requestedStart)
            || reservation.getNumberOfDays() != requestedDays) {
            throw ApiException.conflict(
                "IDEMPOTENCY_KEY_REUSED",
                "The Idempotency-Key was already used for a different reservation request"
            );
        }
    }

    public ReservationView view(Reservation reservation) {
        return new ReservationView(
            reservation.getId(),
            reservation.getVehicle().getId(),
            CarType.valueOf(reservation.getVehicle().getModel().getType().getCode()),
            reservation.getStatus(),
            reservation.getCreatedAt(),
            reservation.getStartAt(),
            reservation.getEndAt(),
            reservation.getNumberOfDays()
        );
    }

    public record ReservationView(
        UUID id,
        UUID vehicleId,
        CarType carType,
        ReservationStatus status,
        Instant createdAt,
        Instant startDateTime,
        Instant endDateTime,
        int numberOfDays
    ) {}
}
