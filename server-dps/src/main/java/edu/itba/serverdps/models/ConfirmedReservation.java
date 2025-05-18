package edu.itba.serverdps.models;

import edu.itba.serverdps.models.Attraction;
import edu.itba.serverdps.models.Reservation;
import edu.itba.serverdps.models.Ticket;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class ConfirmedReservation extends Reservation {
    private final LocalDateTime dateConfirmed;
    private final LocalTime slotTime;
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

    public LocalDateTime getDateConfirmed() {
        return dateConfirmed;
    }

    public LocalTime getSlotTime() {
        return slotTime;
    }

    public int getSortingTiebreaker() {
        return sortingTiebreaker;
    }

    public int compareByDateAndTiebreakerTo(ConfirmedReservation other) {
        int cmp = this.dateConfirmed.compareTo(other.dateConfirmed);
        return cmp == 0 ? Integer.compare(this.sortingTiebreaker, other.sortingTiebreaker) : cmp;
    }
}
