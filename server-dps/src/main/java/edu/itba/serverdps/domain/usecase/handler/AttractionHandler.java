package edu.itba.serverdps.domain.usecase.handler;

import edu.itba.serverdps.application.exceptions.AttractionAlreadyExistsException;
import edu.itba.serverdps.application.exceptions.AttractionNotFoundException;
import edu.itba.serverdps.application.exceptions.MissingPassException;
import edu.itba.serverdps.application.exceptions.TicketAlreadyExistsException;
import edu.itba.serverdps.domain.model.Attraction;
import edu.itba.serverdps.domain.model.ConfirmedReservation;
import edu.itba.serverdps.domain.model.Ticket;
import edu.itba.serverdps.domain.model.TicketType;
import edu.itba.serverdps.domain.usecase.ReservationObserver;
import edu.itba.serverdps.domain.model.result.AttractionAvailabilityResult;
import edu.itba.serverdps.domain.model.result.DefineSlotCapacityResult;
import edu.itba.serverdps.domain.model.result.MakeReservationResult;
import edu.itba.serverdps.domain.model.result.SuggestedCapacityResult;
import edu.itba.serverdps.application.utils.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class AttractionHandler {

    private final ConcurrentMap<String, Attraction> attractions;
    private final ConcurrentMap<UUID, Ticket>[] ticketsByDay;
    private final ReservationObserver reservationObserver;

    @Autowired
    public AttractionHandler(ReservationObserver reservationObserver) {
        this.attractions = new ConcurrentHashMap<>();
        this.ticketsByDay = (ConcurrentMap<UUID, Ticket>[]) new ConcurrentMap[Constants.DAYS_IN_YEAR];
        for (int i = 0; i < ticketsByDay.length; i++)
            ticketsByDay[i] = new ConcurrentHashMap<>();
        this.reservationObserver = reservationObserver;
    }

    /**
     * Creates a AttractionHandler with the given attraction and ticket maps.
     * THIS CONSTRUCTOR IS INTENDED ONLY FOR TESTING. Use the default constructor for everything else.
     */
    public AttractionHandler(ConcurrentMap<String, Attraction> attractions, ConcurrentMap<UUID, Ticket>[] ticketsByDay) {
        this.attractions = attractions;
        this.ticketsByDay = ticketsByDay;
        this.reservationObserver = null;
    }

    private Ticket getTicketOrThrow(UUID visitorId, int dayOfYear) {
        Ticket ticket = this.ticketsByDay[dayOfYear - 1].get(visitorId);
        if (ticket == null)
            throw new MissingPassException();
        return ticket;
    }

    /**
     * Gets an attraction by name.
     *
     * @throws AttractionNotFoundException If no attraction is found with that name.
     */
    public Attraction getAttraction(String attractionName) {
        Attraction attraction = this.attractions.get(attractionName);
        if (attraction == null)
            throw new AttractionNotFoundException();
        return attraction;
    }

    public void createAttraction(String attractionName, LocalTime openTime, LocalTime closeTime, int slotDuration) {
        Attraction attraction = new Attraction(attractionName, openTime, closeTime, slotDuration, reservationObserver);
        if (this.attractions.putIfAbsent(attractionName, attraction) != null) {
            throw new AttractionAlreadyExistsException();
        }
    }

    public DefineSlotCapacityResult setSlotCapacityForAttraction(String attractionName, int dayOfYear, int slotCapacity) {
        return getAttraction(attractionName).setSlotCapacity(dayOfYear, slotCapacity);
    }

    public Collection<Attraction> getAttractions() {
        return Collections.unmodifiableCollection(this.attractions.values());
    }

    public void addTicket(UUID visitorId, int dayOfYear, TicketType ticketType) {
        ConcurrentMap<UUID, Ticket> visitorTickets = ticketsByDay[dayOfYear - 1];
        Ticket ticket = new Ticket(visitorId, dayOfYear, ticketType);
        if (visitorTickets.putIfAbsent(visitorId, ticket) != null) {
            throw new TicketAlreadyExistsException();
        }
    }

    public MakeReservationResult makeReservation(String attractionName, UUID visitorId, int dayOfYear, LocalTime slotTime) {
        Attraction attraction = getAttraction(attractionName);
        Ticket ticket = getTicketOrThrow(visitorId, dayOfYear);

        return ticket.bookTransactional(slotTime, () -> attraction.makeReservation(ticket, slotTime));
    }

    /**
     * Gets the availability for a given attraction, day of year and time slot.
     *
     * @param attractionName The name of the attraction.
     * @param dayOfYear      The day of the year.
     * @param slotFrom       The start of the time slot, inclusive.
     * @param slotTo         The end of the time slot, inclusive, or null to only check slotFrom.
     * @return A collection of AttractionAvailabilityResult with all the available slots for that attraction.
     * @throws AttractionNotFoundException If no attraction is found with that name.
     */
    public Collection<AttractionAvailabilityResult> getAvailabilityForAttraction(String attractionName, int dayOfYear, LocalTime slotFrom, LocalTime slotTo) {
        List<AttractionAvailabilityResult> resultList = new ArrayList<>();
        getAttraction(attractionName).getAvailability(resultList, dayOfYear, slotFrom, slotTo);
        return Collections.unmodifiableList(resultList);
    }

    /**
     * Gets the availability for all attractions for a given day of year and time slot.
     *
     * @param dayOfYear The day of the year.
     * @param slotFrom  The start of the time slot, inclusive.
     * @param slotTo    The end of the time slot, inclusive, or null to only check slotFrom.
     * @return A collection of AttractionAvailabilityResult with all the available slots for the attractions.
     */
    public Collection<AttractionAvailabilityResult> getAvailabilityForAllAttractions(int dayOfYear, LocalTime slotFrom, LocalTime slotTo) {
        List<AttractionAvailabilityResult> resultList = new ArrayList<>();
        for (Attraction attraction : attractions.values())
            attraction.getAvailability(resultList, dayOfYear, slotFrom, slotTo);
        return Collections.unmodifiableList(resultList);
    }

    /**
     * Gets the availability for all attractions, for a given day of year and time slot.
     *
     * @throws AttractionNotFoundException If no attraction is found with that name.
     */
    public void confirmReservation(String attractionName, UUID visitorId, int dayOfYear, LocalTime slotTime) {
        Ticket ticket = getTicketOrThrow(visitorId, dayOfYear);
        if (!ticket.getTicketType().isSlotTimeValid(slotTime))
            throw new MissingPassException();

        getAttraction(attractionName).confirmReservation(visitorId, dayOfYear, slotTime);
    }

    /**
     * Cancels a reservation for a given attraction, day of year and time slot.
     *
     * @throws AttractionNotFoundException If no attraction is found with that name.
     */
    public void cancelReservation(String attractionName, UUID visitorId, int dayOfYear, LocalTime slotTime) {
        Ticket ticket = getTicketOrThrow(visitorId, dayOfYear);
        if (!ticket.getTicketType().isSlotTimeValid(slotTime))
            throw new MissingPassException();

        getAttraction(attractionName).cancelReservation(visitorId, dayOfYear, slotTime);
        ticket.removeBook();
    }

    public SortedSet<SuggestedCapacityResult> getSuggestedCapacities(int dayOfYear) {
        SortedSet<SuggestedCapacityResult> results = new TreeSet<>(SuggestedCapacityResult::compareCapacityTo);
        for (Attraction attraction : attractions.values()) {
            SuggestedCapacityResult r = attraction.getSuggestedCapacity(dayOfYear);
            if (r != null)
                results.add(r);
        }

        return Collections.unmodifiableSortedSet(results);
    }

    public SortedSet<ConfirmedReservation> getConfirmedReservations(int dayOfYear) {
        SortedSet<ConfirmedReservation> results = new TreeSet<>(ConfirmedReservation::compareByDateAndTiebreakerTo);

        for (Attraction attraction : attractions.values())
            attraction.getConfirmedReservations(results, dayOfYear);

        return Collections.unmodifiableSortedSet(results);
    }
}
