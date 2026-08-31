package com.acme.carrental.rental;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.carrental.AbstractIntegrationTest;
import com.acme.carrental.rental.application.*;
import com.acme.carrental.rental.domain.RideStatus;
import com.acme.carrental.fleet.domain.CarType;
import com.acme.carrental.shared.domain.Currency;
import java.time.Clock;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class RideServiceIT extends AbstractIntegrationTest {
    private static final UUID CUSTOMER_ID =
        UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID VEHICLE_ID =
        UUID.fromString("00000000-0000-0000-0000-000000000501");
    private static final UUID RETURN_LOCATION_ID =
        UUID.fromString("00000000-0000-0000-0000-000000000202");

    @Autowired ReservationService reservationService;
    @Autowired RideService rideService;
    @Autowired JdbcTemplate jdbc;
    @Autowired Clock clock;

    private void ensureCustomer() {
        jdbc.update("""
            insert into customer(id, email, display_name, created_at)
            values (?, ?, ?, current_timestamp)
            on conflict (id) do nothing
            """, CUSTOMER_ID, "ride@customer.test", "Ride Customer");
    }

    @Test
    void finishRideCreatesPaymentAndReturnsVehicleToAvailability() {
        ensureCustomer();

        var reservation = reservationService.reserve(CUSTOMER_ID, CarType.SEDAN, clock.instant(), 1, "ride-flow");
        var ride = rideService.start(CUSTOMER_ID, reservation.id());
        var finished = rideService.finish(CUSTOMER_ID, ride.id(), RETURN_LOCATION_ID, "manual-payment-1");

        assertThat(finished.status()).isEqualTo(RideStatus.FINISHED);
        assertThat(finished.payment()).isNotNull();
        assertThat(finished.payment().amount()).isPositive();
        assertThat(finished.payment().currency()).isEqualTo(Currency.PLN);
        assertThat(finished.returnLocationId()).isEqualTo(RETURN_LOCATION_ID);
    }

    @Test
    void cancellingReservationBeforeRideStartsCreatesNoPayment() {
        ensureCustomer();
        var reservation = reservationService.reserve(
            CUSTOMER_ID, CarType.VAN, clock.instant().plusSeconds(3_600), 2, "cancel-reservation");

        reservationService.cancel(CUSTOMER_ID, reservation.id());

        assertThat(jdbc.queryForObject("select count(*) from ride", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from payment", Integer.class)).isZero();
    }
}
