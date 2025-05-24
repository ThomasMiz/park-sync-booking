package edu.itba.serverdps.domain.model;

import java.util.UUID;

/**
 * Represents a reservation, whether pending or confirmed, made by a visitor with a ticket for an attraction.
 */
public record Reservation(
        Ticket ticket,
        Attraction attraction
) {
    public UUID visitorId() {
        return ticket.visitorId();
    }

    public int dayOfYear() {
        return ticket.dayOfYear();
    }
}
