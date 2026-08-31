package com.acme.carrental.identity.application;

import com.acme.carrental.config.AppProperties;
import com.acme.carrental.identity.domain.*;
import com.acme.carrental.identity.infrastructure.*;
import com.acme.carrental.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final AuthUserRepository users;
    private final CustomerRepository customers;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final AppProperties properties;
    private final Clock clock;

    public AuthService(
        AuthUserRepository users,
        CustomerRepository customers,
        PasswordEncoder passwordEncoder,
        JwtEncoder jwtEncoder,
        AppProperties properties,
        Clock clock
    ) {
        this.users = users;
        this.customers = customers;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public TokenResponse registerCustomer(String email, String password, String displayName) {
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("EMAIL_ALREADY_EXISTS", "A user with this email already exists");
        }
        Instant now = clock.instant();
        UUID customerId = UUID.randomUUID();
        customers.save(new Customer(customerId, email.toLowerCase(), displayName, now));
        AuthUser user = users.save(new AuthUser(
            UUID.randomUUID(),
            email.toLowerCase(),
            passwordEncoder.encode(password),
            UserRole.CUSTOMER,
            customerId,
            true,
            now
        ));
        return issueToken(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(String email, String password) {
        AuthUser user = users.findByEmailIgnoreCase(email)
            .filter(AuthUser::isEnabled)
            .orElseThrow(() -> ApiException.forbidden("INVALID_CREDENTIALS", "Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw ApiException.forbidden("INVALID_CREDENTIALS", "Invalid credentials");
        }
        return issueToken(user);
    }

    private TokenResponse issueToken(AuthUser user) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(properties.getSecurity().getAccessTokenTtl());

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
            .issuer("car-rental-backend")
            .issuedAt(now)
            .expiresAt(expiresAt)
            .subject(user.getId().toString())
            .claim("email", user.getEmail())
            .claim("role", user.getRole().name());

        if (user.getSubjectRef() != null) {
            claims.claim("subject_ref", user.getSubjectRef().toString());
        }

        String token = jwtEncoder.encode(JwtEncoderParameters.from(claims.build())).getTokenValue();
        return new TokenResponse(token, "Bearer", expiresAt, user.getRole(), user.getSubjectRef());
    }

    public record TokenResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        UserRole role,
        UUID subjectRef
    ) {}
}
