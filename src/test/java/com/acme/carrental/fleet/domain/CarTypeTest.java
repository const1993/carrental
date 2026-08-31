package com.acme.carrental.fleet.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CarTypeTest {
    @Test
    void supportsExactlySedanSuvAndVan() {
        assertThat(CarType.values()).containsExactly(CarType.SEDAN, CarType.SUV, CarType.VAN);
    }
}
