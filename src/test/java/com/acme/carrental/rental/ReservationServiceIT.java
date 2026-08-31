package com.acme.carrental.rental;

import static org.assertj.core.api.Assertions.*;

import com.acme.carrental.AbstractIntegrationTest;
import com.acme.carrental.fleet.domain.CarType;
import com.acme.carrental.rental.application.ReservationService;
import com.acme.carrental.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class ReservationServiceIT extends AbstractIntegrationTest {
    private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");

    @Autowired ReservationService reservationService;
    @Autowired JdbcTemplate jdbc;
    @Autowired Clock clock;

    private void ensureCustomer(UUID id, String email) {
        jdbc.update("""
            insert into customer(id, email, display_name, created_at)
            values (?, ?, ?, current_timestamp)
            on conflict (id) do nothing
            """, id, email, "IT Customer");
    }

    @Test
    void exposesExactlyTheThreeRequiredCarTypes() {
        assertThat(CarType.values()).containsExactly(CarType.SEDAN, CarType.SUV, CarType.VAN);
    }

    @Test
    void reservesRequestedTypeAtRequestedTimeForRequestedNumberOfDays() {
        ensureCustomer(CUSTOMER_ID, "reservation@customer.test");
        Instant start = clock.instant().plusSeconds(3_600);

        var reservation = reservationService.reserve(CUSTOMER_ID, CarType.VAN, start, 4, "van-four-days");

        assertThat(reservation.carType()).isEqualTo(CarType.VAN);
        assertThat(reservation.startDateTime()).isEqualTo(start);
        assertThat(reservation.endDateTime()).isEqualTo(start.plusSeconds(4 * 86_400L));
        assertThat(reservation.numberOfDays()).isEqualTo(4);
    }

    @Test
    void finiteInventoryRejectsAnOverlappingReservationWhenTypeIsFullyBooked() {
        ensureCustomer(CUSTOMER_ID, "first@customer.test");
        UUID otherCustomer = UUID.randomUUID();
        ensureCustomer(otherCustomer, "other@customer.test");
        Instant start = clock.instant().plusSeconds(3_600);

        reservationService.reserve(CUSTOMER_ID, CarType.SUV, start, 3, "first-suv");

        assertThatThrownBy(() ->
            reservationService.reserve(otherCustomer, CarType.SUV, start.plusSeconds(86_400), 1, "other-suv"))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("No SUV car is available");
    }

    @Test
    void sameLimitedCarCanBeReservedForNonOverlappingPeriods() {
        ensureCustomer(CUSTOMER_ID, "first-period@customer.test");
        UUID otherCustomer = UUID.randomUUID();
        ensureCustomer(otherCustomer, "second-period@customer.test");
        Instant start = clock.instant().plusSeconds(3_600);

        var first = reservationService.reserve(CUSTOMER_ID, CarType.SUV, start, 2, "first-period");
        var second = reservationService.reserve(
            otherCustomer, CarType.SUV, start.plusSeconds(2 * 86_400L), 2, "second-period");

        assertThat(second.vehicleId()).isEqualTo(first.vehicleId());
    }

    @Test
    void sameIdempotencyKeyReturnsSameReservation() {
        ensureCustomer(CUSTOMER_ID, "idempotency@customer.test");
        Instant start = clock.instant().plusSeconds(3_600);

        var first = reservationService.reserve(CUSTOMER_ID, CarType.SEDAN, start, 2, "same-key");
        var second = reservationService.reserve(CUSTOMER_ID, CarType.SEDAN, start, 2, "same-key");

        assertThat(second.id()).isEqualTo(first.id());
    }
}
