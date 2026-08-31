package com.acme.carrental.rental.application;

import com.acme.carrental.config.AppProperties;
import com.acme.carrental.fleet.domain.*;
import com.acme.carrental.fleet.infrastructure.RentalLocationRepository;
import com.acme.carrental.fleet.infrastructure.VehicleRepository;
import com.acme.carrental.payment.domain.Payment;
import com.acme.carrental.payment.infrastructure.PaymentRepository;
import com.acme.carrental.rental.domain.*;
import com.acme.carrental.rental.infrastructure.*;
import com.acme.carrental.shared.error.ApiException;
import com.acme.carrental.shared.domain.Currency;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RideService {
    private final ReservationRepository reservations;
    private final RideRepository rides;
    private final VehicleRepository vehicles;
    private final RentalLocationRepository locations;
    private final PaymentRepository payments;
    private final AppProperties properties;
    private final Clock clock;

    public RideService(
        ReservationRepository reservations,
        RideRepository rides,
        VehicleRepository vehicles,
        RentalLocationRepository locations,
        PaymentRepository payments,
        AppProperties properties,
        Clock clock
    ) {
        this.reservations = reservations;
        this.rides = rides;
        this.vehicles = vehicles;
        this.locations = locations;
        this.payments = payments;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public RideView start(UUID customerId, UUID reservationId) {
        Reservation reservation = reservations.findByIdForUpdate(reservationId)
            .orElseThrow(() -> ApiException.notFound("RESERVATION_NOT_FOUND", "Reservation not found"));

        if (!reservation.getCustomerId().equals(customerId)) {
            throw ApiException.forbidden("NOT_RESERVATION_OWNER", "Reservation belongs to another customer");
        }

        Instant now = clock.instant();
        if (reservation.getStatus() != ReservationStatus.HELD) {
            throw ApiException.conflict("RESERVATION_NOT_HELD", "Reservation is not in HELD state");
        }
        if (reservation.isExpiredAt(now)) {
            throw ApiException.conflict("RESERVATION_EXPIRED", "Reservation period has ended");
        }
        if (now.isBefore(reservation.getStartAt().minusSeconds(1))) {
            throw ApiException.conflict("RESERVATION_NOT_STARTED", "Reservation start time has not arrived");
        }

        Vehicle vehicle = vehicles.findByIdForUpdate(reservation.getVehicle().getId())
            .orElseThrow(() -> ApiException.notFound("VEHICLE_NOT_FOUND", "Vehicle not found"));

        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            throw ApiException.conflict("VEHICLE_NOT_AVAILABLE", "Vehicle is not available to start a ride");
        }

        reservation.convert();
        vehicle.startRide(now);

        Ride ride = rides.save(new Ride(
            UUID.randomUUID(),
            properties.getCompanyId(),
            reservation,
            vehicle,
            customerId,
            now
        ));
        return view(ride, null);
    }

    @Transactional
    public RideView finish(UUID customerId, UUID rideId, UUID returnLocationId, String paymentReference) {
        Ride ride = rides.findByIdForUpdate(rideId)
            .orElseThrow(() -> ApiException.notFound("RIDE_NOT_FOUND", "Ride not found"));

        if (!ride.getCustomerId().equals(customerId)) {
            throw ApiException.forbidden("NOT_RIDE_OWNER", "Ride belongs to another customer");
        }
        if (ride.getStatus() != RideStatus.ACTIVE) {
            throw ApiException.conflict("RIDE_NOT_ACTIVE", "Only an active ride can be finished");
        }

        Vehicle vehicle = vehicles.findByIdForUpdate(ride.getVehicle().getId())
            .orElseThrow(() -> ApiException.notFound("VEHICLE_NOT_FOUND", "Vehicle not found"));

        RentalLocation returnLocation = locations.findById(returnLocationId)
            .filter(location ->
                location.getCompanyId().equals(properties.getCompanyId()) && location.isActive())
            .orElseThrow(() ->
                ApiException.badRequest("RETURN_LOCATION_NOT_FOUND", "Return location not found or inactive"));

        Instant now = clock.instant();
        Charge charge = chargeFor(ride, vehicle, now);

        ride.finish(now, charge.billedDays(), charge.amount(), charge.currency(), returnLocation);
        vehicle.endRideAt(returnLocation, now);

        return view(ride, recordPayment(ride, customerId, charge, paymentReference, now));
    }

    private Charge chargeFor(Ride ride, Vehicle vehicle, Instant at) {
        long seconds = Math.max(1, Duration.between(ride.getStartedAt(), at).getSeconds());
        int billedDays = (int) Math.max(1, (seconds + 86_399L) / 86_400L);
        BigDecimal dailyRate = vehicle.getModel().getType().getDailyRate();
        Currency currency = vehicle.getModel().getType().getCurrency();
        return new Charge(billedDays, dailyRate.multiply(BigDecimal.valueOf(billedDays)), currency);
    }

    private PaymentView recordPayment(
        Ride ride,
        UUID customerId,
        Charge charge,
        String reference,
        Instant recordedAt
    ) {
        Payment payment = payments.save(new Payment(
            UUID.randomUUID(), properties.getCompanyId(), ride, customerId,
            charge.amount(), charge.currency(), reference, recordedAt
        ));
        return new PaymentView(
            payment.getId(), payment.getAmount(), payment.getCurrency(),
            payment.getStatus().name(), payment.getReference(), payment.getRecordedAt()
        );
    }

    private record Charge(int billedDays, BigDecimal amount, Currency currency) {}

    @Transactional(readOnly = true)
    public RideView get(UUID customerId, UUID rideId) {
        Ride ride = rides.findById(rideId)
            .orElseThrow(() -> ApiException.notFound("RIDE_NOT_FOUND", "Ride not found"));
        if (!ride.getCustomerId().equals(customerId)) {
            throw ApiException.forbidden("NOT_RIDE_OWNER", "Ride belongs to another customer");
        }
        return view(ride, null);
    }

    private RideView view(Ride ride, PaymentView payment) {
        return new RideView(
            ride.getId(),
            ride.getReservation().getId(),
            ride.getVehicle().getId(),
            ride.getStatus(),
            ride.getPickupLocation().getId(),
            ride.getReturnLocation() == null ? null : ride.getReturnLocation().getId(),
            ride.getStartedAt(),
            ride.getFinishedAt(),
            ride.getBilledDays(),
            ride.getFinalAmount(),
            ride.getCurrency(),
            payment
        );
    }

    public record RideView(
        UUID id,
        UUID reservationId,
        UUID vehicleId,
        RideStatus status,
        UUID pickupLocationId,
        UUID returnLocationId,
        Instant startedAt,
        Instant finishedAt,
        Integer billedDays,
        BigDecimal finalAmount,
        Currency currency,
        PaymentView payment
    ) {}

    public record PaymentView(
        UUID id,
        BigDecimal amount,
        Currency currency,
        String status,
        String reference,
        Instant recordedAt
    ) {}
}
