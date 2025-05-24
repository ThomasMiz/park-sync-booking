package edu.itba.serverdps.domain.usecase.handler;

import edu.itba.serverdps.application.exceptions.*;
import edu.itba.serverdps.domain.model.Attraction;
import edu.itba.serverdps.domain.model.ConfirmedReservation;
import edu.itba.serverdps.domain.model.Reservation;
import edu.itba.serverdps.domain.model.Ticket;
import edu.itba.serverdps.domain.model.result.AttractionAvailabilityResult;
import edu.itba.serverdps.domain.model.result.DefineSlotCapacityResult;
import edu.itba.serverdps.domain.model.result.MakeReservationResult;
import edu.itba.serverdps.domain.model.result.SuggestedCapacityResult;
import edu.itba.serverdps.domain.usecase.ReservationObserver;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Manages the reservations for an attraction, for a specific day.
 */
@Getter
public class ReservationHandler {

    /**
     * The attraction for which this ReservationHandler manages reservations.
     */
    private final Attraction attraction;

    /**
     * The day of year for which this ReservationHandler manages reservations.
     */
    private final int dayOfYear;

    /**
     * The minute-of-day of the first slot.
     */
    private final int firstSlotMinuteOfDay;

    /**
     * The total amount of slots available for the day.
     */
    private final int slotCount;
    /**
     * Stores the confirmed set of visitors for each slot. The slots are stored ordered by time ascending.
     * Note: all elements in this array start as null and are created as needed.
     */
    private final Map<UUID, ConfirmedReservation>[] slotConfirmedRequests;
    /**
     * Stores the pending reservation requests for each slot. Requests are added to this queue as they arrive, and
     * therefore are ordered chronologically.
     * Note: all elements in this array start as null and are created as needed.
     */
    private final LinkedHashMap<UUID, Reservation>[] slotPendingRequests;
    /**
     * The amount of people each slot may assign, or -1 if this has not been defined yet.
     * -- GETTER --
     * Gets the slot capacity, or -1 if it hasn't been defined yet.
     */
    private int slotCapacity = -1;
    /**
     * A ReservationObserver that listens to reservation changes from this ReservationHandler.
     */
    private final ReservationObserver reservationObserver;

    /**
     * Creates a new ReservationHandler for the given attraction and day of year.
     */
    public ReservationHandler(Attraction attraction, int dayOfYear, ReservationObserver reservationObserver) {
        this.attraction = Objects.requireNonNull(attraction);
        this.dayOfYear = dayOfYear;
        this.reservationObserver = reservationObserver;

        LocalTime openingTime = attraction.openingTime();
        LocalTime closingTime = attraction.closingTime();
        int slotDuration = attraction.slotDuration();
        this.firstSlotMinuteOfDay = openingTime.getMinute() + openingTime.getHour() * 60;

        // slotCount is calculated as: slotCount = ceiling(totalMinutesOpen / slotDuration)
        int closingTimeMinuteOfDay = closingTime.getMinute() + closingTime.getHour() * 60;
        this.slotCount = (closingTimeMinuteOfDay - firstSlotMinuteOfDay + slotDuration - 1) / slotDuration;
        if (this.slotCount <= 0)
            throw new IllegalArgumentException("The attraction must have at least one time slot");

        this.slotConfirmedRequests = (Map<UUID, ConfirmedReservation>[]) new Map[slotCount];
        this.slotPendingRequests = (LinkedHashMap<UUID, Reservation>[]) new LinkedHashMap[slotCount];
    }

