package com.acme.carrental.identity.api;

import com.acme.carrental.identity.application.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a customer and return a JWT")
    AuthService.TokenResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.registerCustomer(request.email(), request.password(), request.displayName());
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate and return a JWT")
    AuthService.TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.email(), request.password());
    }

    public record RegisterRequest(
        @Email @NotBlank String email,
        @Size(min = 8, max = 100) String password,
        @NotBlank @Size(max = 100) String displayName
    ) {}

    public record LoginRequest(
        @Email @NotBlank String email,
        @NotBlank String password
    ) {}
}
