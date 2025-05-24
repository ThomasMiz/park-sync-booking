package edu.itba.serverdps.services;

import edu.itba.serverdps.application.exceptions.*;
import edu.itba.serverdps.domain.model.*;
import edu.itba.serverdps.domain.usecase.handler.ReservationHandler;
import edu.itba.serverdps.domain.model.result.DefineSlotCapacityResult;
import edu.itba.serverdps.domain.model.result.MakeReservationResult;
import edu.itba.serverdps.domain.model.result.SuggestedCapacityResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalTime;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ReservationHandlerTest {
    private static final String ATTRACTION_NAME1 = "Fuerza Electromagnética";
    private static final LocalTime ATTRACTION_OPENING_TIME1 = LocalTime.of(10, 0);
    private static final LocalTime ATTRACTION_CLOSING_TIME1 = LocalTime.of(16, 0);
    private static final LocalTime INVALID_TIME1 = LocalTime.of(16, 0);
    private static final int ATTRACTION_SLOT_DURATION1 = 50;
    private static final int DAY_OF_YEAR = 69;
    private static final LocalTime[] VALID_TIME_SLOTS1 = new LocalTime[]{
            LocalTime.of(10, 0), LocalTime.of(10, 50), LocalTime.of(11, 40),
            LocalTime.of(12, 30), LocalTime.of(13, 20), LocalTime.of(14, 10),
            LocalTime.of(15, 0), LocalTime.of(15, 50)
    };

    private static final String ATTRACTION_NAME2 = "Fuerza Gravitatoria";
    private static final LocalTime ATTRACTION_OPENING_TIME2 = LocalTime.of(4, 0);
    private static final LocalTime ATTRACTION_CLOSING_TIME2 = LocalTime.of(10, 0);
    private static final LocalTime INVALID_TIME2 = LocalTime.of(10, 0);
    private static final int ATTRACTION_SLOT_DURATION2 = 30;
    private static final int ATTRACTION_SLOT_CAPACITY2 = 15;
    private static final LocalTime[] VALID_TIME_SLOTS2 = new LocalTime[]{
            LocalTime.of(4, 0), LocalTime.of(4, 30), LocalTime.of(5, 0),
            LocalTime.of(5, 30), LocalTime.of(6, 0), LocalTime.of(6, 30),
            LocalTime.of(7, 0), LocalTime.of(7, 30), LocalTime.of(8, 0),
            LocalTime.of(8, 30), LocalTime.of(9, 0), LocalTime.of(9, 30)
    };

    private final static UUID USER1 = UUID.fromString("00000000-7dec-11d0-a765-00a0c91e6bf6");
    private final static UUID USER2 = UUID.fromString("11111111-7dec-11d0-a765-00a0c91e6bf6");
    private final static UUID USER3 = UUID.fromString("22222222-7dec-11d0-a765-00a0c91e6bf6");
    private final static UUID USER4 = UUID.fromString("33333333-7dec-11d0-a765-00a0c91e6bf6");
    private final static UUID USER5 = UUID.fromString("44444444-7dec-11d0-a765-00a0c91e6bf6");
    private final static UUID USER6 = UUID.fromString("55555555-7dec-11d0-a765-00a0c91e6bf6");

    private final static Ticket TICKET1 = new Ticket(USER1, DAY_OF_YEAR, TicketType.HALF_DAY);

    private final static Ticket TICKET2 = new Ticket(USER2, DAY_OF_YEAR, TicketType.FULL_DAY);
    private final Map<UUID, ConfirmedReservation>[] slotConfirmedRequests1 = new Map[VALID_TIME_SLOTS1.length];
    private final LinkedHashMap<UUID, Reservation>[] slotPendingRequests1 = new LinkedHashMap[VALID_TIME_SLOTS1.length];
    private final Map<UUID, ConfirmedReservation>[] slotConfirmedRequests2 = new Map[VALID_TIME_SLOTS2.length];
    private final LinkedHashMap<UUID, Reservation>[] slotPendingRequests2 = new LinkedHashMap[VALID_TIME_SLOTS2.length];
    @Mock
    private Attraction attraction1;
    @Mock
    private Attraction attraction2;
    private ReservationHandler reservationHandler1;
    private ReservationHandler reservationHandler2;

    @Before
    public void setUp() {
        // when(attraction1.getName()).thenReturn(ATTRACTION_NAME1);
        when(attraction1.getOpeningTime()).thenReturn(ATTRACTION_OPENING_TIME1);
        when(attraction1.getClosingTime()).thenReturn(ATTRACTION_CLOSING_TIME1);
        when(attraction1.getSlotDuration()).thenReturn(ATTRACTION_SLOT_DURATION1);
        reservationHandler1 = new ReservationHandler(attraction1, DAY_OF_YEAR, null, -1, slotConfirmedRequests1, slotPendingRequests1);

        // when(attraction2.getName()).thenReturn(ATTRACTION_NAME2);
        when(attraction2.getOpeningTime()).thenReturn(ATTRACTION_OPENING_TIME2);
        when(attraction2.getClosingTime()).thenReturn(ATTRACTION_CLOSING_TIME2);
        when(attraction2.getSlotDuration()).thenReturn(ATTRACTION_SLOT_DURATION2);
        reservationHandler2 = new ReservationHandler(attraction2, DAY_OF_YEAR, null, ATTRACTION_SLOT_CAPACITY2, slotConfirmedRequests2, slotPendingRequests2);
    }

    @Test
    public void testSlotCountIncludesLastSlot() {
        assertEquals(VALID_TIME_SLOTS1.length, reservationHandler1.getSlotCount());
    }

    @Test
    public void testValidTimeSlotsAreValid() {
        for (LocalTime validTimeSlot : VALID_TIME_SLOTS1)
            assertTrue(reservationHandler1.isSlotTimeValid(validTimeSlot));
    }

    @Test
    public void testInvalidTimeSlotsAreInvalid() {
        int openingMinuteOfDay = ATTRACTION_OPENING_TIME1.toSecondOfDay() / 60;
        int closingMinuteOfDay = ATTRACTION_CLOSING_TIME1.toSecondOfDay() / 60;
        for (int i = openingMinuteOfDay; i < closingMinuteOfDay; i++)
            if ((i - openingMinuteOfDay) % ATTRACTION_SLOT_DURATION1 != 0)
                assertFalse(reservationHandler1.isSlotTimeValid(LocalTime.ofSecondOfDay(i * 60L)));
    }

    @Test
    public void testTimeSlotsBeforeOpeningAreInvalid() {
        int openingMinuteOfDay = ATTRACTION_OPENING_TIME1.toSecondOfDay() / 60;
        for (int i = 0; i < openingMinuteOfDay; i++)
            assertFalse(reservationHandler1.isSlotTimeValid(LocalTime.ofSecondOfDay(i * 60L)));
    }

    @Test
    public void testTimeSlotsAfterClosingAreInvalid() {
        int closingMinuteOfDay = ATTRACTION_CLOSING_TIME1.toSecondOfDay() / 60;
        int minutesInDay = 24 * 60;
        for (int i = closingMinuteOfDay; i < minutesInDay; i++)
            assertFalse(reservationHandler1.isSlotTimeValid(LocalTime.ofSecondOfDay(i * 60L)));
    }

    @Test
    public void testClosingTimeSlotIsInvalidExact() {
        assertFalse(reservationHandler2.isSlotTimeValid(ATTRACTION_CLOSING_TIME2));
    }

    @Test(expected = InvalidSlotException.class)
    public void testMakeReservationWithInvalidTime() {
        reservationHandler1.makeReservation(TICKET1, INVALID_TIME1);
    }

    @Test
    public void testMakeReservationSlotCapacityUndefined() {
        MakeReservationResult result = reservationHandler1.makeReservation(TICKET1, VALID_TIME_SLOTS1[3]);

        assertFalse(result.isConfirmed());
        assertEquals(attraction1, result.reservation().attraction());
        assertEquals(DAY_OF_YEAR, result.reservation().dayOfYear());
        assertEquals(TICKET1, result.reservation().ticket());
        assertTrue(slotConfirmedRequests1[3] == null || slotConfirmedRequests1[3].isEmpty());
        assertEquals(slotPendingRequests1[3].get(TICKET1.visitorId()), result.reservation());
    }

    @Test
    public void testMakeReservationSlotCapacityUndefinedExisting() {
        slotPendingRequests1[3] = new LinkedHashMap<>();
        Reservation existingReservation = new Reservation(TICKET1, attraction1);
        slotPendingRequests1[3].put(TICKET1.visitorId(), existingReservation);

        assertThrows(
                ReservationAlreadyExistsException.class,
                () -> reservationHandler1.makeReservation(TICKET1, VALID_TIME_SLOTS1[3])
        );

        assertTrue(slotConfirmedRequests1[3] == null || slotConfirmedRequests1[3].isEmpty());
        assertSame(existingReservation, slotPendingRequests1[3].get(TICKET1.visitorId()));
    }

    @Test
    public void testMakeReservationSlotCapacityDefined() {
        MakeReservationResult result = reservationHandler2.makeReservation(TICKET2, VALID_TIME_SLOTS2[3]);

        assertTrue(result.isConfirmed());
        assertEquals(attraction2, result.reservation().attraction());
        assertEquals(DAY_OF_YEAR, result.reservation().dayOfYear());
        assertEquals(TICKET2, result.reservation().ticket());
        assertTrue(slotPendingRequests2[3] == null || slotPendingRequests2[3].isEmpty());
        assertEquals(slotConfirmedRequests2[3].get(TICKET2.visitorId()).reservation(), result.reservation());
    }

    @Test
    public void testMakeReservationSlotCapacityDefinedExistingPending() {
        slotPendingRequests2[3] = new LinkedHashMap<>();
        Reservation existingReservation = new Reservation(TICKET2, attraction2);
        slotPendingRequests2[3].put(TICKET2.visitorId(), existingReservation);

        assertThrows(
                ReservationAlreadyExistsException.class,
                () -> reservationHandler2.makeReservation(TICKET2, VALID_TIME_SLOTS2[3])
        );

        assertTrue(slotConfirmedRequests2[3] == null || slotConfirmedRequests2[3].isEmpty());
        assertSame(existingReservation, slotPendingRequests2[3].get(TICKET2.visitorId()));
    }

    @Test
    public void testMakeReservationSlotCapacityDefinedExistingConfirmed() {
        slotConfirmedRequests2[3] = new HashMap<>();
        ConfirmedReservation existingReservation = new ConfirmedReservation(TICKET2, attraction2, VALID_TIME_SLOTS2[3]);
        slotConfirmedRequests2[3].put(TICKET2.visitorId(), existingReservation);

        assertThrows(
                ReservationAlreadyExistsException.class,
                () -> reservationHandler2.makeReservation(TICKET2, VALID_TIME_SLOTS2[3])
        );

        assertTrue(slotPendingRequests2[3] == null || slotPendingRequests2[3].isEmpty());
        assertSame(existingReservation, slotConfirmedRequests2[3].get(TICKET2.visitorId()));
    }

    @Test(expected = CapacityNotDefinedException.class)
    public void testConfirmReservationBeforeSlotCapacityDefined() {
        reservationHandler1.confirmReservation(TICKET1.visitorId(), VALID_TIME_SLOTS1[3]);
    }

    @Test
    public void testConfirmReservationWhenPending() {
        slotPendingRequests2[3] = new LinkedHashMap<>();
        Reservation existingReservation = new Reservation(TICKET2, attraction2);
        slotPendingRequests2[3].put(TICKET2.visitorId(), existingReservation);

        reservationHandler2.confirmReservation(TICKET2.visitorId(), VALID_TIME_SLOTS2[3]);

        assertTrue(slotPendingRequests2[3] == null || slotPendingRequests2[3].isEmpty());
        assertSame(existingReservation.ticket(), slotConfirmedRequests2[3].get(TICKET2.visitorId()).ticket());
        assertSame(existingReservation.attraction(), slotConfirmedRequests2[3].get(TICKET2.visitorId()).attraction());
    }

    @Test(expected = ReservationNotFoundException.class)
    public void testConfirmReservationWhenNonExisting() {
        reservationHandler2.confirmReservation(TICKET2.visitorId(), VALID_TIME_SLOTS2[3]);
    }

    @Test(expected = ReservationAlreadyConfirmedException.class)
    public void testConfirmReservationWhenAlreadyConfirmed() {
        slotConfirmedRequests2[3] = new HashMap<>();
        ConfirmedReservation existingReservation = new ConfirmedReservation(TICKET2, attraction2, VALID_TIME_SLOTS2[3]);
        slotConfirmedRequests2[3].put(TICKET2.visitorId(), existingReservation);

        reservationHandler2.confirmReservation(TICKET2.visitorId(), VALID_TIME_SLOTS2[3]);
    }

    @Test
    public void testCancelReservationWhenPending() {
        slotPendingRequests2[3] = new LinkedHashMap<>();
        Reservation existingReservation = new Reservation(TICKET2, attraction2);
        slotPendingRequests2[3].put(TICKET2.visitorId(), existingReservation);

        reservationHandler2.cancelReservation(TICKET2.visitorId(), VALID_TIME_SLOTS2[3]);

        assertTrue(slotPendingRequests2[3] == null || slotPendingRequests2[3].isEmpty());
        assertTrue(slotConfirmedRequests2[3] == null || slotConfirmedRequests2[3].isEmpty());
    }

    @Test(expected = ReservationNotFoundException.class)
    public void testCancelReservationWhenNonExisting() {
        reservationHandler1.cancelReservation(TICKET1.visitorId(), VALID_TIME_SLOTS1[3]);
    }

    @Test
    public void testCancelReservationWhenConfirmed() {
        slotConfirmedRequests1[3] = new HashMap<>();
        ConfirmedReservation existingReservation = new ConfirmedReservation(TICKET1, attraction1, VALID_TIME_SLOTS2[3]);
        slotConfirmedRequests1[3].put(TICKET1.visitorId(), existingReservation);

        reservationHandler1.cancelReservation(TICKET1.visitorId(), VALID_TIME_SLOTS1[3]);

        assertTrue(slotPendingRequests1[3] == null || slotPendingRequests1[3].isEmpty());
        assertTrue(slotConfirmedRequests1[3] == null || slotConfirmedRequests1[3].isEmpty());
    }

    @Test
    public void testSuggestedCapacitySlotCapacityAlreadyDefined() {

        SuggestedCapacityResult result = reservationHandler2.getSuggestedCapacity();

        assertNull(result);
    }

    private ReservationHandler createReservationHandlerWithPendingRequests(List<Integer> pendingReservationsPerSlot) {
        long user = 99999999;
        for (int i = 0; i < slotPendingRequests1.length && i < pendingReservationsPerSlot.size(); ++i) {
            slotPendingRequests1[i] = new LinkedHashMap<>();

            for (int j = 0; j < pendingReservationsPerSlot.get(i); j++) {
                UUID uuid = UUID.fromString(user-- + "-7dec-11d0-a765-00a0c91e6bf6");
                slotPendingRequests1[i].put(uuid, new Reservation(new Ticket(uuid, DAY_OF_YEAR, TicketType.FULL_DAY), attraction1));
            }
        }
        return new ReservationHandler(attraction1, DAY_OF_YEAR, null, -1, slotConfirmedRequests1, slotPendingRequests1);
    }

    private void checkConsistentState(Map<UUID, ConfirmedReservation>[] confirmed, LinkedHashMap<UUID, Reservation>[] pending, List<Integer> originalPendingNumbers, int capacity) {
        for (int i = 0; i < confirmed.length; ++i) {
            Map<UUID, ConfirmedReservation> currentConfirmed = confirmed[i];

            // Expected confirmed bookings
            int expectedSizeConfirmation = originalPendingNumbers.get(i) > capacity ? capacity : originalPendingNumbers.get(i);
            assertEquals(expectedSizeConfirmation, currentConfirmed.size());

            LinkedHashMap<UUID, Reservation> currentPendingList = pending[i];
            assertTrue(expectedSizeConfirmation + currentPendingList.size() <= capacity);
        }
    }

    @Test
    public void testSuggestedCapacityEqualFullySlots() {
        ReservationHandler reservationHandler = createReservationHandlerWithPendingRequests(Arrays.asList(3, 3, 3, 3, 3, 3, 3, 3));

        SuggestedCapacityResult result = reservationHandler.getSuggestedCapacity();

        assertEquals(3, result.maxPendingReservationCount());
        assertEquals(VALID_TIME_SLOTS1[0], result.slotTime());
    }

    @Test
    public void testSuggestedCapacityManySlots() {
        ReservationHandler reservationHandler = createReservationHandlerWithPendingRequests(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8));

        SuggestedCapacityResult result = reservationHandler.getSuggestedCapacity();

        assertEquals(8, result.maxPendingReservationCount());
        assertEquals(VALID_TIME_SLOTS1[7], result.slotTime());
    }

    @Test
    public void testSuggestedCapacityManySlots2() {
        ReservationHandler reservationHandler = createReservationHandlerWithPendingRequests(Arrays.asList(8, 7, 6, 5, 4, 3, 2, 1));

        SuggestedCapacityResult result = reservationHandler.getSuggestedCapacity();

        assertEquals(8, result.maxPendingReservationCount());
        assertEquals(VALID_TIME_SLOTS1[0], result.slotTime());
    }

    @Test
    public void testDefineSlotCapacityCapacityAlreadyDefined() {

        assertThrows(CapacityAlreadyDefinedException.class, () -> reservationHandler2.defineSlotCapacity(50));
    }

    @Test
    public void testDefineSlotCapacityAlgorithmAllFit() {
        int capacity = 10;
        List<Integer> pendingReservations = Arrays.asList(8, 7, 6, 5, 4, 3, 2, 1);
        ReservationHandler reservationHandler = createReservationHandlerWithPendingRequests(pendingReservations);

        DefineSlotCapacityResult result = reservationHandler.defineSlotCapacity(capacity);

        assertEquals(10, reservationHandler.getSlotCapacity());
        assertEquals(pendingReservations.stream().mapToInt(Integer::intValue).sum(), result.bookingsConfirmed());
        assertEquals(0, result.bookingsRelocated());
        assertEquals(0, result.bookingsCancelled());
        checkConsistentState(slotConfirmedRequests1, slotPendingRequests1, pendingReservations, capacity);
    }

    @Test
    public void testDefineSlotCapacityAlgorithmSomeRelocatedOneSlot() {
        int capacity = 10;
        List<Integer> pendingReservations = Arrays.asList(11, 7, 12, 5, 12, 3, 2, 1);
        ReservationHandler reservationHandler = createReservationHandlerWithPendingRequests(pendingReservations);

        DefineSlotCapacityResult result = reservationHandler.defineSlotCapacity(capacity);

        assertEquals(10, reservationHandler.getSlotCapacity());
        assertEquals(pendingReservations.stream().mapToInt((num) -> num < capacity ? num : capacity).sum(), result.bookingsConfirmed());
        assertEquals(pendingReservations.stream().mapToInt((num) -> num < capacity ? 0 : (num - capacity)).sum(), result.bookingsRelocated());
        assertEquals(0, result.bookingsCancelled());
        checkConsistentState(slotConfirmedRequests1, slotPendingRequests1, pendingReservations, capacity);
    }

    @Test
    public void testDefineSlotCapacityAlgorithmRelocateAllSlots() {
        int capacity = 10;
        List<Integer> pendingReservations = Arrays.asList(80, 0, 0, 0, 0, 0, 0, 0);
        ReservationHandler reservationHandler = createReservationHandlerWithPendingRequests(pendingReservations);

        DefineSlotCapacityResult result = reservationHandler.defineSlotCapacity(capacity);

        assertEquals(capacity, reservationHandler.getSlotCapacity());
        assertEquals(10, result.bookingsConfirmed());
        assertEquals(70, result.bookingsRelocated());
        assertEquals(0, result.bookingsCancelled());
        checkConsistentState(slotConfirmedRequests1, slotPendingRequests1, pendingReservations, capacity);
    }

    @Test
    public void testDefineSlotCapacityAlgorithmCancelLast() {
        int capacity = 10;
        List<Integer> pendingReservations = Arrays.asList(81, 0, 0, 0, 0, 0, 0, 0);
        ReservationHandler reservationHandler = createReservationHandlerWithPendingRequests(pendingReservations);

        DefineSlotCapacityResult result = reservationHandler.defineSlotCapacity(capacity);

        assertEquals(capacity, reservationHandler.getSlotCapacity());
        assertEquals(10, result.bookingsConfirmed());
        assertEquals(70, result.bookingsRelocated());
        assertEquals(1, result.bookingsCancelled());
        checkConsistentState(slotConfirmedRequests1, slotPendingRequests1, pendingReservations, capacity);
    }

    @Test
    public void testDefineSlotCapacityAlgorithmRelocationFromStart() {
        int capacity = 10;
        List<Integer> pendingReservations = Arrays.asList(51, 2, 5, 75, 8, 6, 0, 0);
        ReservationHandler reservationHandler = createReservationHandlerWithPendingRequests(pendingReservations);

        DefineSlotCapacityResult result = reservationHandler.defineSlotCapacity(capacity);

        assertEquals(capacity, reservationHandler.getSlotCapacity());
        assertEquals(pendingReservations.stream().mapToInt((num) -> num < capacity ? num : capacity).sum(), result.bookingsConfirmed());
        int cancelled = pendingReservations.stream().mapToInt(Integer::intValue).sum() - capacity * 8;
        assertEquals(cancelled, result.bookingsCancelled());
        assertEquals(pendingReservations.stream().mapToInt((num) -> num < capacity ? 0 : (num - capacity)).sum() - cancelled, result.bookingsRelocated());
        checkConsistentState(slotConfirmedRequests1, slotPendingRequests1, pendingReservations, capacity);
    }

    @Test
    public void testDefineSlotCapacityAlgorithmRelocationFromStart2() {
        int capacity = 10;
        List<Integer> pendingReservations = Arrays.asList(100, 0, 84, 1, 2, 5, 7, 5);
        ReservationHandler reservationHandler = createReservationHandlerWithPendingRequests(pendingReservations);

        DefineSlotCapacityResult result = reservationHandler.defineSlotCapacity(capacity);

        assertEquals(capacity, reservationHandler.getSlotCapacity());
        assertEquals(pendingReservations.stream().mapToInt((num) -> num < capacity ? num : capacity).sum(), result.bookingsConfirmed());
        int cancelled = pendingReservations.stream().mapToInt(Integer::intValue).sum() - capacity * 8;
        assertEquals(cancelled, result.bookingsCancelled());
        assertEquals(pendingReservations.stream().mapToInt((num) -> num < capacity ? 0 : (num - capacity)).sum() - cancelled, result.bookingsRelocated());
        checkConsistentState(slotConfirmedRequests1, slotPendingRequests1, pendingReservations, capacity);
    }
}