    /**
     * Creates a new ReservationHandler for the given attraction and day of year, and includes the internal confirmed
     * and pending data structures.
     * THIS CONSTRUCTOR IS INTENDED ONLY FOR TESTING. Use the other constructor for everything else.
     */
    public ReservationHandler(Attraction attraction, int dayOfYear, ReservationObserver reservationObserver, int slotCapacity, Map<UUID, ConfirmedReservation>[] slotConfirmedRequests, LinkedHashMap<UUID, Reservation>[] slotPendingRequests) {
        this.attraction = Objects.requireNonNull(attraction);
        this.dayOfYear = dayOfYear;
        this.slotCapacity = slotCapacity;
        this.reservationObserver = reservationObserver;

        LocalTime openingTime = attraction.openingTime();
        LocalTime closingTime = attraction.closingTime();
        int slotDuration = attraction.slotDuration();
        this.firstSlotMinuteOfDay = openingTime.getMinute() + openingTime.getHour() * 60;

        // slotCount is calculated as: slotCount = ceiling(totalMinutesOpen / slotDuration)
        int closingTimeMinuteOfDay = closingTime.getMinute() + closingTime.getHour() * 60;
        this.slotCount = (closingTimeMinuteOfDay - firstSlotMinuteOfDay + slotDuration - 1) / (slotDuration);
        if (this.slotCount <= 0)
            throw new IllegalArgumentException("The attraction must have at least one time slot");

        this.slotConfirmedRequests = slotConfirmedRequests;
        this.slotPendingRequests = slotPendingRequests;
    }

    private Map<UUID, ConfirmedReservation> getOrCreateSlotConfirmedRequests(int slotIndex) {
        return Optional.ofNullable(slotConfirmedRequests[slotIndex])
                .orElseGet(() -> {
                    Map<UUID, ConfirmedReservation> confirmed = new HashMap<>();
                    slotConfirmedRequests[slotIndex] = confirmed;
                    return confirmed;
                });
    }

    private LinkedHashMap<UUID, Reservation> getOrCreateSlotPendingRequests(int slotIndex) {
        return Optional.ofNullable(slotPendingRequests[slotIndex])
                .orElseGet(() -> {
                    LinkedHashMap<UUID, Reservation> pending = new LinkedHashMap<>();
                    slotPendingRequests[slotIndex] = pending;
                    return pending;
                });
    }

    /**
     * Gets the index in the 'slots' array where the slot for a given time is, or -1 if there's no slot with that time.
     */
    private int getSlotIndex(LocalTime slotTime) {
        int slotDuration = attraction.slotDuration();
        int slotMinuteOfDay = slotTime.getMinute() + slotTime.getHour() * 60;
        int diff = slotMinuteOfDay - firstSlotMinuteOfDay;
        int slotIndex = diff / slotDuration;

        // Check that slotTime was the exact time at which the slot starts. Without this check, if a slot went from
        // 9:00 to 9:30 and a slotTime of 9:15 was specified, it would be taken as that slot.
        if (slotIndex * slotDuration != diff)
            return -1;

        return (slotIndex < 0 || slotIndex >= slotCount) ? -1 : slotIndex;
    }

    /**
     * Same as getSlotIndex, but throws an exception instead of returning -1.
     *
     * @throws InvalidSlotException if the slotTime doesn't exactly match a slot's time
     */
    private int getSlotIndexOrThrow(LocalTime slotTime) {
        int slotIndex = getSlotIndex(slotTime);
        if (slotIndex == -1)
            throw new InvalidSlotException();
        return slotIndex;
    }

    /**
     * Inverse of getSlotIndex().
     */
    private LocalTime getSlotTimeByIndex(int slotIndex) {
        int slotMinuteOfDay = firstSlotMinuteOfDay + slotIndex * attraction.slotDuration();
        return LocalTime.ofSecondOfDay(slotMinuteOfDay * 60L);
    }

    /**
     * Returns true if the slot time is valid, false otherwise.
     */
    public boolean isSlotTimeValid(LocalTime slotTime) {
        return getSlotIndex(slotTime) >= 0;
    }

    /**
     * Sets the slot capacity, if it isn't already set.
     *
     * @throws CapacityAlreadyDefinedException if slot capacity is already defined.
     */
    public synchronized DefineSlotCapacityResult defineSlotCapacity(int slotCapacity) {
        if (this.slotCapacity != -1)
            throw new CapacityAlreadyDefinedException();

        this.slotCapacity = slotCapacity;
        if (reservationObserver != null)
            reservationObserver.onSlotCapacitySet(attraction, dayOfYear, slotCapacity);

        int[] stats = processPendingReservations();
        return new DefineSlotCapacityResult(stats[0], stats[1], stats[2]);
    }

