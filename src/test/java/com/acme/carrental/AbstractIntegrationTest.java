package com.acme.carrental;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(properties = {
    "app.seed.enabled=false",
    "app.security.jwt-secret=test-secret-test-secret-test-secret-123456"
})
public abstract class AbstractIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
        new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    JdbcTemplate cleanupJdbc;

    @BeforeEach
    void cleanMutableData() {
        cleanupJdbc.update("delete from payment");
        cleanupJdbc.update("delete from ride");
        cleanupJdbc.update("delete from reservation");
        cleanupJdbc.update("delete from auth_user");
        cleanupJdbc.update("delete from customer");
        cleanupJdbc.update("""
            update vehicle
               set status = 'AVAILABLE',
                   rental_location_id = '00000000-0000-0000-0000-000000000201',
                   latitude = 52.229700,
                   longitude = 21.012200,
                   version = version + 1
             where id = '00000000-0000-0000-0000-000000000501'
            """);
        cleanupJdbc.update("""
            update vehicle
               set status = 'AVAILABLE',
                   rental_location_id = '00000000-0000-0000-0000-000000000202',
                   latitude = 52.165700,
                   longitude = 20.967100,
                   version = version + 1
             where id = '00000000-0000-0000-0000-000000000502'
            """);
    }
}
