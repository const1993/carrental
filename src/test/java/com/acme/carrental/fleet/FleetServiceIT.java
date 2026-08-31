package com.acme.carrental.fleet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.acme.carrental.AbstractIntegrationTest;
import com.acme.carrental.fleet.application.FleetService;
import com.acme.carrental.fleet.domain.CarType;
import com.acme.carrental.identity.domain.UserRole;
import com.acme.carrental.rental.application.ReservationService;
import com.acme.carrental.shared.error.ApiException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "app.fleet.search-radius-km=5")
class FleetServiceIT extends AbstractIntegrationTest {
    private static final UUID AIRPORT_LOCATION_ID =
        UUID.fromString("00000000-0000-0000-0000-000000000202");
    @Autowired FleetService fleetService;
    @Autowired ReservationService reservationService;
    @Autowired JdbcTemplate jdbc;
    @Autowired Clock clock;

    @Test
    void searchWithoutFiltersReturnsAllAvailableCarsOfAnyType() {
        var cars = fleetService.search(null, null, null);

        assertThat(cars).hasSize(4);
        assertThat(cars).extracting(FleetService.VehicleView::type)
            .contains(CarType.SEDAN, CarType.SUV, CarType.VAN);
    }

    @Test
    void positionSearchReturnsOnlyCarsInsideConfiguredRange() {
        var cars = fleetService.search(
            null, new BigDecimal("52.229700"), new BigDecimal("21.012200"));

        assertThat(cars).hasSize(3);
        assertThat(cars).extracting(FleetService.VehicleView::registrationNumber)
            .doesNotContain("WX-DEMO-2");
    }

    @Test
    void positionSearchWithNoMatchesReturnsEmptyList() {
        assertThat(fleetService.search(
            null, new BigDecimal("53.900600"), new BigDecimal("27.559000"))).isEmpty();
    }

    @Test
    void positionSearchRequiresBothCoordinates() {
        assertThatThrownBy(() -> fleetService.search(null, new BigDecimal("52.0"), null))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("provided together");
    }

    @Test
    void customerSearchExcludesVehicleReservedDuringRequestedRange() {
        UUID customerId = UUID.randomUUID();
        ensureCustomer(customerId);
        Instant reservationStart = clock.instant().plusSeconds(3_600);
        var reservation = reservationService.reserve(
            customerId, CarType.SUV, reservationStart, 2, "search-customer-overlap");

        var cars = fleetService.search(
            null, null, null,
            reservationStart.plusSeconds(60), reservationStart.plusSeconds(120),
            UserRole.CUSTOMER);

        assertThat(cars).extracting(FleetService.VehicleView::id)
            .doesNotContain(reservation.vehicleId());
    }

    @Test
    void adminSearchIncludesOverlappingVehicleMarkedReserved() {
        UUID customerId = UUID.randomUUID();
        ensureCustomer(customerId);
        Instant reservationStart = clock.instant().plusSeconds(3_600);
        var reservation = reservationService.reserve(
            customerId, CarType.SUV, reservationStart, 2, "search-admin-overlap");

        var cars = fleetService.search(
            null, null, null,
            reservationStart, reservationStart.plusSeconds(60),
            UserRole.ADMIN);

        assertThat(cars).filteredOn(car -> car.id().equals(reservation.vehicleId()))
            .singleElement()
            .extracting(FleetService.VehicleView::reserved)
            .isEqualTo(true);
        assertThat(cars).filteredOn(car -> !car.id().equals(reservation.vehicleId()))
            .allMatch(car -> !car.reserved());
    }

    @Test
    void searchRejectsIncompleteOrReversedTimeRange() {
        Instant start = clock.instant().plusSeconds(3_600);

        assertThatThrownBy(() -> fleetService.search(
            null, null, null, start, null, UserRole.CUSTOMER))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("provided together");
        assertThatThrownBy(() -> fleetService.search(
            null, null, null, start, start, UserRole.CUSTOMER))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("must be after");
    }

    private void ensureCustomer(UUID customerId) {
        jdbc.update("""
            insert into customer(id, email, display_name, created_at)
            values (?, ?, ?, current_timestamp)
            """, customerId, customerId + "@customer.test", "Search Customer");
    }

    @Test
    @Transactional
    void creatingVehicleAssignsNearestActiveLocationFromPosition() {
        var created = fleetService.create(new FleetService.CreateVehicleCommand(
            CarType.SUV,
            "Demo", "Positioned Car",
            new BigDecimal("50.0"), new BigDecimal("7.0"),
            "DEMO0000000000001", "DEMO-POS-1",
            new BigDecimal("52.165800"), new BigDecimal("20.967200"),
            BigDecimal.ZERO, new BigDecimal("40.0")
        ));

        assertThat(created.rentalLocationId()).isEqualTo(AIRPORT_LOCATION_ID);
        assertThat(created.rentalLocationName()).isEqualTo("Warsaw Chopin Airport");
        assertThat(created.fuelLiters()).isEqualByComparingTo("40.0");
        assertThat(created.remainingRangeKm()).isEqualByComparingTo("571.4");
    }

    @Test
    void deviceReportsFuelAndBackendCalculatesRemainingRange() {
        UUID vehicleId = UUID.fromString("00000000-0000-0000-0000-000000000501");

        var updated = fleetService.updateFuel(
            vehicleId,
            new BigDecimal("21.00"),
            null,
            new FleetService.Principal(UserRole.VEHICLE_DEVICE, vehicleId)
        );

        assertThat(updated.fuelLiters()).isEqualByComparingTo("21.00");
        assertThat(updated.remainingRangeKm()).isEqualByComparingTo("403.8");
    }
}
