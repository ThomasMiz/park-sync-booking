package edu.itba.serverdps.notifications;

import edu.itba.serverdps.models.Attraction;
import edu.itba.serverdps.models.ConfirmedReservation;
import edu.itba.serverdps.models.Reservation;

import java.time.LocalTime;

/**
 * Receives notifications about reservations such as status changes, and when slot capacity is set.
 */
public interface ReservationObserver {
    /**
     * Notifies that slot capacity was set for an attraction at a given day. When the relocation algorithm is run, this
     * should be called before any of the other methods in this interface.
     */
    void onSlotCapacitySet(Attraction attraction, int dayOfYear, int slotCapacity);

    /**
     * Called whenever a new reservation is created.
     *
     * @implNote To avoid race conditions, use the isConfirmed param instead of reservation.isConfirmed().
     */
    void onCreated(Reservation reservation, LocalTime slotTime, boolean isConfirmed);

    /**
     * Called whenever a previously pending reservation was confirmed.
     */
    void onConfirmed(ConfirmedReservation reservation);

    /**
     * Called whenever a pending reservation was relocated (and remains pending).
     */
    void onRelocated(Reservation reservation, LocalTime prevSlotTime, LocalTime newSlotTime);

    /**
     * Called whenever a previously pending reservation was cancelled.
     */
    void onCancelled(Reservation reservation, LocalTime slotTime);
}
