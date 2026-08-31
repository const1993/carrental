package com.acme.carrental.config;

import com.acme.carrental.identity.domain.*;
import com.acme.carrental.identity.infrastructure.*;
import java.time.Clock;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LocalDataSeeder implements ApplicationRunner {
    public static final UUID DEMO_CUSTOMER_ID =
        UUID.fromString("00000000-0000-0000-0000-000000000101");
    public static final UUID DEMO_VEHICLE_ID =
        UUID.fromString("00000000-0000-0000-0000-000000000501");

    private final AppProperties properties;
    private final AuthUserRepository users;
    private final CustomerRepository customers;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public LocalDataSeeder(
        AppProperties properties,
        AuthUserRepository users,
        CustomerRepository customers,
        PasswordEncoder passwordEncoder,
        Clock clock
    ) {
        this.properties = properties;
        this.users = users;
        this.customers = customers;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.getSeed().isEnabled()) {
            return;
        }

        createUserIfMissing("admin@local.test", "admin12345", UserRole.ADMIN, null);

        customers.findById(DEMO_CUSTOMER_ID).orElseGet(() ->
            customers.save(new Customer(
                DEMO_CUSTOMER_ID,
                "customer@local.test",
                "Demo Customer",
                clock.instant()
            ))
        );

        createUserIfMissing(
            "customer@local.test",
            "customer12345",
            UserRole.CUSTOMER,
            DEMO_CUSTOMER_ID
        );

        createUserIfMissing(
            "vehicle@local.test",
            "vehicle12345",
            UserRole.VEHICLE_DEVICE,
            DEMO_VEHICLE_ID
        );
    }

    private void createUserIfMissing(String email, String password, UserRole role, UUID subjectRef) {
        users.findByEmailIgnoreCase(email).orElseGet(() ->
            users.save(new AuthUser(
                UUID.randomUUID(),
                email,
                passwordEncoder.encode(password),
                role,
                subjectRef,
                true,
                clock.instant()
            ))
        );
    }
}
