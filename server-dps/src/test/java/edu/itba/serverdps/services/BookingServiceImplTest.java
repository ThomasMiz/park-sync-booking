package edu.itba.serverdps.services;

import com.google.protobuf.Empty;
import edu.itba.serverdps.adapter.driving.BookingServiceImpl;
import edu.itba.serverdps.application.exceptions.*;
import edu.itba.serverdps.domain.model.*;
import edu.itba.serverdps.port.driving.grpc.AvailabilityRequest;
import edu.itba.serverdps.port.driving.grpc.AvailabilityResponse;
import edu.itba.serverdps.port.driving.grpc.BookingRequest;
import edu.itba.serverdps.port.driving.grpc.GetAttractionsResponse;
import edu.itba.serverdps.domain.usecase.handler.AttractionHandler;
import edu.itba.serverdps.domain.usecase.handler.ReservationHandler;
import edu.itba.serverdps.domain.usecase.ReservationObserver;
import edu.itba.serverdps.application.utils.ParseUtils;
import io.grpc.stub.StreamObserver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class BookingServiceImplTest {
    private static final String ATTRACTION_NAME = "attractionName";
    private static final String ANOTHER_ATTRACTION_NAME = "anotherAttractionName";
    private static final String NON_EXISTING_ATTRACTION_NAME = "nonExistingAttractionName";
    private static final UUID VISITOR_ID = UUID.randomUUID();
    private static final String TIME_FROM_STRING = "10:00";
    private static final String TIME_TO_STRING = "18:00";
    private static final String HALF_DAY_TIME_RESTRICTION_LIMIT = "14:00";
    private static final LocalTime TIME_FROM_LOCAL_TIME = LocalTime.parse(TIME_FROM_STRING);
    private static final LocalTime TIME_TO_LOCAL_TIME = LocalTime.parse(TIME_TO_STRING);
    private static final int SLOT_DURATION_MINUTES = 30;
    private static final int TOTAL_SLOTS = (TIME_TO_LOCAL_TIME.toSecondOfDay() - TIME_FROM_LOCAL_TIME.toSecondOfDay()) / (SLOT_DURATION_MINUTES * 60);
    private static final int INVALID_DAY_OF_YEAR = 0;
    private static final int OTHER_INVALID_DAY_OF_YEAR = 366;
    private static final int VALID_DAY_OF_YEAR = 200;
    private static final int SLOT_CAPACITY = 6;
    private static final int NO_SLOT_CAPACITY = -1;
    private static final TicketType TICKET_TYPE_FULL_DAY = TicketType.FULL_DAY;
    private static final TicketType TICKET_TYPE_HALF_DAY = TicketType.HALF_DAY;
    private static final int MAX_BOOKINGS_FOR_FULL_DAY = 3;
    private static final ConcurrentMap<String, Attraction> attractions = new ConcurrentHashMap<>();
    private static final ConcurrentMap<UUID, Ticket>[] ticketsByDay = TestUtils.generateTicketsByDayMaps();
    @Mock
    private static StreamObserver<AvailabilityResponse> availabilityResponseObserver = Mockito.mock(StreamObserver.class);
    @Mock
    private static StreamObserver<GetAttractionsResponse> attractionResponseObserver = Mockito.mock(StreamObserver.class);
    private final AttractionHandler attractionHandler = new AttractionHandler(attractions, ticketsByDay);
    private final BookingServiceImpl bookingService = new BookingServiceImpl(attractionHandler);

    @Before
    public void setUp() {
        attractions.clear();
        for (int i = 0; i < ticketsByDay.length; i++)
            ticketsByDay[i].clear();
    }

    // https://stackoverflow.com/questions/49871975/how-to-test-and-mock-a-grpc-service-written-in-java-using-mockito
    @Test
    public void testGetAttractionsWithOneAttraction() {
        Attraction attraction = new Attraction(ATTRACTION_NAME, TIME_FROM_LOCAL_TIME, TIME_TO_LOCAL_TIME, SLOT_DURATION_MINUTES);
        attractions.put(ATTRACTION_NAME, attraction);

        bookingService.getAttractions(Empty.newBuilder().build(), attractionResponseObserver);

        // Capture onNext argument for examination
        ArgumentCaptor<GetAttractionsResponse> responseCaptor = ArgumentCaptor.forClass(GetAttractionsResponse.class);
        Mockito.verify(attractionResponseObserver).onNext(responseCaptor.capture());
        GetAttractionsResponse capturedResponse = responseCaptor.getValue();

        assertEquals(1, capturedResponse.getAttractionList().size());
    }

    @Test
    public void testGetAttractionsWithNoAttraction() {
        bookingService.getAttractions(Empty.newBuilder().build(), attractionResponseObserver);

        ArgumentCaptor<GetAttractionsResponse> responseCaptor = ArgumentCaptor.forClass(GetAttractionsResponse.class);
        Mockito.verify(attractionResponseObserver).onNext(responseCaptor.capture());
        GetAttractionsResponse capturedResponse = responseCaptor.getValue();

        assertEquals(0, capturedResponse.getAttractionList().size());
    }

    @Test
    public void testCheckAttractionAvailabilityFailureMoreThan365Days() {
        assertThrows(InvalidDayException.class, () -> {
            bookingService.checkAttractionAvailability(AvailabilityRequest.newBuilder()
                            .setDayOfYear(OTHER_INVALID_DAY_OF_YEAR)
                            .setSlotFrom(TIME_FROM_STRING)
                            .setSlotTo(TIME_TO_STRING)
                            .build(),
                    Mockito.mock(StreamObserver.class));
        });
    }

    @Test
    public void testCheckAttractionAvailabilityFailureLessThan1Day() {
        assertThrows(InvalidDayException.class, () -> {
            bookingService.checkAttractionAvailability(AvailabilityRequest.newBuilder()
                            .setDayOfYear(INVALID_DAY_OF_YEAR)
                            .setSlotFrom(TIME_FROM_STRING)
                            .setSlotTo(TIME_TO_STRING)
                            .build(),
                    Mockito.mock(StreamObserver.class));
        });
    }

    @Test
    public void testCheckAttractionAvailabilityFailureInvalidSlotRange() {
        assertThrows(InvalidSlotException.class, () -> {
            bookingService.checkAttractionAvailability(AvailabilityRequest.newBuilder()
                            .setDayOfYear(VALID_DAY_OF_YEAR)
                            .setSlotFrom(TIME_TO_STRING)
                            .setSlotTo(TIME_FROM_STRING)
                            .build(),
                    Mockito.mock(StreamObserver.class));
        });
    }

    @Test
    public void testCheckAttractionAvailabilityFailureEmptyAttractionNameAndSlotTo() {
        assertThrows(CheckAvailabilityInvalidArgumentException.class, () -> {
            bookingService.checkAttractionAvailability(AvailabilityRequest.newBuilder()
                            .setAttractionName("")
                            .setDayOfYear(VALID_DAY_OF_YEAR)
                            .setSlotFrom(TIME_TO_STRING)
                            .build(),
                    Mockito.mock(StreamObserver.class));
        });
    }

    @Test
    public void testCheckAttractionAvailabilityFailureBlankAttractionNameAndSlotTo() {
        assertThrows(CheckAvailabilityInvalidArgumentException.class, () -> {
            bookingService.checkAttractionAvailability(AvailabilityRequest.newBuilder()
                            .setAttractionName("  ")
                            .setDayOfYear(VALID_DAY_OF_YEAR)
                            .setSlotFrom(TIME_TO_STRING)
                            .build(),
                    Mockito.mock(StreamObserver.class));
        });
    }

    @Test
    public void testCheckAttractionAvailabilityFailureNoAttractionNameAndSlotTo() {
        assertThrows(CheckAvailabilityInvalidArgumentException.class, () -> {
            bookingService.checkAttractionAvailability(AvailabilityRequest.newBuilder()
                            .setDayOfYear(VALID_DAY_OF_YEAR)
                            .setSlotFrom(TIME_TO_STRING)
                            .build(),
                    Mockito.mock(StreamObserver.class));
        });
    }

    @Test
    public void testCheckAttractionAvailabilityFailureNoSlotFrom() {
        assertThrows(InvalidSlotException.class, () -> {
            bookingService.checkAttractionAvailability(AvailabilityRequest.newBuilder()
                            .setDayOfYear(VALID_DAY_OF_YEAR)
                            .setAttractionName(ATTRACTION_NAME)
                            .setSlotTo(TIME_TO_STRING)
                            .build(),
                    Mockito.mock(StreamObserver.class));
        });
    }

    @Test
    public void testCheckAttractionAvailabilityFailureNoDayOfYear() {
        assertThrows(InvalidDayException.class, () -> {
            bookingService.checkAttractionAvailability(AvailabilityRequest.newBuilder()
                            .setAttractionName(ATTRACTION_NAME)
                            .setSlotTo(TIME_TO_STRING)
                            .setSlotFrom(TIME_FROM_STRING)
                            .build(),
                    Mockito.mock(StreamObserver.class));
        });
    }

    @Test
    public void testCheckAvailabilityFailureForNonExistingAttraction() {
        assertThrows(AttractionNotFoundException.class, () -> {
            bookingService.checkAttractionAvailability(AvailabilityRequest.newBuilder()
                            .setDayOfYear(VALID_DAY_OF_YEAR)
                            .setSlotFrom(TIME_FROM_STRING)
                            .setSlotTo(TIME_TO_STRING)
                            .setAttractionName(NON_EXISTING_ATTRACTION_NAME)
                            .build(),
                    Mockito.mock(StreamObserver.class));
        });
    }

    @Test
    public void testCheckAvailabilityForNoAttractions() {
        bookingService.checkAttractionAvailability(AvailabilityRequest.newBuilder()
                        .setDayOfYear(VALID_DAY_OF_YEAR)
                        .setSlotFrom(TIME_FROM_STRING)
                        .setSlotTo(TIME_TO_STRING)
                        .build(),
                availabilityResponseObserver);

        ArgumentCaptor<AvailabilityResponse> responseCaptor = ArgumentCaptor.forClass(AvailabilityResponse.class);
        Mockito.verify(availabilityResponseObserver).onNext(responseCaptor.capture());
        AvailabilityResponse capturedResponse = responseCaptor.getValue();

        assertEquals(0, capturedResponse.getSlotList().size());
    }

    @Test
    public void testCheckAvailabilityOneAttractionUniqueSlot() {
        Attraction attraction = new Attraction(ATTRACTION_NAME, TIME_FROM_LOCAL_TIME, TIME_TO_LOCAL_TIME, SLOT_DURATION_MINUTES);
        attractions.put(ATTRACTION_NAME, attraction);

        bookingService.checkAttractionAvailability(AvailabilityRequest.newBuilder()
                        .setDayOfYear(VALID_DAY_OF_YEAR)
                        .setSlotFrom(TIME_FROM_STRING)
                        .setSlotTo(ParseUtils.formatTime(TIME_FROM_LOCAL_TIME.plusMinutes(SLOT_DURATION_MINUTES / 2)))
                        .build(),
                availabilityResponseObserver);

        ArgumentCaptor<AvailabilityResponse> responseCaptor = ArgumentCaptor.forClass(AvailabilityResponse.class);
        Mockito.verify(availabilityResponseObserver).onNext(responseCaptor.capture());
        AvailabilityResponse capturedResponse = responseCaptor.getValue();

        assertEquals(1, capturedResponse.getSlotList().size());
    }

    @Test
    public void testCheckAvailabilityOneAttraction2() {
        Attraction attraction = new Attraction(ATTRACTION_NAME, TIME_FROM_LOCAL_TIME, TIME_TO_LOCAL_TIME, SLOT_DURATION_MINUTES);
        attractions.put(ATTRACTION_NAME, attraction);

        bookingService.checkAttractionAvailability(AvailabilityRequest.newBuilder()
                        .setDayOfYear(VALID_DAY_OF_YEAR)
                        .setSlotFrom("10:10")
                        .setSlotTo("11:25")
                        .build(),
                availabilityResponseObserver);

        ArgumentCaptor<AvailabilityResponse> responseCaptor = ArgumentCaptor.forClass(AvailabilityResponse.class);
        Mockito.verify(availabilityResponseObserver).onNext(responseCaptor.capture());
        AvailabilityResponse capturedResponse = responseCaptor.getValue();

        assertEquals(2, capturedResponse.getSlotList().size());
    }

    @Test
    public void testCheckAvailabilityOneAttraction3() {
        Attraction attraction = new Attraction(ATTRACTION_NAME, TIME_FROM_LOCAL_TIME, TIME_TO_LOCAL_TIME, SLOT_DURATION_MINUTES);
        attractions.put(ATTRACTION_NAME, attraction);

        bookingService.checkAttractionAvailability(AvailabilityRequest.newBuilder()
                        .setDayOfYear(VALID_DAY_OF_YEAR)
                        .setSlotFrom("09:10")
                        .setSlotTo("11:00")
                        .build(),
                availabilityResponseObserver);

        ArgumentCaptor<AvailabilityResponse> responseCaptor = ArgumentCaptor.forClass(AvailabilityResponse.class);
        Mockito.verify(availabilityResponseObserver).onNext(responseCaptor.capture());
        AvailabilityResponse capturedResponse = responseCaptor.getValue();

        assertEquals(3, capturedResponse.getSlotList().size());
    }

    @Test
    public void testCheckAvailabilityOneAttraction4() {
        Attraction attraction = new Attraction(ATTRACTION_NAME, TIME_FROM_LOCAL_TIME, TIME_TO_LOCAL_TIME, SLOT_DURATION_MINUTES);
        attractions.put(ATTRACTION_NAME, attraction);

        bookingService.checkAttractionAvailability(AvailabilityRequest.newBuilder()
                        .setDayOfYear(VALID_DAY_OF_YEAR)
                        .setSlotFrom("16:17")
                        .setSlotTo("18:00")
                        .build(),
                availabilityResponseObserver);

        ArgumentCaptor<AvailabilityResponse> responseCaptor = ArgumentCaptor.forClass(AvailabilityResponse.class);
        Mockito.verify(availabilityResponseObserver).onNext(responseCaptor.capture());
        AvailabilityResponse capturedResponse = responseCaptor.getValue();

        assertEquals(3, capturedResponse.getSlotList().size());
    }

    @Test
    public void testCheckAvailabilityOneAttraction5() {
        Attraction attraction = new Attraction(ATTRACTION_NAME, TIME_FROM_LOCAL_TIME, TIME_TO_LOCAL_TIME, SLOT_DURATION_MINUTES);
        attractions.put(ATTRACTION_NAME, attraction);

        bookingService.checkAttractionAvailability(AvailabilityRequest.newBuilder()
                        .setDayOfYear(VALID_DAY_OF_YEAR)
                        .setSlotFrom("16:17")
                        .setSlotTo("18:48")
                        .build(),
                availabilityResponseObserver);

        ArgumentCaptor<AvailabilityResponse> responseCaptor = ArgumentCaptor.forClass(AvailabilityResponse.class);
        Mockito.verify(availabilityResponseObserver).onNext(responseCaptor.capture());
        AvailabilityResponse capturedResponse = responseCaptor.getValue();

        assertEquals(3, capturedResponse.getSlotList().size());
    }

    @Test
    public void testCheckAvailabilityOneAttraction6() {
        Attraction attraction = new Attraction(ATTRACTION_NAME, TIME_FROM_LOCAL_TIME, TIME_TO_LOCAL_TIME, SLOT_DURATION_MINUTES);
        attractions.put(ATTRACTION_NAME, attraction);

        bookingService.checkAttractionAvailability(AvailabilityRequest.newBuilder()
                        .setDayOfYear(VALID_DAY_OF_YEAR)
                        .setSlotFrom("10:00")
                        .setSlotTo("18:00")
                        .build(),
                availabilityResponseObserver);

        ArgumentCaptor<AvailabilityResponse> responseCaptor = ArgumentCaptor.forClass(AvailabilityResponse.class);
        Mockito.verify(availabilityResponseObserver).onNext(responseCaptor.capture());
        AvailabilityResponse capturedResponse = responseCaptor.getValue();

        assertEquals(16, capturedResponse.getSlotList().size());
    }

    @Test
    public void testCheckAvailabilityOneAttraction7() {
        Attraction attraction = new Attraction(ATTRACTION_NAME, TIME_FROM_LOCAL_TIME, TIME_TO_LOCAL_TIME, SLOT_DURATION_MINUTES);
        attractions.put(ATTRACTION_NAME, attraction);

        bookingService.checkAttractionAvailability(AvailabilityRequest.newBuilder()
                        .setDayOfYear(VALID_DAY_OF_YEAR)
                        .setSlotFrom("10:02")
                        .setSlotTo("17:58")
                        .build(),
                availabilityResponseObserver);

        ArgumentCaptor<AvailabilityResponse> responseCaptor = ArgumentCaptor.forClass(AvailabilityResponse.class);
        Mockito.verify(availabilityResponseObserver).onNext(responseCaptor.capture());
        AvailabilityResponse capturedResponse = responseCaptor.getValue();

        assertEquals(15, capturedResponse.getSlotList().size());
    }

    @Test
    public void testCheckAvailabilityOneAttraction8() {
        Attraction attraction = new Attraction(ATTRACTION_NAME, TIME_FROM_LOCAL_TIME, TIME_TO_LOCAL_TIME, SLOT_DURATION_MINUTES);
        attractions.put(ATTRACTION_NAME, attraction);

        bookingService.checkAttractionAvailability(AvailabilityRequest.newBuilder()
                        .setDayOfYear(VALID_DAY_OF_YEAR)
                        .setSlotFrom("11:00")
                        .setSlotTo("11:00")
                        .build(),
                availabilityResponseObserver);

        ArgumentCaptor<AvailabilityResponse> responseCaptor = ArgumentCaptor.forClass(AvailabilityResponse.class);
        Mockito.verify(availabilityResponseObserver).onNext(responseCaptor.capture());
        AvailabilityResponse capturedResponse = responseCaptor.getValue();

        assertEquals(1, capturedResponse.getSlotList().size());
    }

    @Test
    public void testCheckAvailabilityForAllAttractions() {
        Attraction attraction = new Attraction(ATTRACTION_NAME, TIME_FROM_LOCAL_TIME, TIME_TO_LOCAL_TIME, SLOT_DURATION_MINUTES);
        attractions.put(ATTRACTION_NAME, attraction);

        Attraction anotherAttraction = new Attraction(ANOTHER_ATTRACTION_NAME, TIME_FROM_LOCAL_TIME, TIME_TO_LOCAL_TIME, SLOT_DURATION_MINUTES);
        attractions.put(ANOTHER_ATTRACTION_NAME, anotherAttraction);

        bookingService.checkAttractionAvailability(AvailabilityRequest.newBuilder()
                        .setDayOfYear(VALID_DAY_OF_YEAR)
                        .setSlotFrom(TIME_FROM_STRING)
                        .setSlotTo(TIME_TO_STRING)
                        .build(),
                availabilityResponseObserver);

        ArgumentCaptor<AvailabilityResponse> responseCaptor = ArgumentCaptor.forClass(AvailabilityResponse.class);
        Mockito.verify(availabilityResponseObserver).onNext(responseCaptor.capture());
        AvailabilityResponse capturedResponse = responseCaptor.getValue();

        assertEquals(2 * TOTAL_SLOTS, capturedResponse.getSlotList().size());

        for (int i = 0; i < TOTAL_SLOTS; i++) {
            assertEquals(NO_SLOT_CAPACITY, capturedResponse.getSlot(i).getSlotCapacity());
            assertEquals(ANOTHER_ATTRACTION_NAME, capturedResponse.getSlot(i).getAttractionName());
            assertEquals(0, capturedResponse.getSlot(i).getBookingsConfirmed());
            assertEquals(0, capturedResponse.getSlot(i).getBookingsPending());
            assertEquals(TIME_FROM_LOCAL_TIME.plusMinutes((long) i * SLOT_DURATION_MINUTES).toString(), capturedResponse.getSlot(i).getSlot());
        }

        for (int i = TOTAL_SLOTS; i < 2 * TOTAL_SLOTS; i++) {
            assertEquals(NO_SLOT_CAPACITY, capturedResponse.getSlot(i).getSlotCapacity());
            assertEquals(ATTRACTION_NAME, capturedResponse.getSlot(i).getAttractionName());
            assertEquals(0, capturedResponse.getSlot(i).getBookingsConfirmed());
            assertEquals(0, capturedResponse.getSlot(i).getBookingsPending());
            assertEquals(TIME_FROM_LOCAL_TIME.plusMinutes((long) (i - TOTAL_SLOTS) * SLOT_DURATION_MINUTES).toString(), capturedResponse.getSlot(i).getSlot());
        }
    }

    @Test
    public void testCheckAvailabilityConfirmed() {
        Attraction attraction = new Attraction(ATTRACTION_NAME, TIME_FROM_LOCAL_TIME, TIME_TO_LOCAL_TIME, SLOT_DURATION_MINUTES);
        attractions.put(ATTRACTION_NAME, attraction);

        Map<UUID, ConfirmedReservation>[] confirmedReservations = new Map[TOTAL_SLOTS];

        for (int i = 0; i < TOTAL_SLOTS; i++) {
            confirmedReservations[i] = new ConcurrentHashMap<>();
            confirmedReservations[i].put(VISITOR_ID, Mockito.mock(ConfirmedReservation.class));
        }

        ReservationHandler reservationHandler = new ReservationHandler(attraction,
                VALID_DAY_OF_YEAR, Mockito.mock(ReservationObserver.class),
                SLOT_CAPACITY, confirmedReservations, new LinkedHashMap[TOTAL_SLOTS]);

        attraction.setReservationHandler(VALID_DAY_OF_YEAR, reservationHandler);

        bookingService.checkAttractionAvailability(AvailabilityRequest.newBuilder()
                        .setDayOfYear(VALID_DAY_OF_YEAR)
                        .setSlotFrom(TIME_FROM_STRING)
                        .setSlotTo(TIME_TO_STRING)
                        .build(),
                availabilityResponseObserver);

        ArgumentCaptor<AvailabilityResponse> responseCaptor = ArgumentCaptor.forClass(AvailabilityResponse.class);
        Mockito.verify(availabilityResponseObserver).onNext(responseCaptor.capture());
        AvailabilityResponse capturedResponse = responseCaptor.getValue();

        assertEquals(TOTAL_SLOTS, capturedResponse.getSlotList().size());
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            assertEquals(SLOT_CAPACITY, capturedResponse.getSlot(i).getSlotCapacity());
            assertEquals(ATTRACTION_NAME, capturedResponse.getSlot(i).getAttractionName());
            assertEquals(1, capturedResponse.getSlot(i).getBookingsConfirmed());
            assertEquals(0, capturedResponse.getSlot(i).getBookingsPending());
            assertEquals(TIME_FROM_LOCAL_TIME.plusMinutes((long) i * SLOT_DURATION_MINUTES).toString(), capturedResponse.getSlot(i).getSlot());
        }
    }

    @Test
    public void testCheckAvailabilityPending() {
        Attraction attraction = new Attraction(ATTRACTION_NAME, TIME_FROM_LOCAL_TIME, TIME_TO_LOCAL_TIME, SLOT_DURATION_MINUTES);
        attractions.put(ATTRACTION_NAME, attraction);

        LinkedHashMap<UUID, Reservation>[] pendingReservations = (LinkedHashMap<UUID, Reservation>[]) new LinkedHashMap[TOTAL_SLOTS];

        for (int i = 0; i < TOTAL_SLOTS; i++) {
            pendingReservations[i] = new LinkedHashMap<>();
            pendingReservations[i].put(VISITOR_ID, Mockito.mock(Reservation.class));
        }

        ReservationHandler reservationHandler = new ReservationHandler(attraction,
                VALID_DAY_OF_YEAR, Mockito.mock(ReservationObserver.class),
                SLOT_CAPACITY, new Map[TOTAL_SLOTS], pendingReservations);
        attraction.setReservationHandler(VALID_DAY_OF_YEAR, reservationHandler);

        bookingService.checkAttractionAvailability(AvailabilityRequest.newBuilder()
                        .setDayOfYear(VALID_DAY_OF_YEAR)
                        .setSlotFrom(TIME_FROM_STRING)
                        .setSlotTo(TIME_TO_STRING)
                        .build(),
                availabilityResponseObserver);

        ArgumentCaptor<AvailabilityResponse> responseCaptor = ArgumentCaptor.forClass(AvailabilityResponse.class);
        Mockito.verify(availabilityResponseObserver).onNext(responseCaptor.capture());
        AvailabilityResponse capturedResponse = responseCaptor.getValue();

        assertEquals(TOTAL_SLOTS, capturedResponse.getSlotList().size());
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            assertEquals(SLOT_CAPACITY, capturedResponse.getSlot(i).getSlotCapacity());
            assertEquals(ATTRACTION_NAME, capturedResponse.getSlot(i).getAttractionName());
            assertEquals(0, capturedResponse.getSlot(i).getBookingsConfirmed());
            assertEquals(1, capturedResponse.getSlot(i).getBookingsPending());
            assertEquals(TIME_FROM_LOCAL_TIME.plusMinutes((long) i * SLOT_DURATION_MINUTES).toString(), capturedResponse.getSlot(i).getSlot());
        }
    }

    @Test
    public void testConfirmReservationFailureLessThan0Days() {
        assertThrows(InvalidDayException.class, () -> {
            bookingService.confirmReservation(BookingRequest.newBuilder()
                            .setAttractionName(ATTRACTION_NAME)
                            .setDayOfYear(INVALID_DAY_OF_YEAR)
                            .setSlot(ParseUtils.formatTime(TIME_FROM_LOCAL_TIME))
                            .setVisitorId(VISITOR_ID.toString())
                            .build(),
                    Mockito.mock(StreamObserver.class));
        });
    }

    @Test
    public void testConfirmReservationFailureMoreThan365Days() {
        assertThrows(InvalidDayException.class, () -> {
            bookingService.confirmReservation(BookingRequest.newBuilder()
                            .setAttractionName(ATTRACTION_NAME)
                            .setDayOfYear(OTHER_INVALID_DAY_OF_YEAR)
                            .setSlot(ParseUtils.formatTime(TIME_FROM_LOCAL_TIME))
                            .setVisitorId(VISITOR_ID.toString())
                            .build(),
                    Mockito.mock(StreamObserver.class));
        });
    }

    @Test
    public void testConfirmReservationFailureEmptyAttractionName() {
        assertThrows(EmptyAttractionException.class, () -> {
            bookingService.confirmReservation(BookingRequest.newBuilder()
                            .setAttractionName("")
                            .setDayOfYear(VALID_DAY_OF_YEAR)
                            .setSlot(ParseUtils.formatTime(TIME_FROM_LOCAL_TIME))
                            .setVisitorId(VISITOR_ID.toString())
                            .build(),
                    Mockito.mock(StreamObserver.class));
        });
    }

    @Test
    public void testConfirmReservationFailureBlankAttractionName() {
        assertThrows(EmptyAttractionException.class, () -> {
            bookingService.confirmReservation(BookingRequest.newBuilder()
                            .setAttractionName("  ")
                            .setDayOfYear(VALID_DAY_OF_YEAR)
                            .setSlot(ParseUtils.formatTime(TIME_FROM_LOCAL_TIME))
                            .setVisitorId(VISITOR_ID.toString())
                            .build(),
                    Mockito.mock(StreamObserver.class));
        });
    }

    @Test
    public void testConfirmReservationFailureNoAttraction() {
        assertThrows(AttractionNotFoundException.class, () -> {

            Ticket ticket = new Ticket(VISITOR_ID, VALID_DAY_OF_YEAR, TICKET_TYPE_FULL_DAY);
            ticketsByDay[VALID_DAY_OF_YEAR - 1].put(VISITOR_ID, ticket);

            bookingService.confirmReservation(BookingRequest.newBuilder()
                            .setAttractionName(NON_EXISTING_ATTRACTION_NAME)
                            .setDayOfYear(VALID_DAY_OF_YEAR)
                            .setSlot(ParseUtils.formatTime(TIME_FROM_LOCAL_TIME))
                            .setVisitorId(VISITOR_ID.toString())
                            .build(),
                    Mockito.mock(StreamObserver.class));
        });
    }

    @Test
    public void testConfirmReservationFailureNoCapacityDefined() {
        assertThrows(CapacityNotDefinedException.class, () -> {
            Attraction attraction = new Attraction(ATTRACTION_NAME, TIME_FROM_LOCAL_TIME, TIME_TO_LOCAL_TIME, SLOT_DURATION_MINUTES);
            attractions.put(ATTRACTION_NAME, attraction);

            Ticket ticket = new Ticket(VISITOR_ID, VALID_DAY_OF_YEAR, TICKET_TYPE_FULL_DAY);
            ticketsByDay[VALID_DAY_OF_YEAR - 1].put(VISITOR_ID, ticket);

            ReservationHandler reservationHandler = attraction.getReservationHandler(VALID_DAY_OF_YEAR);
            attraction.setReservationHandler(VALID_DAY_OF_YEAR, reservationHandler);

            bookingService.confirmReservation(BookingRequest.newBuilder()
                            .setAttractionName(ATTRACTION_NAME)
                            .setDayOfYear(VALID_DAY_OF_YEAR)
                            .setSlot(ParseUtils.formatTime(TIME_FROM_LOCAL_TIME))
                            .setVisitorId(VISITOR_ID.toString())
                            .build(),
                    Mockito.mock(StreamObserver.class));
        });
    }

    @Test
    public void testConfirmReservationFailureReservationAlreadyConfirmed() {
        assertThrows(ReservationAlreadyConfirmedException.class, () -> {
            Attraction attraction = new Attraction(ATTRACTION_NAME, TIME_FROM_LOCAL_TIME, TIME_TO_LOCAL_TIME, SLOT_DURATION_MINUTES);
            attractions.put(ATTRACTION_NAME, attraction);

            Ticket ticket = new Ticket(VISITOR_ID, VALID_DAY_OF_YEAR, TICKET_TYPE_FULL_DAY);
            ticketsByDay[VALID_DAY_OF_YEAR - 1].put(VISITOR_ID, ticket);

            Map<UUID, ConfirmedReservation>[] confirmedReservations = new Map[TOTAL_SLOTS];

            ConfirmedReservation reservation = Mockito.mock(ConfirmedReservation.class);
            confirmedReservations[0] = new ConcurrentHashMap<>();
            confirmedReservations[0].put(VISITOR_ID, reservation);

            ReservationHandler reservationHandler = new ReservationHandler(attraction,
                    VALID_DAY_OF_YEAR, Mockito.mock(ReservationObserver.class),
                    SLOT_CAPACITY, confirmedReservations, new LinkedHashMap[TOTAL_SLOTS]);

            attraction.setReservationHandler(VALID_DAY_OF_YEAR, reservationHandler);

            bookingService.confirmReservation(BookingRequest.newBuilder()
                            .setAttractionName(ATTRACTION_NAME)
                            .setDayOfYear(VALID_DAY_OF_YEAR)
                            .setSlot(ParseUtils.formatTime(TIME_FROM_LOCAL_TIME))
                            .setVisitorId(VISITOR_ID.toString())
                            .build(),
                    Mockito.mock(StreamObserver.class));
        });
    }

    @Test
    public void testConfirmReservationFailureReservationNotFound() {
        assertThrows(ReservationNotFoundException.class, () -> {
            Attraction attraction = new Attraction(ATTRACTION_NAME, TIME_FROM_LOCAL_TIME, TIME_TO_LOCAL_TIME, SLOT_DURATION_MINUTES);
            attractions.put(ATTRACTION_NAME, attraction);

            Ticket ticket = new Ticket(VISITOR_ID, VALID_DAY_OF_YEAR, TICKET_TYPE_FULL_DAY);
            ticketsByDay[VALID_DAY_OF_YEAR - 1].put(VISITOR_ID, ticket);

            Map<UUID, ConfirmedReservation>[] confirmedReservations = new Map[TOTAL_SLOTS];
            confirmedReservations[0] = new ConcurrentHashMap<>();

            ReservationHandler reservationHandler = new ReservationHandler(attraction,
                    VALID_DAY_OF_YEAR, Mockito.mock(ReservationObserver.class),
                    SLOT_CAPACITY, confirmedReservations, new LinkedHashMap[TOTAL_SLOTS]);

            attraction.setReservationHandler(VALID_DAY_OF_YEAR, reservationHandler);

            bookingService.confirmReservation(BookingRequest.newBuilder()
                            .setAttractionName(ATTRACTION_NAME)
                            .setDayOfYear(VALID_DAY_OF_YEAR)
                            .setSlot(ParseUtils.formatTime(TIME_FROM_LOCAL_TIME))
                            .setVisitorId(VISITOR_ID.toString())
                            .build(),
                    Mockito.mock(StreamObserver.class));
        });
    }

    @Test
    public void testConfirmReservationFailureInvalidSlot() {
        assertThrows(InvalidSlotException.class, () -> {
            Attraction attraction = new Attraction(ATTRACTION_NAME, TIME_FROM_LOCAL_TIME, TIME_TO_LOCAL_TIME, SLOT_DURATION_MINUTES);
            attractions.put(ATTRACTION_NAME, attraction);

            Ticket ticket = new Ticket(VISITOR_ID, VALID_DAY_OF_YEAR, TICKET_TYPE_FULL_DAY);
            ticketsByDay[VALID_DAY_OF_YEAR - 1].put(VISITOR_ID, ticket);

            Map<UUID, ConfirmedReservation>[] confirmedReservations = new Map[TOTAL_SLOTS];
            confirmedReservations[0] = new ConcurrentHashMap<>();

            ReservationHandler reservationHandler = new ReservationHandler(attraction,
                    VALID_DAY_OF_YEAR, Mockito.mock(ReservationObserver.class),
                    SLOT_CAPACITY, confirmedReservations, new LinkedHashMap[TOTAL_SLOTS]);

            attraction.setReservationHandler(VALID_DAY_OF_YEAR, reservationHandler);

            bookingService.confirmReservation(BookingRequest.newBuilder()
                            .setAttractionName(ATTRACTION_NAME)
                            .setDayOfYear(VALID_DAY_OF_YEAR)
                            .setSlot(ParseUtils.formatTime(TIME_TO_LOCAL_TIME))
                            .setVisitorId(VISITOR_ID.toString())
                            .build(),
                    Mockito.mock(StreamObserver.class));
        });
    }

    @Test
    public void testConfirmReservationFailureNoTicket() {
        assertThrows(MissingPassException.class, () -> {
            Attraction attraction = new Attraction(ATTRACTION_NAME, TIME_FROM_LOCAL_TIME, TIME_TO_LOCAL_TIME, SLOT_DURATION_MINUTES);
            attractions.put(ATTRACTION_NAME, attraction);

            bookingService.confirmReservation(BookingRequest.newBuilder()
                            .setAttractionName(ATTRACTION_NAME)
                            .setDayOfYear(VALID_DAY_OF_YEAR)
                            .setSlot(ParseUtils.formatTime(TIME_FROM_LOCAL_TIME))
                            .setVisitorId(VISITOR_ID.toString())
                            .build(),
                    Mockito.mock(StreamObserver.class));
        });
    }

    @Test
    public void testConfirmReservationFailureCantBookWithHalfDayPass() {
        assertThrows(MissingPassException.class, () -> {
            Attraction attraction = new Attraction(ATTRACTION_NAME, TIME_FROM_LOCAL_TIME, TIME_TO_LOCAL_TIME, SLOT_DURATION_MINUTES);
            attractions.put(ATTRACTION_NAME, attraction);

            Ticket ticket = new Ticket(VISITOR_ID, VALID_DAY_OF_YEAR, TICKET_TYPE_HALF_DAY);
            ticketsByDay[VALID_DAY_OF_YEAR - 1].put(VISITOR_ID, ticket);

            bookingService.confirmReservation(BookingRequest.newBuilder()
                            .setAttractionName(ATTRACTION_NAME)
                            .setDayOfYear(VALID_DAY_OF_YEAR)
                            .setSlot(HALF_DAY_TIME_RESTRICTION_LIMIT)
                            .setVisitorId(VISITOR_ID.toString())
                            .build(),
                    Mockito.mock(StreamObserver.class));
        });
    }

    @Test
    public void testConfirmReservation() {
        Attraction attraction = new Attraction(ATTRACTION_NAME, TIME_FROM_LOCAL_TIME, TIME_TO_LOCAL_TIME, SLOT_DURATION_MINUTES);
        attractions.put(ATTRACTION_NAME, attraction);

        Ticket ticket = new Ticket(VISITOR_ID, VALID_DAY_OF_YEAR, TICKET_TYPE_FULL_DAY);
        ticketsByDay[VALID_DAY_OF_YEAR - 1].put(VISITOR_ID, ticket);

        LinkedHashMap<UUID, Reservation>[] pendingReservations = (LinkedHashMap<UUID, Reservation>[]) new LinkedHashMap[TOTAL_SLOTS];
        pendingReservations[0] = new LinkedHashMap<>();

        Reservation reservation = new Reservation(ticket, attraction);
        pendingReservations[0].put(VISITOR_ID, reservation);

        ReservationHandler reservationHandler = new ReservationHandler(attraction,
                VALID_DAY_OF_YEAR, Mockito.mock(ReservationObserver.class),
                SLOT_CAPACITY, new Map[TOTAL_SLOTS], pendingReservations);

        attraction.setReservationHandler(VALID_DAY_OF_YEAR, reservationHandler);

        bookingService.confirmReservation(BookingRequest.newBuilder()
                        .setAttractionName(ATTRACTION_NAME)
                        .setDayOfYear(VALID_DAY_OF_YEAR)
                        .setSlot(ParseUtils.formatTime(TIME_FROM_LOCAL_TIME))
                        .setVisitorId(VISITOR_ID.toString())
                        .build(),
                Mockito.mock(StreamObserver.class));
    }

    @Test
    public void testCancelReservationFailureLessThan0Days() {
        assertThrows(InvalidDayException.class, () -> {
            bookingService.cancelReservation(BookingRequest.newBuilder()
                            .setAttractionName(ATTRACTION_NAME)
                            .setDayOfYear(INVALID_DAY_OF_YEAR)
                            .setSlot(ParseUtils.formatTime(TIME_FROM_LOCAL_TIME))
                            .setVisitorId(VISITOR_ID.toString())
                            .build(),
                    Mockito.mock(StreamObserver.class));
        });
    }

    @Test
    public void testCancelReservationFailureMoreThan365Days() {
        assertThrows(InvalidDayException.class, () -> {
            bookingService.cancelReservation(BookingRequest.newBuilder()
                            .setAttractionName(ATTRACTION_NAME)
                            .setDayOfYear(OTHER_INVALID_DAY_OF_YEAR)
                            .setSlot(ParseUtils.formatTime(TIME_FROM_LOCAL_TIME))
                            .setVisitorId(VISITOR_ID.toString())
                            .build(),
                    Mockito.mock(StreamObserver.class));
        });
    }

    @Test
    public void testCancelReservationFailureEmptyAttractionName() {
        assertThrows(EmptyAttractionException.class, () -> {
            bookingService.cancelReservation(BookingRequest.newBuilder()
                            .setAttractionName("")
                            .setDayOfYear(VALID_DAY_OF_YEAR)
                            .setSlot(ParseUtils.formatTime(TIME_FROM_LOCAL_TIME))
                            .setVisitorId(VISITOR_ID.toString())
                            .build(),
                    Mockito.mock(StreamObserver.class));
        });
    }

    @Test
    public void testCancelReservationFailureBlankAttractionName() {
        assertThrows(EmptyAttractionException.class, () -> {
            bookingService.cancelReservation(BookingRequest.newBuilder()
                            .setAttractionName("  ")
                            .setDayOfYear(VALID_DAY_OF_YEAR)
                            .setSlot(ParseUtils.formatTime(TIME_FROM_LOCAL_TIME))
                            .setVisitorId(VISITOR_ID.toString())
                            .build(),
                    Mockito.mock(StreamObserver.class));
        });
    }

    @Test
    public void testCancelReservationFailureNoAttraction() {
        assertThrows(AttractionNotFoundException.class, () -> {
            Ticket ticket = new Ticket(VISITOR_ID, VALID_DAY_OF_YEAR, TICKET_TYPE_FULL_DAY);
            ticketsByDay[VALID_DAY_OF_YEAR - 1].put(VISITOR_ID, ticket);

            bookingService.cancelReservation(BookingRequest.newBuilder()
                            .setAttractionName(NON_EXISTING_ATTRACTION_NAME)
                            .setDayOfYear(VALID_DAY_OF_YEAR)
                            .setSlot(ParseUtils.formatTime(TIME_FROM_LOCAL_TIME))
                            .setVisitorId(VISITOR_ID.toString())
                            .build(),
                    Mockito.mock(StreamObserver.class));
        });
    }

    @Test
    public void testCancelReservationFailureInvalidSlot() {
        assertThrows(InvalidSlotException.class, () -> {
            Attraction attraction = new Attraction(ATTRACTION_NAME, TIME_FROM_LOCAL_TIME, TIME_TO_LOCAL_TIME, SLOT_DURATION_MINUTES);
            attractions.put(ATTRACTION_NAME, attraction);

            Ticket ticket = new Ticket(VISITOR_ID, VALID_DAY_OF_YEAR, TICKET_TYPE_FULL_DAY);
            ticketsByDay[VALID_DAY_OF_YEAR - 1].put(VISITOR_ID, ticket);

            Map<UUID, ConfirmedReservation>[] confirmedReservations = new Map[TOTAL_SLOTS];
            confirmedReservations[0] = new ConcurrentHashMap<>();

            ReservationHandler reservationHandler = new ReservationHandler(attraction,
                    VALID_DAY_OF_YEAR, Mockito.mock(ReservationObserver.class),
                    SLOT_CAPACITY, confirmedReservations, new LinkedHashMap[TOTAL_SLOTS]);

            attraction.setReservationHandler(VALID_DAY_OF_YEAR, reservationHandler);

            bookingService.cancelReservation(BookingRequest.newBuilder()
                            .setAttractionName(ATTRACTION_NAME)
                            .setDayOfYear(VALID_DAY_OF_YEAR)
                            .setSlot(ParseUtils.formatTime(TIME_TO_LOCAL_TIME))
                            .setVisitorId(VISITOR_ID.toString())
                            .build(),
                    Mockito.mock(StreamObserver.class));
        });
    }

    @Test
    public void testCancelReservationFailureNoPreviousReservationMade() {
        assertThrows(ReservationNotFoundException.class, () -> {
            Attraction attraction = new Attraction(ATTRACTION_NAME, TIME_FROM_LOCAL_TIME, TIME_TO_LOCAL_TIME, SLOT_DURATION_MINUTES);
            attractions.put(ATTRACTION_NAME, attraction);

            Ticket ticket = new Ticket(VISITOR_ID, VALID_DAY_OF_YEAR, TICKET_TYPE_FULL_DAY);
            ticketsByDay[VALID_DAY_OF_YEAR - 1].put(VISITOR_ID, ticket);

            Map<UUID, ConfirmedReservation>[] confirmedReservations = new Map[TOTAL_SLOTS];
            confirmedReservations[0] = new ConcurrentHashMap<>();

            ReservationHandler reservationHandler = new ReservationHandler(attraction,
                    VALID_DAY_OF_YEAR, Mockito.mock(ReservationObserver.class),
                    SLOT_CAPACITY, confirmedReservations, new LinkedHashMap[TOTAL_SLOTS]);

            attraction.setReservationHandler(VALID_DAY_OF_YEAR, reservationHandler);

            bookingService.cancelReservation(BookingRequest.newBuilder()
                            .setAttractionName(ATTRACTION_NAME)
                            .setDayOfYear(VALID_DAY_OF_YEAR)
                            .setSlot(ParseUtils.formatTime(TIME_FROM_LOCAL_TIME))
                            .setVisitorId(VISITOR_ID.toString())
                            .build(),
                    Mockito.mock(StreamObserver.class));
        });
    }

    @Test
    public void testCancelReservationSuccess() {
        Attraction attraction = new Attraction(ATTRACTION_NAME, TIME_FROM_LOCAL_TIME, TIME_TO_LOCAL_TIME, SLOT_DURATION_MINUTES);
        attractions.put(ATTRACTION_NAME, attraction);

        Ticket ticket = new Ticket(VISITOR_ID, VALID_DAY_OF_YEAR, TICKET_TYPE_FULL_DAY, 1);
        ticketsByDay[VALID_DAY_OF_YEAR - 1].put(VISITOR_ID, ticket);

        Map<UUID, ConfirmedReservation>[] confirmedReservations = new Map[TOTAL_SLOTS];

        ConfirmedReservation reservation = Mockito.mock(ConfirmedReservation.class);
        confirmedReservations[0] = new ConcurrentHashMap<>();
        confirmedReservations[0].put(VISITOR_ID, reservation);

        ReservationHandler reservationHandler = new ReservationHandler(attraction,
                VALID_DAY_OF_YEAR, Mockito.mock(ReservationObserver.class),
                SLOT_CAPACITY, confirmedReservations, new LinkedHashMap[TOTAL_SLOTS]);

        attraction.setReservationHandler(VALID_DAY_OF_YEAR, reservationHandler);

        bookingService.cancelReservation(BookingRequest.newBuilder()
                        .setAttractionName(ATTRACTION_NAME)
                        .setDayOfYear(VALID_DAY_OF_YEAR)
                        .setSlot(ParseUtils.formatTime(TIME_FROM_LOCAL_TIME))
                        .setVisitorId(VISITOR_ID.toString())
                        .build(),
                Mockito.mock(StreamObserver.class));

        assertTrue(confirmedReservations[0].isEmpty());
    }

    @Test
    public void testReserveAttractionFailureLessThan0Days() {
        assertThrows(InvalidDayException.class, () -> {
            bookingService.reserveAttraction(BookingRequest.newBuilder()
                            .setAttractionName(ATTRACTION_NAME)
                            .setDayOfYear(INVALID_DAY_OF_YEAR)
                            .setSlot(ParseUtils.formatTime(TIME_FROM_LOCAL_TIME))
                            .setVisitorId(VISITOR_ID.toString())
                            .build(),
                    Mockito.mock(StreamObserver.class));
        });
    }

    @Test
    public void testReserveAttractionFailureMoreThan365Days() {
        assertThrows(InvalidDayException.class, () -> {
            bookingService.reserveAttraction(BookingRequest.newBuilder()
                            .setAttractionName(ATTRACTION_NAME)
                            .setDayOfYear(OTHER_INVALID_DAY_OF_YEAR)
                            .setSlot(ParseUtils.formatTime(TIME_FROM_LOCAL_TIME))
                            .setVisitorId(VISITOR_ID.toString())
                            .build(),
                    Mockito.mock(StreamObserver.class));
        });
    }

    @Test
    public void testReserveAttractionFailureEmptyAttractionName() {
        assertThrows(EmptyAttractionException.class, () -> {
            bookingService.reserveAttraction(BookingRequest.newBuilder()
                            .setAttractionName("")
                            .setDayOfYear(VALID_DAY_OF_YEAR)
                            .setSlot(ParseUtils.formatTime(TIME_FROM_LOCAL_TIME))
                            .setVisitorId(VISITOR_ID.toString())
                            .build(),
                    Mockito.mock(StreamObserver.class));
        });
    }

    @Test
    public void testReserveAttractionFailureBlankAttractionName() {
        assertThrows(EmptyAttractionException.class, () -> {
            bookingService.reserveAttraction(BookingRequest.newBuilder()
                            .setAttractionName("  ")
                            .setDayOfYear(VALID_DAY_OF_YEAR)
                            .setSlot(ParseUtils.formatTime(TIME_FROM_LOCAL_TIME))
                            .setVisitorId(VISITOR_ID.toString())
                            .build(),
                    Mockito.mock(StreamObserver.class));
        });
    }

    @Test
    public void testReserveAttractionFailureNoAttraction() {
        assertThrows(AttractionNotFoundException.class, () -> {
            bookingService.reserveAttraction(BookingRequest.newBuilder()
                            .setAttractionName(NON_EXISTING_ATTRACTION_NAME)
                            .setDayOfYear(VALID_DAY_OF_YEAR)
                            .setSlot(ParseUtils.formatTime(TIME_FROM_LOCAL_TIME))
                            .setVisitorId(VISITOR_ID.toString())
                            .build(),
                    Mockito.mock(StreamObserver.class));
        });
    }

    @Test
    public void testReserveAttractionFailureInvalidSlot() {
        assertThrows(InvalidSlotException.class, () -> {
            Attraction attraction = new Attraction(ATTRACTION_NAME, TIME_FROM_LOCAL_TIME, TIME_TO_LOCAL_TIME, SLOT_DURATION_MINUTES);
            attractions.put(ATTRACTION_NAME, attraction);

            Ticket ticket = new Ticket(VISITOR_ID, VALID_DAY_OF_YEAR, TICKET_TYPE_FULL_DAY);
            ticketsByDay[VALID_DAY_OF_YEAR - 1].put(VISITOR_ID, ticket);

            Map<UUID, ConfirmedReservation>[] confirmedReservations = new Map[TOTAL_SLOTS];
            confirmedReservations[0] = new ConcurrentHashMap<>();

            ReservationHandler reservationHandler = new ReservationHandler(attraction,
                    VALID_DAY_OF_YEAR, Mockito.mock(ReservationObserver.class),
                    SLOT_CAPACITY, confirmedReservations, new LinkedHashMap[TOTAL_SLOTS]);

            attraction.setReservationHandler(VALID_DAY_OF_YEAR, reservationHandler);

            bookingService.reserveAttraction(BookingRequest.newBuilder()
                            .setAttractionName(ATTRACTION_NAME)
                            .setDayOfYear(VALID_DAY_OF_YEAR)
                            .setSlot(ParseUtils.formatTime(TIME_TO_LOCAL_TIME))
                            .setVisitorId(VISITOR_ID.toString())
                            .build(),
                    Mockito.mock(StreamObserver.class));
        });
    }

    @Test
    public void testReserveAttractionFailureNoTicket() {
        assertThrows(MissingPassException.class, () -> {
            Attraction attraction = new Attraction(ATTRACTION_NAME, TIME_FROM_LOCAL_TIME, TIME_TO_LOCAL_TIME, SLOT_DURATION_MINUTES);
            attractions.put(ATTRACTION_NAME, attraction);

            bookingService.reserveAttraction(BookingRequest.newBuilder()
                            .setAttractionName(ATTRACTION_NAME)
                            .setDayOfYear(VALID_DAY_OF_YEAR)
                            .setSlot(HALF_DAY_TIME_RESTRICTION_LIMIT)
                            .setVisitorId(VISITOR_ID.toString())
                            .build(),
                    Mockito.mock(StreamObserver.class));
        });
    }

    @Test
    public void testReserveAttractionFailureHalfDayPassRestriction() {
        assertThrows(MissingPassException.class, () -> {
            Attraction attraction = new Attraction(ATTRACTION_NAME, TIME_FROM_LOCAL_TIME, TIME_TO_LOCAL_TIME, SLOT_DURATION_MINUTES);
            attractions.put(ATTRACTION_NAME, attraction);

            Ticket ticket = new Ticket(VISITOR_ID, VALID_DAY_OF_YEAR, TICKET_TYPE_HALF_DAY);
            ticketsByDay[VALID_DAY_OF_YEAR - 1].put(VISITOR_ID, ticket);

            bookingService.reserveAttraction(BookingRequest.newBuilder()
                            .setAttractionName(ATTRACTION_NAME)
                            .setDayOfYear(VALID_DAY_OF_YEAR)
                            .setSlot(HALF_DAY_TIME_RESTRICTION_LIMIT)
                            .setVisitorId(VISITOR_ID.toString())
                            .build(),
                    Mockito.mock(StreamObserver.class));
        });
    }

    @Test
    public void testReserveAttractionFailureFullDayPassRestriction() {
        assertThrows(MissingPassException.class, () -> {
            Attraction attraction = new Attraction(ATTRACTION_NAME, TIME_FROM_LOCAL_TIME, TIME_TO_LOCAL_TIME, SLOT_DURATION_MINUTES);
            attractions.put(ATTRACTION_NAME, attraction);

            Ticket ticket = new Ticket(VISITOR_ID, VALID_DAY_OF_YEAR, TICKET_TYPE_FULL_DAY, MAX_BOOKINGS_FOR_FULL_DAY);
            ticketsByDay[VALID_DAY_OF_YEAR - 1].put(VISITOR_ID, ticket);

            bookingService.reserveAttraction(BookingRequest.newBuilder()
                            .setAttractionName(ATTRACTION_NAME)
                            .setDayOfYear(VALID_DAY_OF_YEAR)
                            .setSlot(TIME_FROM_STRING)
                            .setVisitorId(VISITOR_ID.toString())
                            .build(),
                    Mockito.mock(StreamObserver.class));
        });
    }

    @Test
    public void testReserveAttractionSuccessNoCapacityDefined() {
        Attraction attraction = new Attraction(ATTRACTION_NAME, TIME_FROM_LOCAL_TIME, TIME_TO_LOCAL_TIME, SLOT_DURATION_MINUTES);
        attractions.put(ATTRACTION_NAME, attraction);

        Ticket ticket = new Ticket(VISITOR_ID, VALID_DAY_OF_YEAR, TICKET_TYPE_FULL_DAY);
        ticketsByDay[VALID_DAY_OF_YEAR - 1].put(VISITOR_ID, ticket);

        LinkedHashMap<UUID, Reservation>[] pendingReservations = (LinkedHashMap<UUID, Reservation>[]) new LinkedHashMap[TOTAL_SLOTS];

        ReservationHandler reservationHandler = new ReservationHandler(attraction,
                VALID_DAY_OF_YEAR, Mockito.mock(ReservationObserver.class),
                -1, new Map[TOTAL_SLOTS], pendingReservations);

        attraction.setReservationHandler(VALID_DAY_OF_YEAR, reservationHandler);

        bookingService.reserveAttraction(BookingRequest.newBuilder()
                        .setAttractionName(ATTRACTION_NAME)
                        .setDayOfYear(VALID_DAY_OF_YEAR)
                        .setSlot(TIME_FROM_STRING)
                        .setVisitorId(VISITOR_ID.toString())
                        .build(),
                Mockito.mock(StreamObserver.class));

        assertEquals(1, pendingReservations[0].size());
    }

    @Test
    public void testReserveAttractionSuccessCapacityDefined() {
        Attraction attraction = new Attraction(ATTRACTION_NAME, TIME_FROM_LOCAL_TIME, TIME_TO_LOCAL_TIME, SLOT_DURATION_MINUTES);
        attractions.put(ATTRACTION_NAME, attraction);

        Ticket ticket = new Ticket(VISITOR_ID, VALID_DAY_OF_YEAR, TICKET_TYPE_FULL_DAY);
        ticketsByDay[VALID_DAY_OF_YEAR - 1].put(VISITOR_ID, ticket);

        Map<UUID, ConfirmedReservation>[] confirmedReservations = (Map<UUID, ConfirmedReservation>[]) new Map[TOTAL_SLOTS];

        ReservationHandler reservationHandler = new ReservationHandler(attraction,
                VALID_DAY_OF_YEAR, Mockito.mock(ReservationObserver.class),
                SLOT_CAPACITY, confirmedReservations, new LinkedHashMap[TOTAL_SLOTS]);

        attraction.setReservationHandler(VALID_DAY_OF_YEAR, reservationHandler);

        bookingService.reserveAttraction(BookingRequest.newBuilder()
                        .setAttractionName(ATTRACTION_NAME)
                        .setDayOfYear(VALID_DAY_OF_YEAR)
                        .setSlot(TIME_FROM_STRING)
                        .setVisitorId(VISITOR_ID.toString())
                        .build(),
                Mockito.mock(StreamObserver.class));

        assertEquals(1, confirmedReservations[0].size());
    }

    @Test
    public void testReserveAttractionFailureMaxCapacityReached() {
        assertThrows(OutOfCapacityException.class, () -> {
            Attraction attraction = new Attraction(ATTRACTION_NAME, TIME_FROM_LOCAL_TIME, TIME_TO_LOCAL_TIME, SLOT_DURATION_MINUTES);
            attractions.put(ATTRACTION_NAME, attraction);

            Ticket ticket = new Ticket(VISITOR_ID, VALID_DAY_OF_YEAR, TICKET_TYPE_FULL_DAY);
            ticketsByDay[VALID_DAY_OF_YEAR - 1].put(VISITOR_ID, ticket);

            Map<UUID, ConfirmedReservation>[] confirmedReservations = (Map<UUID, ConfirmedReservation>[]) new Map[TOTAL_SLOTS];
            confirmedReservations[0] = new ConcurrentHashMap<>();

            for (int i = 0; i < SLOT_CAPACITY; i++) {
                ConfirmedReservation reservation = Mockito.mock(ConfirmedReservation.class);
                confirmedReservations[0].put(UUID.randomUUID(), reservation);
            }

            ReservationHandler reservationHandler = new ReservationHandler(attraction,
                    VALID_DAY_OF_YEAR, Mockito.mock(ReservationObserver.class),
                    SLOT_CAPACITY, confirmedReservations, new LinkedHashMap[TOTAL_SLOTS]);

            attraction.setReservationHandler(VALID_DAY_OF_YEAR, reservationHandler);

            bookingService.reserveAttraction(BookingRequest.newBuilder()
                            .setAttractionName(ATTRACTION_NAME)
                            .setDayOfYear(VALID_DAY_OF_YEAR)
                            .setSlot(TIME_FROM_STRING)
                            .setVisitorId(VISITOR_ID.toString())
                            .build(),
                    Mockito.mock(StreamObserver.class));
        });
    }

    @Test
    public void testReserveAttractionFailureReservationAlreadyExists() {
        assertThrows(ReservationAlreadyExistsException.class, () -> {
            Attraction attraction = new Attraction(ATTRACTION_NAME, TIME_FROM_LOCAL_TIME, TIME_TO_LOCAL_TIME, SLOT_DURATION_MINUTES);
            attractions.put(ATTRACTION_NAME, attraction);

            Ticket ticket = new Ticket(VISITOR_ID, VALID_DAY_OF_YEAR, TICKET_TYPE_FULL_DAY);
            ticketsByDay[VALID_DAY_OF_YEAR - 1].put(VISITOR_ID, ticket);

            Map<UUID, ConfirmedReservation>[] confirmedReservations = (Map<UUID, ConfirmedReservation>[]) new Map[TOTAL_SLOTS];
            confirmedReservations[0] = new ConcurrentHashMap<>();

            ConfirmedReservation reservation = Mockito.mock(ConfirmedReservation.class);
            confirmedReservations[0].put(VISITOR_ID, reservation);

            ReservationHandler reservationHandler = new ReservationHandler(attraction,
                    VALID_DAY_OF_YEAR, Mockito.mock(ReservationObserver.class),
                    SLOT_CAPACITY, confirmedReservations, new LinkedHashMap[TOTAL_SLOTS]);

            attraction.setReservationHandler(VALID_DAY_OF_YEAR, reservationHandler);

            bookingService.reserveAttraction(BookingRequest.newBuilder()
                            .setAttractionName(ATTRACTION_NAME)
                            .setDayOfYear(VALID_DAY_OF_YEAR)
                            .setSlot(TIME_FROM_STRING)
                            .setVisitorId(VISITOR_ID.toString())
                            .build(),
                    Mockito.mock(StreamObserver.class));
        });
    }
}