    private int[] processPendingReservations() {
        int[] stats = new int[3]; // [confirmed, relocated, cancelled]
        final int[] sortingTiebreaker = {0};
        LocalDateTime dateTimeNow = LocalDateTime.now();

        IntStream.range(0, slotPendingRequests.length)
                .forEach(slotIndex -> {
                    LinkedHashMap<UUID, Reservation> requests = slotPendingRequests[slotIndex];
                    if (requests == null || requests.isEmpty())
                        return;

                    Map<UUID, ConfirmedReservation> confirmed = getOrCreateSlotConfirmedRequests(slotIndex);
                    LocalTime slotIndexTime = getSlotTimeByIndex(slotIndex);

                    // Collect reservations to process first
                    List<Reservation> reservationsToProcess = requests.values().stream()
                            .limit(Math.max(0, slotCapacity - confirmed.size()))
                            .collect(Collectors.toList());

                    // Process collected reservations
                    reservationsToProcess.forEach(reservation -> {
                        requests.remove(reservation.visitorId());
                        ConfirmedReservation confirmedReservation = new ConfirmedReservation(
                                reservation, slotIndexTime, dateTimeNow, sortingTiebreaker[0]++);
                        confirmed.put(reservation.visitorId(), confirmedReservation);
                        stats[0]++;
                        if (reservationObserver != null)
                            reservationObserver.onConfirmed(confirmedReservation);
                    });
                });

        IntStream.range(0, slotPendingRequests.length)
                .forEach(slotIndex -> {
                    LinkedHashMap<UUID, Reservation> requests = slotPendingRequests[slotIndex];
                    if (requests == null || requests.isEmpty())
                        return;

                    LocalTime slotIndexTime = getSlotTimeByIndex(slotIndex);
                    int amountToRelocate = Math.max(0, slotConfirmedRequests[slotIndex].size() + requests.size() - slotCapacity);

                    // Collect reservations to relocate first
                    List<Reservation> reservationsToRelocate = requests.values().stream()
                            .limit(amountToRelocate)
                            .collect(Collectors.toList());

                    // Process collected reservations
                    reservationsToRelocate.forEach(reservationToRelocate -> {
                        requests.remove(reservationToRelocate.visitorId());
                        boolean relocated = tryRelocateReservation(reservationToRelocate, slotIndex, slotIndexTime);
                        if (relocated) {
                            stats[1]++;
                        } else {
                            stats[2]++;
                        }
                    });
                });

        return stats;
    }

    private boolean tryRelocateReservation(Reservation reservation, int currentSlotIndex, LocalTime currentSlotTime) {
        return IntStream.range(currentSlotIndex + 1, slotCount)
                .filter(nextSlotIndex -> reservation.ticket().ticketType()
                        .isSlotTimeValid(getSlotTimeByIndex(nextSlotIndex)))
                .filter(nextSlotIndex -> {
                    Map<UUID, ConfirmedReservation> nextConfirmed = getOrCreateSlotConfirmedRequests(nextSlotIndex);
                    LinkedHashMap<UUID, Reservation> nextPending = slotPendingRequests[nextSlotIndex];
                    int nextTotal = nextConfirmed.size() + (nextPending == null ? 0 : nextPending.size());
                    return nextTotal < slotCapacity;
                })
                .boxed()
                .findFirst()
                .map(nextSlotIndex -> {
                    LinkedHashMap<UUID, Reservation> nextPending = getOrCreateSlotPendingRequests(nextSlotIndex);
                    boolean success = nextPending.putIfAbsent(reservation.visitorId(), reservation) == null;
                    if (success && reservationObserver != null) {
                        reservationObserver.onRelocated(reservation, currentSlotTime, getSlotTimeByIndex(nextSlotIndex));
                    }
                    return success;
                })
                .orElseGet(() -> {
                    if (reservationObserver != null) {
                        reservationObserver.onCancelled(reservation, currentSlotTime);
                    }
                    return false;
                });
    }

    private void cancelPendingReservationsForSlotIfFull(int slotIndex) {
        Map<UUID, ConfirmedReservation> confirmed = slotConfirmedRequests[slotIndex];
        LinkedHashMap<UUID, Reservation> pendings = slotPendingRequests[slotIndex];

        if (confirmed != null && confirmed.size() >= slotCapacity && pendings != null) {
            if (reservationObserver != null) {
                LocalTime slotTime = getSlotTimeByIndex(slotIndex);
                pendings.values().forEach(r -> reservationObserver.onCancelled(r, slotTime));
            }
            pendings.clear();
        }
    }

