package com.acme.carrental.rental.infrastructure;

import java.util.UUID;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * PostgreSQL transaction-scoped advisory lock for one customer's idempotency key.
 * Hash collisions only cause harmless extra serialization; correctness is still
 * protected by the unique database constraint.
 */
@Component
public class IdempotencyLock {
    private final JdbcTemplate jdbc;

    public IdempotencyLock(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void lock(UUID customerId, String idempotencyKey) {
        String lockName = customerId + ":" + idempotencyKey;
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            try (var statement = connection.prepareStatement(
                "select pg_advisory_xact_lock(hashtextextended(?, 0))"
            )) {
                statement.setString(1, lockName);
                statement.execute();
                return null;
            }
        });
    }
}
