package edu.itba.serverdps.domain.model.result;

/**
 * Represents the result of a set slot capacity request.
 */
public record DefineSlotCapacityResult(
        int bookingsConfirmed,
        int bookingsRelocated,
        int bookingsCancelled
) {}
