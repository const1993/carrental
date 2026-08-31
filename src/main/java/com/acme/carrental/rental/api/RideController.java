package com.acme.carrental.rental.api;

import com.acme.carrental.identity.application.CurrentPrincipal;
import com.acme.carrental.rental.application.RideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rides")
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "Rides")
@SecurityRequirement(name = "bearerAuth")
public class RideController {
    private final RideService rideService;
    private final CurrentPrincipal currentPrincipal;

    public RideController(RideService rideService, CurrentPrincipal currentPrincipal) {
        this.rideService = rideService;
        this.currentPrincipal = currentPrincipal;
    }

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Start a ride when its reserved period begins")
    RideService.RideView start(
        @Valid @RequestBody StartRideRequest request,
        @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt
    ) {
        return rideService.start(
            currentPrincipal.from(jwt).requireCustomerId(),
            request.reservationId()
        );
    }

    @PostMapping("/{rideId}/finish")
    @Operation(summary = "Finish a ride and atomically record the calculated payment")
    RideService.RideView finish(
        @PathVariable UUID rideId,
        @Valid @RequestBody FinishRideRequest request,
        @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt
    ) {
        return rideService.finish(
            currentPrincipal.from(jwt).requireCustomerId(),
            rideId,
            request.returnLocationId(),
            request.paymentReference()
        );
    }

    @GetMapping("/{rideId}")
    @Operation(summary = "Get ride state")
    RideService.RideView get(
        @PathVariable UUID rideId,
        @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt
    ) {
        return rideService.get(
            currentPrincipal.from(jwt).requireCustomerId(),
            rideId
        );
    }

    public record StartRideRequest(@NotNull UUID reservationId) {}
    public record FinishRideRequest(
        @NotNull UUID returnLocationId,
        @Size(max = 100) String paymentReference
    ) {}
}
