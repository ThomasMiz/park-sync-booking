package edu.itba.serverdps.results;

import java.time.LocalTime;

/**
 * Represents the result of a get attraction slot availability request.
 */
public record AttractionAvailabilityResult(String attractionName, LocalTime slotTime, int slotCapacity, int confirmedReservations, int pendingReservations) {
}