    /**
     * Attempts to make a reservation for a given visitor and time slot.
     *
     * @return A Reservation
     * @throws InvalidSlotException              when the slot doesn't exist
     * @throws ReservationAlreadyExistsException if no such reservation exists
     * @throws OutOfCapacityException            if the slot is out of capacity
     */
    public synchronized MakeReservationResult makeReservation(Ticket ticket, LocalTime slotTime) {
        final int slotIndex = getSlotIndexOrThrow(slotTime);

        // The behavior of this method changes depending on whether slot capacities have been defined:
        // - If slot capacities have not been defined, the reservation is queued until they are.
        // - If slot capacities have been defined, the reservation is attempted immediately.

        if (slotCapacity == -1) {
            // Slot capacity has not been defined, queue the reservation.
            Reservation reservation = new Reservation(ticket, attraction);
            if (getOrCreateSlotPendingRequests(slotIndex).putIfAbsent(reservation.visitorId(), reservation) != null)
                throw new ReservationAlreadyExistsException();

            if (reservationObserver != null)
                reservationObserver.onCreated(reservation, slotTime, false);
            return new MakeReservationResult(reservation, false);
        }

        // Slot capacity has been defined, attempt the reservation right now.
        final Map<UUID, ConfirmedReservation> confirmed = getOrCreateSlotConfirmedRequests(slotIndex);
        if (confirmed.size() >= slotCapacity)
            throw new OutOfCapacityException();

        // Check if the reservation already exists as pending
        LinkedHashMap<UUID, Reservation> pendings = slotPendingRequests[slotIndex];
        if (pendings != null && pendings.containsKey(ticket.visitorId()))
            throw new ReservationAlreadyExistsException();

        ConfirmedReservation reservation = new ConfirmedReservation(ticket, attraction, slotTime);
        boolean success = confirmed.putIfAbsent(reservation.visitorId(), reservation) == null;
        if (!success)
            throw new ReservationAlreadyExistsException();

        // If max capacity was reached for this slot, cancel all its pending reservations.
        cancelPendingReservationsForSlotIfFull(slotIndex);

        if (reservationObserver != null)
            reservationObserver.onCreated(reservation.reservation(), slotTime, true);
        return new MakeReservationResult(reservation.reservation(), true);
    }

    /**
     * Confirms a reservation.
     *
     * @throws InvalidSlotException                 when the slot doesn't exist
     * @throws ReservationNotFoundException         if no such reservation exists
     * @throws ReservationAlreadyConfirmedException if the reservation has already been confirmed
     */
    public synchronized void confirmReservation(UUID visitorId, LocalTime slotTime) {
        int slotIndex = getSlotIndexOrThrow(slotTime);
        if (slotCapacity == -1)
            throw new CapacityNotDefinedException();

        LinkedHashMap<UUID, Reservation> pendings = slotPendingRequests[slotIndex];
        Reservation reservation;
        if (pendings == null || (reservation = pendings.remove(visitorId)) == null) {
            Map<UUID, ConfirmedReservation> confirmed = slotConfirmedRequests[slotIndex];
            if (confirmed != null && confirmed.containsKey(visitorId))
                throw new ReservationAlreadyConfirmedException();
            throw new ReservationNotFoundException();
        }

        Map<UUID, ConfirmedReservation> confirmed = getOrCreateSlotConfirmedRequests(slotIndex);
        ConfirmedReservation confirmedReservation = new ConfirmedReservation(reservation, slotTime);
        boolean success = confirmed.putIfAbsent(reservation.visitorId(), confirmedReservation) == null;

        if (success) {
            if (reservationObserver != null)
                reservationObserver.onConfirmed(confirmedReservation);
            cancelPendingReservationsForSlotIfFull(slotIndex);
        } else {
            // This should never happen, as checks are in place to ensure a pending reservation is never left where
            // there is already a confirmed one. We leave this here to be thorough.
            if (reservationObserver != null)
                reservationObserver.onCancelled(reservation, slotTime);
        }
    }

