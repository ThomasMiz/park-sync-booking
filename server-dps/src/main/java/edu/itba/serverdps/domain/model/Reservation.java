package edu.itba.serverdps.domain.model;

import lombok.Getter;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a reservation, whether pending or confirmed, made by a visitor with a ticket for an attraction.
 */
public class Reservation {
    @Getter
    private final Ticket ticket;
    @Getter
    private final Attraction attraction;

    public Reservation(Ticket ticket, Attraction attraction) {
        this.ticket = ticket;
        this.attraction = attraction;
    }

    public UUID getVisitorId() {
        return ticket.getVisitorId();
    }

    public int getDayOfYear() {
        return ticket.getDayOfYear();
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
