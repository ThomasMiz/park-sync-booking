package edu.itba.serverdps.domain.model;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record ConfirmedReservation(
        Reservation reservation,
        LocalTime slotTime,
        LocalDateTime dateConfirmed,
        int sortingTiebreaker
) {
    public ConfirmedReservation(Reservation reservation, LocalTime slotTime) {
        this(reservation, slotTime, LocalDateTime.now(), 0);
    }

    public ConfirmedReservation(Ticket ticket, Attraction attraction, LocalTime slotTime) {
        this(new Reservation(ticket, attraction), slotTime, LocalDateTime.now(), 0);
    }

    public int compareByDateAndTiebreakerTo(ConfirmedReservation other) {
        int cmp = this.dateConfirmed.compareTo(other.dateConfirmed);
        return cmp == 0 ? Integer.compare(this.sortingTiebreaker, other.sortingTiebreaker) : cmp;
    }

    public UUID visitorId() {
        return reservation.visitorId();
    }

    public int dayOfYear() {
        return reservation.dayOfYear();
    }

    public Attraction attraction() {
        return reservation.attraction();
    }

    public Ticket ticket() {
        return reservation.ticket();
    }
}
