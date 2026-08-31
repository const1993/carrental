package com.acme.carrental.rental.api;

import com.acme.carrental.identity.application.CurrentPrincipal;
import com.acme.carrental.fleet.domain.CarType;
import com.acme.carrental.rental.application.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reservations")
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "Reservations")
@SecurityRequirement(name = "bearerAuth")
public class ReservationController {
    private final ReservationService reservationService;
    private final CurrentPrincipal currentPrincipal;

    public ReservationController(ReservationService reservationService, CurrentPrincipal currentPrincipal) {
        this.reservationService = reservationService;
        this.currentPrincipal = currentPrincipal;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Reserve an available car of a requested type for a date/time and number of days")
    ReservationService.ReservationView reserve(
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @Valid @RequestBody ReserveRequest request,
        @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt
    ) {
        return reservationService.reserve(
            currentPrincipal.from(jwt).requireCustomerId(),
            request.carType(),
            request.startDateTime(),
            request.numberOfDays(),
            idempotencyKey
        );
    }

    @PostMapping("/{reservationId}/cancel")
    @Operation(summary = "Cancel an active scheduled reservation")
    ReservationService.ReservationView cancel(
        @PathVariable UUID reservationId,
        @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt
    ) {
        return reservationService.cancel(
            currentPrincipal.from(jwt).requireCustomerId(),
            reservationId
        );
    }

    public record ReserveRequest(
        @NotNull CarType carType,
        @NotNull Instant startDateTime,
        @Min(1) @Max(365) int numberOfDays
    ) {}
}
