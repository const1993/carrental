package com.acme.carrental.fleet.api;

import com.acme.carrental.fleet.application.FleetService;
import com.acme.carrental.fleet.domain.CarType;
import com.acme.carrental.identity.application.CurrentPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/api/v1/vehicles")
@Tag(name = "Vehicles")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class VehicleController {
    private final FleetService fleetService;
    private final CurrentPrincipal currentPrincipal;

    public VehicleController(FleetService fleetService, CurrentPrincipal currentPrincipal) {
        this.fleetService = fleetService;
        this.currentPrincipal = currentPrincipal;
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    @Operation(summary = "Find currently available cars, optionally by enum type and branch")
    List<FleetService.VehicleView> search(
        @RequestParam(required = false) CarType type,
        @RequestParam(required = false) @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @RequestParam(required = false) @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
        @RequestParam(required = false) Instant startDateTime,
        @RequestParam(required = false) Instant endDateTime,
        @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt
    ) {
        return fleetService.search(
            type, latitude, longitude, startDateTime, endDateTime, currentPrincipal.from(jwt).role());
    }

    @GetMapping("/{vehicleId}/state")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','VEHICLE_DEVICE')")
    @Operation(summary = "Get current car state")
    FleetService.VehicleView state(@PathVariable UUID vehicleId) {
        return fleetService.getState(vehicleId);
    }

    @PatchMapping("/{vehicleId}/position")
    @PreAuthorize("hasAnyRole('VEHICLE_DEVICE','ADMIN')")
    @Operation(summary = "Update vehicle GPS position")
    FleetService.VehicleView updatePosition(
        @PathVariable UUID vehicleId,
        @Valid @RequestBody PositionRequest request,
        @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt
    ) {
        var principal = currentPrincipal.from(jwt);
        return fleetService.updatePosition(
            vehicleId,
            request.latitude(),
            request.longitude(),
            new FleetService.Principal(principal.role(), principal.subjectRef())
        );
    }

    @PatchMapping("/{vehicleId}/fuel")
    @PreAuthorize("hasAnyRole('VEHICLE_DEVICE','ADMIN')")
    @Operation(summary = "Report fuel remaining; backend calculates estimated driving range")
    FleetService.VehicleView updateFuel(
        @PathVariable UUID vehicleId,
        @Valid @RequestBody FuelRequest request,
        @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt
    ) {
        var principal = currentPrincipal.from(jwt);
        return fleetService.updateFuel(
            vehicleId,
            request.fuelLiters(),
            request.odometerKm(),
            new FleetService.Principal(principal.role(), principal.subjectRef())
        );
    }

    public record PositionRequest(
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude
    ) {}

    public record FuelRequest(
        @NotNull @DecimalMin("0.0") BigDecimal fuelLiters,
        @DecimalMin("0.0") BigDecimal odometerKm
    ) {}
}
