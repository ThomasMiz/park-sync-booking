package edu.itba.serverdps.models;

import edu.itba.serverdps.handlers.ReservationHandler;
import edu.itba.serverdps.models.Ticket;
import edu.itba.serverdps.notifications.ReservationObserver;
import edu.itba.serverdps.results.AttractionAvailabilityResult;
import edu.itba.serverdps.results.DefineSlotCapacityResult;
import edu.itba.serverdps.results.MakeReservationResult;
import edu.itba.serverdps.results.SuggestedCapacityResult;
import edu.itba.serverdps.utils.Constants;
import lombok.Getter;

import java.time.LocalTime;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

public class Attraction {
    @Getter
    private final String name;
    @Getter
    private final LocalTime openingTime;
    @Getter
    private final LocalTime closingTime;
    @Getter
    private final int slotDuration;

    private final ReservationHandler[] reservationHandlers;

    public Attraction(String name, LocalTime openingTime, LocalTime closingTime, int slotDuration, ReservationObserver reservationObserver) {
        this.name = Objects.requireNonNull(name);
        this.openingTime = Objects.requireNonNull(openingTime);
        this.closingTime = Objects.requireNonNull(closingTime);
        this.slotDuration = slotDuration;
        this.reservationHandlers = new ReservationHandler[Constants.DAYS_IN_YEAR];
        for (int i = 0; i < reservationHandlers.length; i++)
            reservationHandlers[i] = new ReservationHandler(this, i + 1, reservationObserver);
    }

    public Attraction(String name, LocalTime openingTime, LocalTime closingTime, int slotDuration) {
        this(name, openingTime, closingTime, slotDuration, null);
    }

    /**
     * Attempts to set slot capacity, failing if capacity is already set.
     */
    public DefineSlotCapacityResult setSlotCapacity(int dayOfYear, int slotCapacity) {
        ReservationHandler reservationHandler = reservationHandlers[dayOfYear - 1];
        return reservationHandler.defineSlotCapacity(slotCapacity);
    }

    /**
     * Gets the suggested slot capacity for a given day, or null if slot capacity has already been set for that day.
     */
    public SuggestedCapacityResult getSuggestedCapacity(int dayOfYear) {
        ReservationHandler reservationHandler = reservationHandlers[dayOfYear - 1];
        return reservationHandler.getSuggestedCapacity();
    }

    /**
     * Attempts to make a reservation for a given ticket (which includes visitorId and dayOfYear) and time slot.
     */
    public MakeReservationResult makeReservation(Ticket ticket, LocalTime slotTime) {
        return reservationHandlers[ticket.getDayOfYear() - 1].makeReservation(ticket, slotTime);
    }

    /**
     * Gets the availability for a given time slot.
     *
     * @param resultCollection The collection to which to add the resulting elements.
     * @param dayOfYear        The day of the year.
     * @param slotFrom         The start of the time slot, inclusive.
     * @param slotTo           The end of the time slot, inclusive, or null to only check slotFrom.
     */
    public void getAvailability(Collection<AttractionAvailabilityResult> resultCollection, int dayOfYear, LocalTime slotFrom, LocalTime slotTo) {
        reservationHandlers[dayOfYear - 1].getAvailability(resultCollection, slotFrom, slotTo);
    }

    /**
     * Gets all the confirmed reservations for a given day.
     *
     * @param resultCollection The collection to which to add the resulting elements.
     * @param dayOfYear        The day of the year.
     */
    public void getConfirmedReservations(Collection<ConfirmedReservation> resultCollection, int dayOfYear) {
        reservationHandlers[dayOfYear - 1].getConfirmedReservations(resultCollection);
    }

    /**
     * Gets the reservation handler for a given day of year. ONLY USED FOR TESTING PURPOSES!
     */
    public ReservationHandler getReservationHandler(int dayOfYear) {
        return reservationHandlers[dayOfYear - 1];
    }

    /**
     * Sets the reservation handler for a given day of year. ONLY USED FOR TESTING PURPOSES!
     */
    public void setReservationHandler(int dayOfYear, ReservationHandler reservationHandler) {
        reservationHandlers[dayOfYear - 1] = reservationHandler;
    }

    /**
     * Confirms a reservation for a given visitorId, day of year and time slot.
     */
    public void confirmReservation(UUID visitorId, int dayOfYear, LocalTime slotTime) {
        reservationHandlers[dayOfYear - 1].confirmReservation(visitorId, slotTime);
    }

    /**
     * Cancels a reservation for a given visitorId, day of year and time slot.
     */
    public void cancelReservation(UUID visitorId, int dayOfYear, LocalTime slotTime) {
        reservationHandlers[dayOfYear - 1].cancelReservation(visitorId, slotTime);
    }
}