    /**
     * Cancels a reservation.
     *
     * @throws InvalidSlotException         when the slot doesn't exist
     * @throws ReservationNotFoundException if no such reservation exists
     */
    public synchronized void cancelReservation(UUID visitorId, LocalTime slotTime) {
        int slotIndex = getSlotIndexOrThrow(slotTime);

        Reservation reservation = Optional.ofNullable(slotPendingRequests[slotIndex])
                .map(pendings -> pendings.remove(visitorId))
                .orElseGet(() -> Optional.ofNullable(slotConfirmedRequests[slotIndex])
                        .map(confirmed -> confirmed.remove(visitorId))
                        .map(ConfirmedReservation::reservation)
                        .orElseThrow(ReservationNotFoundException::new));

        if (reservationObserver != null)
            reservationObserver.onCancelled(reservation, slotTime);
    }

    /**
     * Computes the suggested slot capacity.
     *
     * @return If slot capacity has already been decided, returns null. Otherwise, returns the suggested capacity as
     * the maximum between all slots, and the slot with the said maximum capacity.
     */
    public synchronized SuggestedCapacityResult getSuggestedCapacity() {
        if (slotCapacity != -1 || slotCount == 0)
            return null;

        return IntStream.range(0, slotPendingRequests.length)
                .mapToObj(i -> new AbstractMap.SimpleEntry<>(i,
                        Optional.ofNullable(slotPendingRequests[i])
                                .map(LinkedHashMap::size)
                                .orElse(0)))
                .max(Map.Entry.comparingByValue())
                .map(entry -> new SuggestedCapacityResult(
                        attraction,
                        entry.getValue(),
                        getSlotTimeByIndex(entry.getKey())))
                .orElse(null);
    }

    /**
     * Similar to getSlotIndex, but rounding and clamping the slot index instead of fetching an exact match.
     */
    private int getClampedSlotIndex(LocalTime slotTime, boolean clampMin) {
        int slotDuration = attraction.slotDuration();
        int slotMinuteOfDay = slotTime.getMinute() + slotTime.getHour() * 60;

        if (slotMinuteOfDay < firstSlotMinuteOfDay)
            return 0;

        int diff = slotMinuteOfDay - firstSlotMinuteOfDay;
        int slotIndex = diff / slotDuration;

        if (slotIndex >= slotCount)
            return slotCount - 1;

        if (clampMin && slotIndex * slotDuration != diff)
            return slotIndex < (slotCount - 1) ? slotIndex + 1 : (slotCount - 1);
        return slotIndex;
    }

    /**
     * Gets the availability for a given time slot.
     *
     * @param resultCollection The collection to which to add the resulting elements.
     * @param slotFrom         The start of the time slot, inclusive.
     * @param slotTo           The end of the time slot, inclusive, or null to only check slotFrom.
     * @throws InvalidSlotException if the slotFrom or slotTo times are invalid.
     */
    public synchronized void getAvailability(Collection<AttractionAvailabilityResult> resultCollection, LocalTime slotFrom, LocalTime slotTo) {
        int slotFromIndex = getClampedSlotIndex(slotFrom, true);
        int slotToIndex = slotTo == null ? slotFromIndex : getClampedSlotIndex(slotTo, false);

        IntStream.rangeClosed(slotFromIndex, slotToIndex).forEach(slotIndex -> {
            Map<UUID, ConfirmedReservation> confirmed = slotConfirmedRequests[slotIndex];
            LinkedHashMap<UUID, Reservation> pendings = slotPendingRequests[slotIndex];
            LocalTime slotTime = getSlotTimeByIndex(slotIndex);

            int confirmedCount = confirmed == null ? 0 : confirmed.size();
            int pendingCount = pendings == null ? 0 : pendings.size();

            resultCollection.add(new AttractionAvailabilityResult(
                    this.attraction.name(),
                    slotTime,
                    this.slotCapacity,
                    confirmedCount,
                    pendingCount));
        });
    }

    /**
     * Gets all the confirmed reservations.
     *
     * @param resultCollection The collection to which to add the resulting elements.
     */
    public synchronized void getConfirmedReservations(Collection<ConfirmedReservation> resultCollection) {
        IntStream.range(0, slotConfirmedRequests.length)
                .mapToObj(i -> slotConfirmedRequests[i])
                .filter(Objects::nonNull)
                .filter(map -> !map.isEmpty())
                .map(Map::values)
                .forEach(resultCollection::addAll);
    }
}