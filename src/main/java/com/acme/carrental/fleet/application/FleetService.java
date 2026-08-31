package com.acme.carrental.fleet.application;

import com.acme.carrental.config.AppProperties;
import com.acme.carrental.fleet.domain.*;
import com.acme.carrental.fleet.infrastructure.*;
import com.acme.carrental.identity.domain.UserRole;
import com.acme.carrental.rental.infrastructure.ReservationRepository;
import com.acme.carrental.shared.error.ApiException;
import com.acme.carrental.shared.domain.Currency;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FleetService {
    private final VehicleRepository vehicles;
    private final VehicleTypeRepository types;
    private final VehicleModelRepository models;
    private final RentalLocationRepository locations;
    private final ReservationRepository reservations;
    private final AppProperties properties;
    private final Clock clock;

    public FleetService(
        VehicleRepository vehicles,
        VehicleTypeRepository types,
        VehicleModelRepository models,
        RentalLocationRepository locations,
        ReservationRepository reservations,
        AppProperties properties,
        Clock clock
    ) {
        this.vehicles = vehicles;
        this.types = types;
        this.models = models;
        this.locations = locations;
        this.reservations = reservations;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<VehicleView> search(CarType type, BigDecimal latitude, BigDecimal longitude) {
        Instant now = clock.instant();
        return search(type, latitude, longitude, now, now.plusNanos(1), UserRole.CUSTOMER);
    }

    @Transactional(readOnly = true)
    public List<VehicleView> search(
        CarType type,
        BigDecimal latitude,
        BigDecimal longitude,
        Instant startDateTime,
        Instant endDateTime,
        UserRole role
    ) {
        if ((latitude == null) != (longitude == null)) {
            throw ApiException.badRequest(
                "INCOMPLETE_POSITION",
                "Latitude and longitude must be provided together"
            );
        }
        if ((startDateTime == null) != (endDateTime == null)) {
            throw ApiException.badRequest(
                "INCOMPLETE_TIME_RANGE",
                "Start and end date/time must be provided together"
            );
        }
        if (startDateTime != null && !endDateTime.isAfter(startDateTime)) {
            throw ApiException.badRequest(
                "INVALID_TIME_RANGE",
                "End date/time must be after start date/time"
            );
        }

        Instant rangeStart = startDateTime;
        Instant rangeEnd = endDateTime;
        if (rangeStart == null) {
            rangeStart = clock.instant();
            rangeEnd = rangeStart.plusNanos(1);
        }

        List<Vehicle> candidates = vehicles.searchCandidates(
            properties.getCompanyId(), type == null ? null : type.name());
        Set<UUID> reservedVehicleIds = candidates.isEmpty()
            ? Set.of()
            : reservations.findVehicleIdsReservedDuring(
                candidates.stream().map(Vehicle::getId).toList(), rangeStart, rangeEnd);

        boolean admin = role == UserRole.ADMIN;
        return candidates.stream()
            .filter(vehicle -> latitude == null || isWithinSearchRadius(vehicle, latitude, longitude))
            .filter(vehicle -> admin || !reservedVehicleIds.contains(vehicle.getId()))
            .map(vehicle -> view(vehicle, reservedVehicleIds.contains(vehicle.getId())))
            .toList();
    }

    private boolean isWithinSearchRadius(Vehicle vehicle, BigDecimal latitude, BigDecimal longitude) {
        double earthRadiusKm = 6_371.0088;
        double latitude1 = Math.toRadians(latitude.doubleValue());
        double latitude2 = Math.toRadians(vehicle.getLatitude().doubleValue());
        double latitudeDelta = latitude2 - latitude1;
        double longitudeDelta = Math.toRadians(vehicle.getLongitude().doubleValue() - longitude.doubleValue());
        double haversine = Math.pow(Math.sin(latitudeDelta / 2), 2)
            + Math.cos(latitude1) * Math.cos(latitude2) * Math.pow(Math.sin(longitudeDelta / 2), 2);
        double distanceKm = 2 * earthRadiusKm * Math.asin(Math.sqrt(haversine));
        return distanceKm <= properties.getFleet().getSearchRadiusKm().doubleValue();
    }

    @Transactional(readOnly = true)
    public VehicleView getState(UUID id) {
        Vehicle vehicle = vehicles.findById(id)
            .orElseThrow(() -> ApiException.notFound("VEHICLE_NOT_FOUND", "Vehicle not found"));
        return view(vehicle);
    }

    @Transactional
    public VehicleView create(CreateVehicleCommand command) {
        VehicleType type = types.findByCompanyIdAndCodeIgnoreCase(properties.getCompanyId(), command.type().name())
            .orElseThrow(() -> ApiException.badRequest("VEHICLE_TYPE_NOT_FOUND", "Unknown vehicle type: " + command.type()));

        RentalLocation location = nearestActiveLocation(command.latitude(), command.longitude());

        VehicleModel model = models.findByCompanyIdAndType_IdAndMakeIgnoreCaseAndModelIgnoreCase(
                properties.getCompanyId(), type.getId(), command.make(), command.model())
            .orElseGet(() -> models.save(new VehicleModel(
                UUID.randomUUID(),
                properties.getCompanyId(),
                type,
                command.make(),
                command.model(),
                command.tankCapacityLiters(),
                command.consumptionLitersPer100Km()
            )));

        if (command.fuelLiters().compareTo(model.getTankCapacityLiters()) > 0) {
            throw ApiException.badRequest(
                "INVALID_FUEL_LEVEL",
                "Fuel remaining cannot exceed the vehicle model's tank capacity"
            );
        }

        Vehicle vehicle = new Vehicle(
            UUID.randomUUID(),
            properties.getCompanyId(),
            model,
            location,
            command.vin(),
            command.registrationNumber(),
            command.latitude(),
            command.longitude(),
            command.odometerKm(),
            command.fuelLiters(),
            clock.instant()
        );
        return view(vehicles.save(vehicle));
    }

    private RentalLocation nearestActiveLocation(BigDecimal latitude, BigDecimal longitude) {
        return locations.findByCompanyIdAndActiveTrue(properties.getCompanyId()).stream()
            .min(java.util.Comparator.comparingDouble(location ->
                distanceSquared(latitude, longitude, location.getLatitude(), location.getLongitude())))
            .orElseThrow(() -> ApiException.conflict(
                "NO_ACTIVE_RENTAL_LOCATION",
                "No active rental location is available for the vehicle position"
            ));
    }

    private static double distanceSquared(
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal locationLatitude,
        BigDecimal locationLongitude
    ) {
        double latitudeDelta = latitude.doubleValue() - locationLatitude.doubleValue();
        double longitudeDelta = longitude.doubleValue() - locationLongitude.doubleValue();
        double longitudeScale = Math.cos(Math.toRadians(latitude.doubleValue()));
        return latitudeDelta * latitudeDelta
            + longitudeDelta * longitudeDelta * longitudeScale * longitudeScale;
    }

    @Transactional
    public void retire(UUID id) {
        Vehicle vehicle = vehicles.findByIdForUpdate(id)
            .orElseThrow(() -> ApiException.notFound("VEHICLE_NOT_FOUND", "Vehicle not found"));
        if (vehicle.getStatus() == VehicleStatus.IN_RIDE) {
            throw ApiException.conflict("VEHICLE_IN_RIDE", "An active ride must finish before the vehicle can be retired");
        }
        vehicle.retire(clock.instant());
    }

    @Transactional
    public VehicleView updatePosition(UUID id, BigDecimal latitude, BigDecimal longitude, Principal principal) {
        Vehicle vehicle = authorizedDeviceVehicle(id, principal);
        vehicle.changePosition(latitude, longitude, clock.instant());
        return view(vehicle);
    }

    @Transactional
    public VehicleView updateFuel(UUID id, BigDecimal fuelLiters, BigDecimal odometerKm, Principal principal) {
        Vehicle vehicle = authorizedDeviceVehicle(id, principal);
        try {
            vehicle.updateFuel(
                fuelLiters,
                odometerKm,
                clock.instant()
            );
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest("INVALID_VEHICLE_TELEMETRY", ex.getMessage());
        }
        return view(vehicle);
    }

    private Vehicle authorizedDeviceVehicle(UUID id, Principal principal) {
        if (principal.role() != UserRole.ADMIN) {
            if (principal.role() != UserRole.VEHICLE_DEVICE || !id.equals(principal.subjectRef())) {
                throw ApiException.forbidden(
                    "VEHICLE_DEVICE_MISMATCH",
                    "Vehicle device may update only its assigned vehicle"
                );
            }
        }
        return vehicles.findByIdForUpdate(id)
            .orElseThrow(() -> ApiException.notFound("VEHICLE_NOT_FOUND", "Vehicle not found"));
    }

    private VehicleView view(Vehicle vehicle) {
        return view(vehicle, false);
    }

    private VehicleView view(Vehicle vehicle, boolean reserved) {
        return new VehicleView(
            vehicle.getId(),
            CarType.valueOf(vehicle.getModel().getType().getCode()),
            vehicle.getModel().getMake(),
            vehicle.getModel().getModel(),
            vehicle.getRegistrationNumber(),
            vehicle.getStatus(),
            vehicle.getRentalLocation().getId(),
            vehicle.getRentalLocation().getName(),
            vehicle.getLatitude(),
            vehicle.getLongitude(),
            vehicle.getOdometerKm(),
            vehicle.getFuelLiters(),
            vehicle.getRemainingRangeKm(),
            vehicle.getModel().getType().getDailyRate(),
            vehicle.getModel().getType().getCurrency(),
            vehicle.getUpdatedAt(),
            reserved
        );
    }

    public record Principal(UserRole role, UUID subjectRef) {}

    public record CreateVehicleCommand(
        CarType type,
        String make,
        String model,
        BigDecimal tankCapacityLiters,
        BigDecimal consumptionLitersPer100Km,
        String vin,
        String registrationNumber,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal odometerKm,
        BigDecimal fuelLiters
    ) {}

    public record VehicleView(
        UUID id,
        CarType type,
        String make,
        String model,
        String registrationNumber,
        VehicleStatus status,
        UUID rentalLocationId,
        String rentalLocationName,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal odometerKm,
        BigDecimal fuelLiters,
        BigDecimal remainingRangeKm,
        BigDecimal dailyRate,
        Currency currency,
        Instant updatedAt,
        boolean reserved
    ) {}
}
