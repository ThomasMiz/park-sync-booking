package edu.itba.serverdps.models;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a reservation, whether pending or confirmed, made by a visitor with a ticket for an attraction.
 */
public class Reservation {
    private final Ticket ticket;
    private final Attraction attraction;

    public Reservation(Ticket ticket, Attraction attraction) {
        this.ticket = ticket;
        this.attraction = attraction;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public UUID getVisitorId() {
        return ticket.getVisitorId();
    }

    public int getDayOfYear() {
        return ticket.getDayOfYear();
    }

    public Attraction getAttraction() {
        return attraction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reservation that = (Reservation) o;
        return Objects.equals(ticket, that.ticket) && Objects.equals(attraction, that.attraction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ticket, attraction);
    }
}
