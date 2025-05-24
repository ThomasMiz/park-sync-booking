package edu.itba.serverdps.domain.usecase.handler;

import edu.itba.serverdps.application.exceptions.*;
import edu.itba.serverdps.domain.model.*;
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

    @Mock
    private Attraction attraction1;
    @Mock
    private Attraction attraction2;

    private ReservationHandler reservationHandler1;
    private ReservationHandler reservationHandler2;

    @Before
    public void setUp() {
        // when(attraction1.getName()).thenReturn(ATTRACTION_NAME1);
        when(attraction1.openingTime()).thenReturn(ATTRACTION_OPENING_TIME1);
        when(attraction1.closingTime()).thenReturn(ATTRACTION_CLOSING_TIME1);
        when(attraction1.slotDuration()).thenReturn(ATTRACTION_SLOT_DURATION1);
        reservationHandler1 = new ReservationHandler(attraction1, DAY_OF_YEAR, null);

        // when(attraction2.getName()).thenReturn(ATTRACTION_NAME2);
        when(attraction2.openingTime()).thenReturn(ATTRACTION_OPENING_TIME2);
        when(attraction2.closingTime()).thenReturn(ATTRACTION_CLOSING_TIME2);
        when(attraction2.slotDuration()).thenReturn(ATTRACTION_SLOT_DURATION2);
        reservationHandler2 = new ReservationHandler(attraction2, DAY_OF_YEAR, null);
        reservationHandler2.defineSlotCapacity(ATTRACTION_SLOT_CAPACITY2);
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
        assertFalse(reservationHandler1.hasConfirmedReservation(TICKET1.visitorId(), VALID_TIME_SLOTS1[3]));
        assertEquals(reservationHandler1.getPendingReservation(TICKET1.visitorId(), VALID_TIME_SLOTS1[3]), result.reservation());
    }

    @Test
    public void testMakeReservationSlotCapacityUndefinedExisting() {
        Reservation existingReservation = new Reservation(TICKET1, attraction1);
        reservationHandler1.addPendingReservationForTesting(TICKET1.visitorId(), VALID_TIME_SLOTS1[3], existingReservation);

        assertThrows(
                ReservationAlreadyExistsException.class,
                () -> reservationHandler1.makeReservation(TICKET1, VALID_TIME_SLOTS1[3])
        );

        assertFalse(reservationHandler1.hasConfirmedReservation(TICKET1.visitorId(), VALID_TIME_SLOTS1[3]));
        assertSame(existingReservation, reservationHandler1.getPendingReservation(TICKET1.visitorId(), VALID_TIME_SLOTS1[3]));
    }

    @Test
    public void testMakeReservationSlotCapacityDefined() {
        MakeReservationResult result = reservationHandler2.makeReservation(TICKET2, VALID_TIME_SLOTS2[3]);

        assertTrue(result.isConfirmed());
        assertEquals(attraction2, result.reservation().attraction());
        assertEquals(DAY_OF_YEAR, result.reservation().dayOfYear());
        assertEquals(TICKET2, result.reservation().ticket());
        assertFalse(reservationHandler2.hasPendingReservation(TICKET2.visitorId(), VALID_TIME_SLOTS2[3]));
        assertEquals(reservationHandler2.getConfirmedReservation(TICKET2.visitorId(), VALID_TIME_SLOTS2[3]).reservation(), result.reservation());
    }

    @Test
    public void testMakeReservationSlotCapacityDefinedExistingPending() {
        Reservation existingReservation = new Reservation(TICKET2, attraction2);
        reservationHandler2.addPendingReservationForTesting(TICKET2.visitorId(), VALID_TIME_SLOTS2[3], existingReservation);

        assertThrows(
                ReservationAlreadyExistsException.class,
                () -> reservationHandler2.makeReservation(TICKET2, VALID_TIME_SLOTS2[3])
        );

        assertFalse(reservationHandler2.hasConfirmedReservation(TICKET2.visitorId(), VALID_TIME_SLOTS2[3]));
        assertSame(existingReservation, reservationHandler2.getPendingReservation(TICKET2.visitorId(), VALID_TIME_SLOTS2[3]));
    }

    @Test
    public void testMakeReservationSlotCapacityDefinedExistingConfirmed() {
        ConfirmedReservation existingReservation = new ConfirmedReservation(TICKET2, attraction2, VALID_TIME_SLOTS2[3]);
        reservationHandler2.addConfirmedReservationForTesting(TICKET2.visitorId(), VALID_TIME_SLOTS2[3], existingReservation);

        assertThrows(
                ReservationAlreadyExistsException.class,
                () -> reservationHandler2.makeReservation(TICKET2, VALID_TIME_SLOTS2[3])
        );

        assertFalse(reservationHandler2.hasPendingReservation(TICKET2.visitorId(), VALID_TIME_SLOTS2[3]));
        assertSame(existingReservation, reservationHandler2.getConfirmedReservation(TICKET2.visitorId(), VALID_TIME_SLOTS2[3]));
    }

    @Test(expected = CapacityNotDefinedException.class)
    public void testConfirmReservationBeforeSlotCapacityDefined() {
        reservationHandler1.confirmReservation(TICKET1.visitorId(), VALID_TIME_SLOTS1[3]);
    }

    @Test
    public void testConfirmReservationWhenPending() {
        Reservation existingReservation = new Reservation(TICKET2, attraction2);
        reservationHandler2.addPendingReservationForTesting(TICKET2.visitorId(), VALID_TIME_SLOTS2[3], existingReservation);

        reservationHandler2.confirmReservation(TICKET2.visitorId(), VALID_TIME_SLOTS2[3]);

        assertFalse(reservationHandler2.hasPendingReservation(TICKET2.visitorId(), VALID_TIME_SLOTS2[3]));
        ConfirmedReservation confirmedReservation = reservationHandler2.getConfirmedReservation(TICKET2.visitorId(), VALID_TIME_SLOTS2[3]);
        assertSame(existingReservation.ticket(), confirmedReservation.ticket());
        assertSame(existingReservation.attraction(), confirmedReservation.attraction());
    }

    @Test(expected = ReservationNotFoundException.class)
    public void testConfirmReservationWhenNonExisting() {
        reservationHandler2.confirmReservation(TICKET2.visitorId(), VALID_TIME_SLOTS2[3]);
    }

    @Test(expected = ReservationAlreadyConfirmedException.class)
    public void testConfirmReservationWhenAlreadyConfirmed() {
        ConfirmedReservation existingReservation = new ConfirmedReservation(TICKET2, attraction2, VALID_TIME_SLOTS2[3]);
        reservationHandler2.addConfirmedReservationForTesting(TICKET2.visitorId(), VALID_TIME_SLOTS2[3], existingReservation);

        reservationHandler2.confirmReservation(TICKET2.visitorId(), VALID_TIME_SLOTS2[3]);
    }

    @Test
    public void testCancelReservationWhenPending() {
        Reservation existingReservation = new Reservation(TICKET2, attraction2);
        reservationHandler2.addPendingReservationForTesting(TICKET2.visitorId(), VALID_TIME_SLOTS2[3], existingReservation);

        reservationHandler2.cancelReservation(TICKET2.visitorId(), VALID_TIME_SLOTS2[3]);

        assertFalse(reservationHandler2.hasPendingReservation(TICKET2.visitorId(), VALID_TIME_SLOTS2[3]));
        assertFalse(reservationHandler2.hasConfirmedReservation(TICKET2.visitorId(), VALID_TIME_SLOTS2[3]));
    }

    @Test(expected = ReservationNotFoundException.class)
    public void testCancelReservationWhenNonExisting() {
        reservationHandler1.cancelReservation(TICKET1.visitorId(), VALID_TIME_SLOTS1[3]);
    }

    @Test
    public void testCancelReservationWhenConfirmed() {
        ConfirmedReservation existingReservation = new ConfirmedReservation(TICKET1, attraction1, VALID_TIME_SLOTS1[3]);
        reservationHandler1.addConfirmedReservationForTesting(TICKET1.visitorId(), VALID_TIME_SLOTS1[3], existingReservation);

        reservationHandler1.cancelReservation(TICKET1.visitorId(), VALID_TIME_SLOTS1[3]);

        assertFalse(reservationHandler1.hasPendingReservation(TICKET1.visitorId(), VALID_TIME_SLOTS1[3]));
        assertFalse(reservationHandler1.hasConfirmedReservation(TICKET1.visitorId(), VALID_TIME_SLOTS1[3]));
    }

    @Test
    public void testSuggestedCapacitySlotCapacityAlreadyDefined() {

        SuggestedCapacityResult result = reservationHandler2.getSuggestedCapacity();

        assertNull(result);
    }

    private ReservationHandler createReservationHandlerWithPendingRequests(List<Integer> pendingReservationsPerSlot) {
        ReservationHandler handler = new ReservationHandler(attraction1, DAY_OF_YEAR, null);

        long user = 99999999;
        for (int i = 0; i < VALID_TIME_SLOTS1.length && i < pendingReservationsPerSlot.size(); ++i) {
            LocalTime slotTime = VALID_TIME_SLOTS1[i];

            for (int j = 0; j < pendingReservationsPerSlot.get(i); j++) {
                UUID uuid = UUID.fromString(user-- + "-7dec-11d0-a765-00a0c91e6bf6");
                Ticket ticket = new Ticket(uuid, DAY_OF_YEAR, TicketType.FULL_DAY);
                Reservation reservation = new Reservation(ticket, attraction1);
                handler.addPendingReservationForTesting(uuid, slotTime, reservation);
            }
        }
        return handler;
    }

    private void checkConsistentState(ReservationHandler handler, List<Integer> originalPendingNumbers, int capacity) {
        List<ConfirmedReservation> confirmedReservations = new ArrayList<>();
        handler.getConfirmedReservations(confirmedReservations);

        List<Reservation> pendingReservations = new ArrayList<>();
        handler.getPendingReservations(pendingReservations);

        // Count confirmed and pending reservations per slot
        Map<LocalTime, Integer> confirmedPerSlot = new HashMap<>();
        for (ConfirmedReservation reservation : confirmedReservations) {
            confirmedPerSlot.put(reservation.slotTime(), confirmedPerSlot.getOrDefault(reservation.slotTime(), 0) + 1);
        }

        Map<LocalTime, Integer> pendingPerSlot = new HashMap<>();
        for (Reservation reservation : pendingReservations) {
            // We don't have the slot time in the Reservation object, so we need to check all slots
            for (LocalTime slotTime : VALID_TIME_SLOTS1) {
                if (handler.hasPendingReservation(reservation.visitorId(), slotTime)) {
                    pendingPerSlot.put(slotTime, pendingPerSlot.getOrDefault(slotTime, 0) + 1);
                    break;
                }
            }
        }

        // Check that the number of confirmed reservations per slot is as expected
        for (int i = 0; i < VALID_TIME_SLOTS1.length && i < originalPendingNumbers.size(); ++i) {
            LocalTime slotTime = VALID_TIME_SLOTS1[i];
            int expectedConfirmed = originalPendingNumbers.get(i) > capacity ? capacity : originalPendingNumbers.get(i);
            int actualConfirmed = confirmedPerSlot.getOrDefault(slotTime, 0);
            assertEquals("Slot " + i + " (" + slotTime + ") has wrong number of confirmed reservations", expectedConfirmed, actualConfirmed);

            int actualPending = pendingPerSlot.getOrDefault(slotTime, 0);
            assertTrue("Slot " + i + " (" + slotTime + ") has too many reservations", expectedConfirmed + actualPending <= capacity);
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
        checkConsistentState(reservationHandler, pendingReservations, capacity);
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
        checkConsistentState(reservationHandler, pendingReservations, capacity);
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
        checkConsistentState(reservationHandler, pendingReservations, capacity);
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
        checkConsistentState(reservationHandler, pendingReservations, capacity);
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
        checkConsistentState(reservationHandler, pendingReservations, capacity);
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
        checkConsistentState(reservationHandler, pendingReservations, capacity);
    }
}
