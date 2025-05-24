package edu.itba.serverdps.domain.usecase.handler;

import edu.itba.serverdps.application.exceptions.*;
import edu.itba.serverdps.domain.model.Attraction;
import edu.itba.serverdps.domain.model.TicketType;
import edu.itba.serverdps.domain.model.result.AttractionAvailabilityResult;
import edu.itba.serverdps.domain.model.result.DefineSlotCapacityResult;
import edu.itba.serverdps.domain.model.result.MakeReservationResult;
import edu.itba.serverdps.domain.usecase.ReservationObserver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalTime;
import java.util.*;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class AttractionHandlerTest {
    private static final String ATTRACTION_NAME = "attractionName";
    private static final String ANOTHER_ATTRACTION_NAME = "otherAttractionName";
    private static final String INVALID_ATTRACTION_NAME = "";
    private static final LocalTime OPENING_TIME = LocalTime.of(10, 0);
    private static final LocalTime CLOSING_TIME = LocalTime.of(18, 0);
    private static final int SLOT_DURATION = 10;
    private static final int TOTAL_SLOTS = (CLOSING_TIME.toSecondOfDay() - OPENING_TIME.toSecondOfDay()) / (SLOT_DURATION * 60);
    private static final int NO_SLOT_GAP = 0;
    private static final int NEGATIVE_SLOT_GAP = -1;
    private static final String DEFAULT_VISITOR_ID_STRING = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
    private static final UUID DEFAULT_VISITOR_ID_UUID = UUID.fromString(DEFAULT_VISITOR_ID_STRING);
    private static final int VALID_DAY_OF_YEAR = 1;
    private static final int OTHER_VALID_DAY_OF_YEAR = 365;
    private static final int INVALID_DAY_OF_YEAR = 366;
    private static final int OTHER_INVALID_DAY_OF_YEAR = 0;
    private static final int VALID_CAPACITY = 10;
    private static final int INVALID_CAPACITY = -1;
    private static final String TIME_FROM_STRING = "10:00";
    private static final String TIME_TO_STRING = "18:00";
    private static final String HALF_DAY_TIME_RESTRICTION_LIMIT = "14:00";
    private static final UUID VISITOR_ID = UUID.randomUUID();
    private static final String NON_EXISTING_ATTRACTION_NAME = "nonExistingAttractionName";
    private static final TicketType TICKET_TYPE_FULL_DAY = TicketType.FULL_DAY;
    private static final TicketType TICKET_TYPE_HALF_DAY = TicketType.HALF_DAY;
    private static final int SLOT_CAPACITY = 6;
    private static final int NO_SLOT_CAPACITY = -1;
    private static final int MAX_BOOKINGS_FOR_FULL_DAY = 3;

    @Mock
    private ReservationObserver reservationObserver;

    @InjectMocks
    private AttractionHandler attractionHandler;

    @Test
    public void testAddAttraction() {
        attractionHandler.createAttraction(ATTRACTION_NAME, OPENING_TIME, CLOSING_TIME, SLOT_DURATION);

        Attraction attraction = attractionHandler.getAttraction(ATTRACTION_NAME);
        assertNotNull(attraction);
        assertEquals(ATTRACTION_NAME, attraction.name());
        assertEquals(OPENING_TIME, attraction.openingTime());
        assertEquals(CLOSING_TIME, attraction.closingTime());
        assertEquals(SLOT_DURATION, attraction.slotDuration());
    }

    @Test
    public void testAddAnotherAttraction() {
        // First create an attraction
        attractionHandler.createAttraction(ANOTHER_ATTRACTION_NAME, OPENING_TIME, CLOSING_TIME, SLOT_DURATION);

        // Then create another attraction
        attractionHandler.createAttraction(ATTRACTION_NAME, OPENING_TIME, CLOSING_TIME, SLOT_DURATION);

        // Verify both attractions exist
        Attraction attraction1 = attractionHandler.getAttraction(ATTRACTION_NAME);
        Attraction attraction2 = attractionHandler.getAttraction(ANOTHER_ATTRACTION_NAME);

        assertNotNull(attraction1);
        assertNotNull(attraction2);
        assertEquals(ATTRACTION_NAME, attraction1.name());
        assertEquals(ANOTHER_ATTRACTION_NAME, attraction2.name());
    }

    @Test
    public void testAddAttractionWithExistingName() {
        // First create an attraction
        attractionHandler.createAttraction(ATTRACTION_NAME, OPENING_TIME, CLOSING_TIME, SLOT_DURATION);

        // Try to create another attraction with the same name
        assertThrows(AttractionAlreadyExistsException.class, () ->
                attractionHandler.createAttraction(ATTRACTION_NAME, OPENING_TIME, CLOSING_TIME, SLOT_DURATION));

        // Verify the attraction exists
        Attraction attraction = attractionHandler.getAttraction(ATTRACTION_NAME);
        assertNotNull(attraction);
        assertEquals(ATTRACTION_NAME, attraction.name());
    }

    @Test
    public void testAddTicket() {
        // Add a ticket
        attractionHandler.addTicket(DEFAULT_VISITOR_ID_UUID, VALID_DAY_OF_YEAR, TicketType.FULL_DAY);

        // Make a test reservation that should succeed, which verifies the ticket exists
        attractionHandler.createAttraction(ATTRACTION_NAME, OPENING_TIME, CLOSING_TIME, SLOT_DURATION);
        attractionHandler.setSlotCapacityForAttraction(ATTRACTION_NAME, VALID_DAY_OF_YEAR, VALID_CAPACITY);

        // Try to make a reservation - this would throw MissingPassException if the ticket didn't exist
        MakeReservationResult result = attractionHandler.makeReservation(
                ATTRACTION_NAME, DEFAULT_VISITOR_ID_UUID, VALID_DAY_OF_YEAR, OPENING_TIME);

        // Verify the result shows a successful reservation
        assertTrue(result.isConfirmed());
    }

    @Test
    public void testAddSameTicketPassForSameDate() {
        // Add a ticket
        attractionHandler.addTicket(DEFAULT_VISITOR_ID_UUID, VALID_DAY_OF_YEAR, TicketType.FULL_DAY);

        // Try to add the same ticket again
        assertThrows(TicketAlreadyExistsException.class, () ->
                attractionHandler.addTicket(DEFAULT_VISITOR_ID_UUID, VALID_DAY_OF_YEAR, TicketType.FULL_DAY));

        // Verify the ticket exists by making a reservation
        attractionHandler.createAttraction(ATTRACTION_NAME, OPENING_TIME, CLOSING_TIME, SLOT_DURATION);
        attractionHandler.setSlotCapacityForAttraction(ATTRACTION_NAME, VALID_DAY_OF_YEAR, VALID_CAPACITY);

        MakeReservationResult result = attractionHandler.makeReservation(
                ATTRACTION_NAME, DEFAULT_VISITOR_ID_UUID, VALID_DAY_OF_YEAR, OPENING_TIME);

        assertTrue(result.isConfirmed());
    }

    @Test
    public void testAddOtherPassForSameDate() {
        // Add a ticket with FULL_DAY type
        attractionHandler.addTicket(DEFAULT_VISITOR_ID_UUID, VALID_DAY_OF_YEAR, TicketType.FULL_DAY);

        // Try to add a different type of ticket for the same date and visitor
        assertThrows(TicketAlreadyExistsException.class, () ->
                attractionHandler.addTicket(DEFAULT_VISITOR_ID_UUID, VALID_DAY_OF_YEAR, TicketType.HALF_DAY));

        // Verify the original ticket still exists by making a reservation
        attractionHandler.createAttraction(ATTRACTION_NAME, OPENING_TIME, CLOSING_TIME, SLOT_DURATION);
        attractionHandler.setSlotCapacityForAttraction(ATTRACTION_NAME, VALID_DAY_OF_YEAR, VALID_CAPACITY);

        MakeReservationResult result = attractionHandler.makeReservation(
                ATTRACTION_NAME, DEFAULT_VISITOR_ID_UUID, VALID_DAY_OF_YEAR, OPENING_TIME);

        assertTrue(result.isConfirmed());
    }

    @Test
    public void testAddTicketForOtherDay() {
        // Add a ticket for a different day
        attractionHandler.addTicket(DEFAULT_VISITOR_ID_UUID, OTHER_VALID_DAY_OF_YEAR, TicketType.FULL_DAY);

        // Try to add the same ticket again
        assertThrows(TicketAlreadyExistsException.class, () ->
                attractionHandler.addTicket(DEFAULT_VISITOR_ID_UUID, OTHER_VALID_DAY_OF_YEAR, TicketType.FULL_DAY));

        // Set up for reservation test
        attractionHandler.createAttraction(ATTRACTION_NAME, OPENING_TIME, CLOSING_TIME, SLOT_DURATION);
        attractionHandler.setSlotCapacityForAttraction(ATTRACTION_NAME, OTHER_VALID_DAY_OF_YEAR, VALID_CAPACITY);

        // Verify the ticket for OTHER_VALID_DAY_OF_YEAR exists by making a reservation
        MakeReservationResult result = attractionHandler.makeReservation(
                ATTRACTION_NAME, DEFAULT_VISITOR_ID_UUID, OTHER_VALID_DAY_OF_YEAR, OPENING_TIME);

        assertTrue(result.isConfirmed());

        // Verify that there's no ticket for VALID_DAY_OF_YEAR
        assertThrows(MissingPassException.class, () ->
                attractionHandler.makeReservation(ATTRACTION_NAME, DEFAULT_VISITOR_ID_UUID, VALID_DAY_OF_YEAR, OPENING_TIME));
    }

    @Test
    public void testAddCapacityFailureAttractionDoesNotExist() {
        assertThrows(AttractionNotFoundException.class, () ->
                attractionHandler.setSlotCapacityForAttraction(ATTRACTION_NAME, VALID_DAY_OF_YEAR, VALID_CAPACITY));
    }

    @Test
    public void testAddCapacityFailureCapacityAlreadySet() {
        attractionHandler.createAttraction(ATTRACTION_NAME, OPENING_TIME, CLOSING_TIME, SLOT_DURATION);

        // Set capacity first time
        DefineSlotCapacityResult result = attractionHandler.setSlotCapacityForAttraction(
                ATTRACTION_NAME, VALID_DAY_OF_YEAR, VALID_CAPACITY);

        assertEquals(0, result.bookingsConfirmed());

        // Try to set capacity again
        assertThrows(CapacityAlreadyDefinedException.class, () ->
                attractionHandler.setSlotCapacityForAttraction(ATTRACTION_NAME, VALID_DAY_OF_YEAR, VALID_CAPACITY));
    }

    @Test
    public void testAddCapacitySuccess() {
        attractionHandler.createAttraction(ATTRACTION_NAME, OPENING_TIME, CLOSING_TIME, SLOT_DURATION);

        DefineSlotCapacityResult result = attractionHandler.setSlotCapacityForAttraction(
                ATTRACTION_NAME, VALID_DAY_OF_YEAR, VALID_CAPACITY);

        // Verify the capacity was set correctly
        assertEquals(0, result.bookingsConfirmed());
        assertEquals(0, result.bookingsCancelled());
        assertEquals(0, result.bookingsRelocated());

        Attraction attraction = attractionHandler.getAttraction(ATTRACTION_NAME);
        assertEquals(VALID_CAPACITY, attraction.getReservationHandler(VALID_DAY_OF_YEAR).getSlotCapacity());
    }

    @Test
    public void testAddCapacityConfirmPendingRequests() {
        // For these complex reservation scenario tests, we need to work directly with ReservationHandler
        // since AttractionHandler doesn't expose methods to manipulate pending reservations

        // Create attraction with the handler
        attractionHandler.createAttraction(ATTRACTION_NAME, OPENING_TIME, CLOSING_TIME, SLOT_DURATION);
        Attraction attraction = attractionHandler.getAttraction(ATTRACTION_NAME);

        // Add tickets and make reservations (that will be pending since capacity isn't set)
        for (int i = 0; i < VALID_CAPACITY; i++) {
            UUID visitorId = UUID.randomUUID();
            attractionHandler.addTicket(visitorId, VALID_DAY_OF_YEAR, TicketType.UNLIMITED);
            attractionHandler.makeReservation(ATTRACTION_NAME, visitorId, VALID_DAY_OF_YEAR, OPENING_TIME);
        }

        // Now set capacity - this should confirm the pending reservations
        DefineSlotCapacityResult result = attractionHandler.setSlotCapacityForAttraction(
                ATTRACTION_NAME, VALID_DAY_OF_YEAR, VALID_CAPACITY);

        // Verify the capacity was set and reservations confirmed
        assertEquals(VALID_CAPACITY, result.bookingsConfirmed());
        assertEquals(0, result.bookingsCancelled());
        assertEquals(0, result.bookingsRelocated());
    }

    @Test
    public void testReservationAndCapacitySetting() {
        // Create the attraction first
        attractionHandler.createAttraction(ATTRACTION_NAME, OPENING_TIME, CLOSING_TIME, SLOT_DURATION);

        // Add tickets for visitors
        UUID visitorId1 = UUID.randomUUID();
        UUID visitorId2 = UUID.randomUUID();
        attractionHandler.addTicket(visitorId1, VALID_DAY_OF_YEAR, TicketType.UNLIMITED);
        attractionHandler.addTicket(visitorId2, VALID_DAY_OF_YEAR, TicketType.UNLIMITED);

        // Make reservations before capacity is set (should be pending)
        MakeReservationResult result1 = attractionHandler.makeReservation(
                ATTRACTION_NAME, visitorId1, VALID_DAY_OF_YEAR, OPENING_TIME);
        MakeReservationResult result2 = attractionHandler.makeReservation(
                ATTRACTION_NAME, visitorId2, VALID_DAY_OF_YEAR, OPENING_TIME);

        assertFalse(result1.isConfirmed());
        assertFalse(result2.isConfirmed());

        // Set capacity to 1 - this should confirm only one reservation
        DefineSlotCapacityResult capacityResult = attractionHandler.setSlotCapacityForAttraction(
                ATTRACTION_NAME, VALID_DAY_OF_YEAR, 1);

        // One booking should be confirmed, one should not (moved to another slot or cancelled)
        assertEquals(1, capacityResult.bookingsConfirmed());
        assertTrue(capacityResult.bookingsCancelled() + capacityResult.bookingsRelocated() > 0);
    }

    @Test
    public void testGetAttractionsWithOneAttraction() {
        attractionHandler.createAttraction(ATTRACTION_NAME, OPENING_TIME, CLOSING_TIME, SLOT_DURATION);

        List<Attraction> attractions = List.copyOf(attractionHandler.getAttractions());

        assertEquals(1, attractions.size());
        assertEquals(ATTRACTION_NAME, attractions.getFirst().name());
        assertEquals(OPENING_TIME, attractions.getFirst().openingTime());
        assertEquals(CLOSING_TIME, attractions.getFirst().closingTime());
        assertEquals(SLOT_DURATION, attractions.getFirst().slotDuration());
    }

    @Test
    public void testCheckAttractionAvailabilityFailureEmptyAttractionName() {
        assertThrows(AttractionNotFoundException.class, () -> {
            attractionHandler.getAttraction("");
        });
    }

    @Test
    public void testCheckAttractionAvailabilityFailureAnotherAttractionName() {
        attractionHandler.createAttraction(ATTRACTION_NAME, OPENING_TIME, CLOSING_TIME, SLOT_DURATION);

        assertThrows(AttractionNotFoundException.class, () -> {
            attractionHandler.getAttraction(ANOTHER_ATTRACTION_NAME);
        });
    }

    @Test
    public void testGetAvailabilityForAllAttractions() {
        attractionHandler.createAttraction(ATTRACTION_NAME, OPENING_TIME, CLOSING_TIME, SLOT_DURATION);

        LocalTime slotFrom = LocalTime.of(10, 0);
        LocalTime slotTo = LocalTime.of(10, SLOT_DURATION - 1);

        List<AttractionAvailabilityResult> results = new ArrayList<>(attractionHandler.getAvailabilityForAllAttractions(VALID_DAY_OF_YEAR, slotFrom, slotTo));
        results.sort(Comparator.comparing(AttractionAvailabilityResult::attractionName));

        assertEquals(1, results.size());
        assertEquals(ATTRACTION_NAME, results.getFirst().attractionName());
        assertEquals(slotFrom, results.getFirst().slotTime());
    }

    @Test
    public void testCheckAvailabilityFailureForNonExistingAttraction() {
        LocalTime slotFrom = LocalTime.of(10, 0);
        LocalTime slotTo = LocalTime.of(10, SLOT_DURATION - 1);

        Collection<AttractionAvailabilityResult> results = attractionHandler.getAvailabilityForAllAttractions(VALID_DAY_OF_YEAR, slotFrom, slotTo);

        assertTrue(results.isEmpty());
    }

    @Test
    public void testCheckAvailabilityOneAttractionUniqueSlot() {
        attractionHandler.createAttraction(ATTRACTION_NAME, OPENING_TIME, CLOSING_TIME, SLOT_DURATION);

        LocalTime slotFrom = LocalTime.of(10, 0);
        LocalTime slotTo = LocalTime.of(10, 5);

        List<AttractionAvailabilityResult> results = new ArrayList<>(attractionHandler.getAvailabilityForAttraction(ATTRACTION_NAME, VALID_DAY_OF_YEAR, slotFrom, slotTo));

        assertEquals(1, results.size());
        assertEquals(ATTRACTION_NAME, results.getFirst().attractionName());
        assertEquals(OPENING_TIME, results.getFirst().slotTime());
        assertEquals(-1, results.getFirst().slotCapacity());
        assertEquals(0, results.getFirst().confirmedReservations());
        assertEquals(0, results.getFirst().pendingReservations());
    }

    @Test
    public void testCheckAvailabilityOneAttraction2() {
        attractionHandler.createAttraction(ATTRACTION_NAME, OPENING_TIME, CLOSING_TIME, SLOT_DURATION);

        LocalTime slotFrom = LocalTime.of(10, 10);
        LocalTime slotTo = LocalTime.of(11, 25);

        List<AttractionAvailabilityResult> results = new ArrayList<>(attractionHandler.getAvailabilityForAttraction(ATTRACTION_NAME, VALID_DAY_OF_YEAR, slotFrom, slotTo));

        assertEquals(8, results.size());

        for (int i = 0; i < 8; i++) {
            assertEquals(ATTRACTION_NAME, results.get(i).attractionName());
            assertEquals(LocalTime.of(10, 10).plusMinutes(10 * i), results.get(i).slotTime());
            assertEquals(-1, results.get(i).slotCapacity());
            assertEquals(0, results.get(i).confirmedReservations());
            assertEquals(0, results.get(i).pendingReservations());
        }
    }

    @Test
    public void testCheckAvailabilityOneAttraction3() {
        attractionHandler.createAttraction(ATTRACTION_NAME, OPENING_TIME, CLOSING_TIME, SLOT_DURATION);

        LocalTime slotFrom = LocalTime.of(9, 10);
        LocalTime slotTo = LocalTime.of(11, 0);

        List<AttractionAvailabilityResult> results = new ArrayList<>(attractionHandler.getAvailabilityForAttraction(ATTRACTION_NAME, VALID_DAY_OF_YEAR, slotFrom, slotTo));

        assertEquals(7, results.size());
        for (int i = 0; i < 7; i++) {
            assertEquals(ATTRACTION_NAME, results.get(i).attractionName());
            assertEquals(LocalTime.of(10, 0).plusMinutes(10 * i), results.get(i).slotTime());
            assertEquals(-1, results.get(i).slotCapacity());
            assertEquals(0, results.get(i).confirmedReservations());
            assertEquals(0, results.get(i).pendingReservations());
        }
    }

    @Test
    public void testCheckAvailabilityOneAttraction4() {
        attractionHandler.createAttraction(ATTRACTION_NAME, OPENING_TIME, CLOSING_TIME, SLOT_DURATION);

        LocalTime slotFrom = LocalTime.of(16, 17);
        LocalTime slotTo = LocalTime.of(18, 0);

        List<AttractionAvailabilityResult> results = new ArrayList<>(attractionHandler.getAvailabilityForAttraction(ATTRACTION_NAME, VALID_DAY_OF_YEAR, slotFrom, slotTo));

        assertEquals(10, results.size());
        for (int i = 0; i < 10; i++) {
            assertEquals(ATTRACTION_NAME, results.get(i).attractionName());
            assertEquals(LocalTime.of(16, 20).plusMinutes(10 * i), results.get(i).slotTime());
            assertEquals(-1, results.get(i).slotCapacity());
            assertEquals(0, results.get(i).confirmedReservations());
            assertEquals(0, results.get(i).pendingReservations());
        }
    }

    @Test
    public void testCheckAvailabilityOneAttraction5() {
        attractionHandler.createAttraction(ATTRACTION_NAME, OPENING_TIME, CLOSING_TIME, SLOT_DURATION);

        LocalTime slotFrom = LocalTime.of(16, 17);
        LocalTime slotTo = LocalTime.of(18, 48);

        List<AttractionAvailabilityResult> results = new ArrayList<>(attractionHandler.getAvailabilityForAttraction(ATTRACTION_NAME, VALID_DAY_OF_YEAR, slotFrom, slotTo));

        assertEquals(10, results.size());
        for (int i = 0; i < 10; i++) {
            assertEquals(ATTRACTION_NAME, results.get(i).attractionName());
            assertEquals(LocalTime.of(16, 20).plusMinutes(10 * i), results.get(i).slotTime());
            assertEquals(-1, results.get(i).slotCapacity());
            assertEquals(0, results.get(i).confirmedReservations());
            assertEquals(0, results.get(i).pendingReservations());
        }
    }

    @Test
    public void testCheckAvailabilityForAllAttractions() {
        attractionHandler.createAttraction(ATTRACTION_NAME, OPENING_TIME, CLOSING_TIME, SLOT_DURATION);
        attractionHandler.createAttraction(ANOTHER_ATTRACTION_NAME, OPENING_TIME, CLOSING_TIME, SLOT_DURATION);

        LocalTime slotFrom = LocalTime.of(10, 0);
        LocalTime slotTo = LocalTime.of(18, 0);

        List<AttractionAvailabilityResult> results = new ArrayList<>(attractionHandler.getAvailabilityForAllAttractions(VALID_DAY_OF_YEAR, slotFrom, slotTo));

        // Verify the result
        assertEquals(2 * TOTAL_SLOTS, results.size());

        // Convert results to a list for easier verification
        List<AttractionAvailabilityResult> resultsList = new ArrayList<>(results);

        // Verify the first attraction's results
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            AttractionAvailabilityResult result = resultsList.get(i);
            assertEquals(NO_SLOT_CAPACITY, result.slotCapacity());
            assertEquals(ATTRACTION_NAME, result.attractionName());
            assertEquals(0, result.confirmedReservations());
            assertEquals(0, result.pendingReservations());
            assertEquals(OPENING_TIME.plusMinutes((long) i * SLOT_DURATION), result.slotTime());
        }

        // Verify the second attraction's results
        for (int i = TOTAL_SLOTS; i < 2 * TOTAL_SLOTS; i++) {
            AttractionAvailabilityResult result = resultsList.get(i);
            assertEquals(NO_SLOT_CAPACITY, result.slotCapacity());
            assertEquals(ANOTHER_ATTRACTION_NAME, result.attractionName());
            assertEquals(0, result.confirmedReservations());
            assertEquals(0, result.pendingReservations());
            assertEquals(OPENING_TIME.plusMinutes((long) (i - TOTAL_SLOTS) * SLOT_DURATION), result.slotTime());
        }
    }

    @Test
    public void testConfirmReservationFailureNoTicket() {
        assertThrows(MissingPassException.class, () -> {
            // Call the method directly on attractionHandler
            attractionHandler.confirmReservation(
                    "",
                    VISITOR_ID,
                    VALID_DAY_OF_YEAR,
                    OPENING_TIME
            );
        });
    }

    @Test
    public void testConfirmReservationFailureEmptyAttractionName() {
        attractionHandler.addTicket(VISITOR_ID, VALID_DAY_OF_YEAR, TICKET_TYPE_FULL_DAY);

        assertThrows(AttractionNotFoundException.class, () -> {
            // Call the method directly on attractionHandler
            attractionHandler.confirmReservation(
                    "",
                    VISITOR_ID,
                    VALID_DAY_OF_YEAR,
                    OPENING_TIME
            );
        });
    }

    @Test
    public void testConfirmReservationFailureBlankAttractionName() {
        attractionHandler.addTicket(VISITOR_ID, VALID_DAY_OF_YEAR, TICKET_TYPE_FULL_DAY);
        attractionHandler.createAttraction(ATTRACTION_NAME, OPENING_TIME, CLOSING_TIME, SLOT_DURATION);

        assertThrows(AttractionNotFoundException.class, () -> {
            // Call the method directly on attractionHandler
            attractionHandler.confirmReservation(
                    ANOTHER_ATTRACTION_NAME,
                    VISITOR_ID,
                    VALID_DAY_OF_YEAR,
                    OPENING_TIME
            );
        });
    }

    @Test
    public void testConfirmReservationFailureNoCapacityDefined() {
        attractionHandler.addTicket(VISITOR_ID, VALID_DAY_OF_YEAR, TICKET_TYPE_FULL_DAY);
        attractionHandler.createAttraction(ATTRACTION_NAME, OPENING_TIME, CLOSING_TIME, SLOT_DURATION);

        assertThrows(CapacityNotDefinedException.class, () -> {
            attractionHandler.confirmReservation(
                    ATTRACTION_NAME,
                    VISITOR_ID,
                    VALID_DAY_OF_YEAR,
                    OPENING_TIME
            );
        });
    }

    @Test
    public void testConfirmReservationFailureReservationNotFound() {
        attractionHandler.addTicket(VISITOR_ID, VALID_DAY_OF_YEAR, TICKET_TYPE_FULL_DAY);
        attractionHandler.createAttraction(ATTRACTION_NAME, OPENING_TIME, CLOSING_TIME, SLOT_DURATION);
        attractionHandler.setSlotCapacityForAttraction(ATTRACTION_NAME, VALID_DAY_OF_YEAR, 10);

        assertThrows(ReservationNotFoundException.class, () -> {
            attractionHandler.confirmReservation(
                    ATTRACTION_NAME,
                    VISITOR_ID,
                    VALID_DAY_OF_YEAR,
                    OPENING_TIME
            );
        });
    }

    @Test
    public void testConfirmReservationFailureReservationAlreadyConfirmed() {
        attractionHandler.addTicket(VISITOR_ID, VALID_DAY_OF_YEAR, TICKET_TYPE_FULL_DAY);
        attractionHandler.createAttraction(ATTRACTION_NAME, OPENING_TIME, CLOSING_TIME, SLOT_DURATION);
        attractionHandler.setSlotCapacityForAttraction(ATTRACTION_NAME, VALID_DAY_OF_YEAR, 10);

        attractionHandler.makeReservation(ATTRACTION_NAME, VISITOR_ID, VALID_DAY_OF_YEAR, OPENING_TIME);

        assertThrows(ReservationAlreadyConfirmedException.class, () -> {
            attractionHandler.confirmReservation(
                    ATTRACTION_NAME,
                    VISITOR_ID,
                    VALID_DAY_OF_YEAR,
                    OPENING_TIME
            );
        });
    }

    @Test
    public void testConfirmReservationFailureCantBookWithHalfDayPass() {
        attractionHandler.addTicket(VISITOR_ID, VALID_DAY_OF_YEAR, TICKET_TYPE_HALF_DAY);
        attractionHandler.createAttraction(ATTRACTION_NAME, OPENING_TIME, CLOSING_TIME, SLOT_DURATION);
        attractionHandler.setSlotCapacityForAttraction(ATTRACTION_NAME, VALID_DAY_OF_YEAR, 10);

        assertThrows(MissingPassException.class, () -> {
            attractionHandler.confirmReservation(
                    ATTRACTION_NAME,
                    VISITOR_ID,
                    VALID_DAY_OF_YEAR,
                    LocalTime.parse(HALF_DAY_TIME_RESTRICTION_LIMIT)
            );
        });
    }

    @Test
    public void testCancelReservationFailureNoAttraction() {
        attractionHandler.addTicket(VISITOR_ID, VALID_DAY_OF_YEAR, TICKET_TYPE_FULL_DAY);
        attractionHandler.createAttraction(ATTRACTION_NAME, OPENING_TIME, CLOSING_TIME, SLOT_DURATION);
        attractionHandler.setSlotCapacityForAttraction(ATTRACTION_NAME, VALID_DAY_OF_YEAR, 10);

        assertThrows(AttractionNotFoundException.class, () -> {
            attractionHandler.cancelReservation(
                    NON_EXISTING_ATTRACTION_NAME,
                    VISITOR_ID,
                    VALID_DAY_OF_YEAR,
                    OPENING_TIME
            );
        });
    }

    @Test
    public void testCancelReservationFailureNoPreviousReservationMade() {
        attractionHandler.addTicket(VISITOR_ID, VALID_DAY_OF_YEAR, TICKET_TYPE_FULL_DAY);
        attractionHandler.createAttraction(ATTRACTION_NAME, OPENING_TIME, CLOSING_TIME, SLOT_DURATION);
        attractionHandler.setSlotCapacityForAttraction(ATTRACTION_NAME, VALID_DAY_OF_YEAR, 10);

        assertThrows(ReservationNotFoundException.class, () -> {
            attractionHandler.cancelReservation(
                    ATTRACTION_NAME,
                    VISITOR_ID,
                    VALID_DAY_OF_YEAR,
                    OPENING_TIME
            );
        });
    }

    @Test
    public void testCancelReservationSuccess() {
        attractionHandler.addTicket(VISITOR_ID, VALID_DAY_OF_YEAR, TICKET_TYPE_FULL_DAY);
        attractionHandler.createAttraction(ATTRACTION_NAME, OPENING_TIME, CLOSING_TIME, SLOT_DURATION);
        attractionHandler.setSlotCapacityForAttraction(ATTRACTION_NAME, VALID_DAY_OF_YEAR, 10);

        attractionHandler.makeReservation(ATTRACTION_NAME, VISITOR_ID, VALID_DAY_OF_YEAR, OPENING_TIME);

        attractionHandler.cancelReservation(
                ATTRACTION_NAME,
                VISITOR_ID,
                VALID_DAY_OF_YEAR,
                OPENING_TIME
        );
    }

    @Test
    public void testReserveAttractionFailureNoTicket() {
        attractionHandler.createAttraction(ATTRACTION_NAME, OPENING_TIME, CLOSING_TIME, SLOT_DURATION);

        assertThrows(MissingPassException.class, () -> {
            attractionHandler.makeReservation(
                    ATTRACTION_NAME,
                    VISITOR_ID,
                    VALID_DAY_OF_YEAR,
                    OPENING_TIME
            );
        });
    }

    @Test
    public void testReserveAttractionFailureHalfDayPassRestriction() {
        attractionHandler.addTicket(VISITOR_ID, VALID_DAY_OF_YEAR, TICKET_TYPE_HALF_DAY);
        attractionHandler.createAttraction(ATTRACTION_NAME, LocalTime.of(15, 0), CLOSING_TIME, SLOT_DURATION);
        attractionHandler.setSlotCapacityForAttraction(ATTRACTION_NAME, VALID_DAY_OF_YEAR, 10);

        assertThrows(MissingPassException.class, () -> {
            attractionHandler.makeReservation(
                    ATTRACTION_NAME,
                    VISITOR_ID,
                    VALID_DAY_OF_YEAR,
                    LocalTime.parse(HALF_DAY_TIME_RESTRICTION_LIMIT)
            );
        });
    }

    @Test
    public void testReserveAttractionFailureFullDayPassRestriction() {
        attractionHandler.addTicket(VISITOR_ID, VALID_DAY_OF_YEAR, TICKET_TYPE_FULL_DAY);
        attractionHandler.createAttraction(ATTRACTION_NAME, OPENING_TIME, CLOSING_TIME, SLOT_DURATION);
        attractionHandler.setSlotCapacityForAttraction(ATTRACTION_NAME, VALID_DAY_OF_YEAR, 10);

        attractionHandler.makeReservation(ATTRACTION_NAME, VISITOR_ID, VALID_DAY_OF_YEAR, OPENING_TIME);

        assertThrows(ReservationAlreadyExistsException.class, () -> {
            attractionHandler.makeReservation(
                    ATTRACTION_NAME,
                    VISITOR_ID,
                    VALID_DAY_OF_YEAR,
                    OPENING_TIME
            );
        });
    }

    @Test
    public void testReserveAttractionSuccessNoCapacityDefined() {
        attractionHandler.addTicket(VISITOR_ID, VALID_DAY_OF_YEAR, TICKET_TYPE_FULL_DAY);
        attractionHandler.createAttraction(ATTRACTION_NAME, OPENING_TIME, CLOSING_TIME, SLOT_DURATION);

        MakeReservationResult result = attractionHandler.makeReservation(
                ATTRACTION_NAME,
                VISITOR_ID,
                VALID_DAY_OF_YEAR,
                OPENING_TIME
        );

        assertFalse(result.isConfirmed());
        assertEquals(ATTRACTION_NAME, result.reservation().attraction().name());
        assertEquals(VISITOR_ID, result.reservation().ticket().visitorId());
    }

    @Test
    public void testReserveAttractionSuccessCapacityDefined() {
        attractionHandler.addTicket(VISITOR_ID, VALID_DAY_OF_YEAR, TICKET_TYPE_FULL_DAY);
        attractionHandler.createAttraction(ATTRACTION_NAME, OPENING_TIME, CLOSING_TIME, SLOT_DURATION);
        attractionHandler.setSlotCapacityForAttraction(ATTRACTION_NAME, VALID_DAY_OF_YEAR, 10);

        MakeReservationResult result = attractionHandler.makeReservation(
                ATTRACTION_NAME,
                VISITOR_ID,
                VALID_DAY_OF_YEAR,
                OPENING_TIME
        );

        assertTrue(result.isConfirmed());
        assertEquals(ATTRACTION_NAME, result.reservation().attraction().name());
        assertEquals(VISITOR_ID, result.reservation().ticket().visitorId());
    }

    @Test
    public void testReserveAttractionFailureMaxCapacityReached() {
        attractionHandler.addTicket(VISITOR_ID, VALID_DAY_OF_YEAR, TICKET_TYPE_FULL_DAY);
        attractionHandler.addTicket(DEFAULT_VISITOR_ID_UUID, VALID_DAY_OF_YEAR, TICKET_TYPE_FULL_DAY);
        attractionHandler.createAttraction(ATTRACTION_NAME, OPENING_TIME, CLOSING_TIME, SLOT_DURATION);
        attractionHandler.setSlotCapacityForAttraction(ATTRACTION_NAME, VALID_DAY_OF_YEAR, 1);

        attractionHandler.makeReservation(ATTRACTION_NAME, DEFAULT_VISITOR_ID_UUID, VALID_DAY_OF_YEAR, OPENING_TIME);

        assertThrows(OutOfCapacityException.class, () -> {
            attractionHandler.makeReservation(
                    ATTRACTION_NAME,
                    VISITOR_ID,
                    VALID_DAY_OF_YEAR,
                    OPENING_TIME
            );
        });
    }
}
