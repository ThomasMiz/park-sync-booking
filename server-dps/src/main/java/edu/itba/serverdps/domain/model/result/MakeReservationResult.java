package edu.itba.serverdps.domain.model.result;

import edu.itba.serverdps.domain.model.Reservation;

/**
 * Represents the result of a make reservation request.
 */
public record MakeReservationResult(
        Reservation reservation,
        boolean isConfirmed
) {}