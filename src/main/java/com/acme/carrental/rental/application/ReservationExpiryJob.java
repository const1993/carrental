package com.acme.carrental.rental.application;

import com.acme.carrental.rental.infrastructure.ReservationRepository;
import java.time.Clock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ReservationExpiryJob {
    private final ReservationRepository reservations;
    private final Clock clock;

    public ReservationExpiryJob(ReservationRepository reservations, Clock clock) {
        this.reservations = reservations;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.reservation.cleanup-interval:PT30S}")
    @Transactional
    public void expireReservations() {
        reservations.expireStaleHolds(clock.instant());
    }
}
