package com.acme.carrental.fleet.api;

import com.acme.carrental.fleet.application.FleetService;
import com.acme.carrental.fleet.domain.CarType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/vehicles")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin vehicles")
@SecurityRequirement(name = "bearerAuth")
public class AdminVehicleController {
    private final FleetService fleetService;

    public AdminVehicleController(FleetService fleetService) {
        this.fleetService = fleetService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a vehicle and assign its nearest active rental location from GPS position")
    FleetService.VehicleView create(@Valid @RequestBody CreateVehicleRequest request) {
        return fleetService.create(new FleetService.CreateVehicleCommand(
            request.type(),
            request.make(),
            request.model(),
            request.tankCapacityLiters(),
            request.consumptionLitersPer100Km(),
            request.vin(),
            request.registrationNumber(),
            request.latitude(),
            request.longitude(),
            request.odometerKm(),
            request.fuelLiters()
        ));
    }

    @DeleteMapping("/{vehicleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Retire a vehicle while preserving history")
    void retire(@PathVariable UUID vehicleId) {
        fleetService.retire(vehicleId);
    }

    public record CreateVehicleRequest(
        @NotNull CarType type,
        @NotBlank String make,
        @NotBlank String model,
        @NotNull @DecimalMin("1.0") BigDecimal tankCapacityLiters,
        @NotNull @DecimalMin("0.1") BigDecimal consumptionLitersPer100Km,
        @NotBlank @Size(min = 17, max = 17) String vin,
        @NotBlank String registrationNumber,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
        @NotNull @DecimalMin("0.0") BigDecimal odometerKm,
        @NotNull @DecimalMin("0.0") BigDecimal fuelLiters
    ) {}
}
