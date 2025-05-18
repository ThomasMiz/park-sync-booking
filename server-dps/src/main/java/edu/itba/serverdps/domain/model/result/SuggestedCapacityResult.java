package edu.itba.serverdps.domain.model.result;

import edu.itba.serverdps.domain.model.Attraction;

import java.time.LocalTime;

/**
 * Represents the result of a get suggested capacity query.
 *
 * @param attraction                 The attraction from which this result originated.
 * @param maxPendingReservationCount The maximum amount of pending reservations any given slot has, or 0 if there are no slots.
 * @param slotTime                   The slot's time, or null if there are no slots.
 */
public record SuggestedCapacityResult(Attraction attraction, int maxPendingReservationCount, LocalTime slotTime) {
    public int compareCapacityTo(SuggestedCapacityResult other) {
        int cmp = Integer.compare(other.maxPendingReservationCount, this.maxPendingReservationCount);
        return cmp == 0 ? this.attraction.getName().compareTo(other.attraction.getName()) : cmp;
    }
}