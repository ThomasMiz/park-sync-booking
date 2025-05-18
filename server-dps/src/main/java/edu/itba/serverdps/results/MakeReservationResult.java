package edu.itba.serverdps.results;

import edu.itba.serverdps.models.Reservation;

/**
 * Represents the result of a make reservation request.
 */
public record MakeReservationResult(Reservation reservation, boolean isConfirmed) {
}