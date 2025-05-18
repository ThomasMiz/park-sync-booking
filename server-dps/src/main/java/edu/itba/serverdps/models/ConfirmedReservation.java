package edu.itba.serverdps.models;

import lombok.Getter;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class ConfirmedReservation extends Reservation {
    @Getter
    private final LocalDateTime dateConfirmed;
    @Getter
    private final LocalTime slotTime;
    @Getter
    private final int sortingTiebreaker;

    public ConfirmedReservation(Ticket ticket, Attraction attraction, LocalTime slotTime, LocalDateTime dateConfirmed, int sortingTiebreaker) {
        super(ticket, attraction);
        this.slotTime = slotTime;
        this.dateConfirmed = dateConfirmed;
        this.sortingTiebreaker = sortingTiebreaker;
    }

    public ConfirmedReservation(Reservation reservation, LocalTime slotTime, LocalDateTime dateConfirmed, int sortingTiebreaker) {
        this(reservation.getTicket(), reservation.getAttraction(), slotTime, dateConfirmed, sortingTiebreaker);
    }

    public ConfirmedReservation(Reservation reservation, LocalTime slotTime) {
        this(reservation.getTicket(), reservation.getAttraction(), slotTime, LocalDateTime.now(), 0);
    }

    public ConfirmedReservation(Ticket ticket, Attraction attraction, LocalTime slotTime) {
        this(ticket, attraction, slotTime, LocalDateTime.now(), 0);
    }

    public int compareByDateAndTiebreakerTo(ConfirmedReservation other) {
        int cmp = this.dateConfirmed.compareTo(other.dateConfirmed);
        return cmp == 0 ? Integer.compare(this.sortingTiebreaker, other.sortingTiebreaker) : cmp;
    }
}
