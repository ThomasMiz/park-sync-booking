package edu.itba.serverdps.adapter.driving;

import com.google.protobuf.Empty;
import edu.itba.serverdps.application.exceptions.*;
import edu.itba.serverdps.application.utils.ParseUtils;
import edu.itba.serverdps.domain.model.Attraction;
import edu.itba.serverdps.domain.model.Reservation;
import edu.itba.serverdps.domain.model.TicketType;
import edu.itba.serverdps.domain.model.result.AttractionAvailabilityResult;
import edu.itba.serverdps.domain.model.result.MakeReservationResult;
import edu.itba.serverdps.domain.usecase.handler.AttractionHandler;
import edu.itba.serverdps.port.driving.grpc.*;
import io.grpc.stub.StreamObserver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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

    @Mock
    private AttractionHandler attractionHandler;

    @InjectMocks
    private BookingServiceImpl bookingService;

    // https://stackoverflow.com/questions/49871975/how-to-test-and-mock-a-grpc-service-written-in-java-using-mockito
    @Test
    public void testGetAttractionsWithOneAttraction() {
        StreamObserver<GetAttractionsResponse> responseObserver = Mockito.mock(StreamObserver.class);

        Attraction attraction = new Attraction(ATTRACTION_NAME, TIME_FROM_LOCAL_TIME, TIME_TO_LOCAL_TIME, SLOT_DURATION_MINUTES);
        when(attractionHandler.getAttractions()).thenReturn(List.of(attraction));

        bookingService.getAttractions(Empty.newBuilder().build(), responseObserver);

        // Capture onNext argument for examination
        ArgumentCaptor<GetAttractionsResponse> responseCaptor = ArgumentCaptor.forClass(GetAttractionsResponse.class);
        Mockito.verify(responseObserver).onNext(responseCaptor.capture());
        GetAttractionsResponse capturedResponse = responseCaptor.getValue();

        assertEquals(1, capturedResponse.getAttractionList().size());
        verify(responseObserver).onCompleted();
    }

    @Test
    public void testGetAttractionsWithNoAttraction() {
        StreamObserver<GetAttractionsResponse> responseObserver = Mockito.mock(StreamObserver.class);

        when(attractionHandler.getAttractions()).thenReturn(Collections.emptyList());

        bookingService.getAttractions(Empty.newBuilder().build(), responseObserver);

        ArgumentCaptor<GetAttractionsResponse> responseCaptor = ArgumentCaptor.forClass(GetAttractionsResponse.class);
        Mockito.verify(responseObserver).onNext(responseCaptor.capture());
        GetAttractionsResponse capturedResponse = responseCaptor.getValue();

        assertEquals(0, capturedResponse.getAttractionList().size());
        verify(responseObserver).onCompleted();
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
        when(attractionHandler.getAvailabilityForAttraction(eq(NON_EXISTING_ATTRACTION_NAME), anyInt(), any(), any()))
                .thenThrow(new AttractionNotFoundException());

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
        StreamObserver<AvailabilityResponse> responseObserver = Mockito.mock(StreamObserver.class);

        when(attractionHandler.getAvailabilityForAllAttractions(eq(VALID_DAY_OF_YEAR), any(), any()))
                .thenReturn(Collections.emptyList());

        bookingService.checkAttractionAvailability(AvailabilityRequest.newBuilder()
                        .setDayOfYear(VALID_DAY_OF_YEAR)
                        .setSlotFrom(TIME_FROM_STRING)
                        .setSlotTo(TIME_TO_STRING)
                        .build(),
                responseObserver);

        ArgumentCaptor<AvailabilityResponse> responseCaptor = ArgumentCaptor.forClass(AvailabilityResponse.class);
        Mockito.verify(responseObserver).onNext(responseCaptor.capture());
        AvailabilityResponse capturedResponse = responseCaptor.getValue();

        assertEquals(0, capturedResponse.getSlotList().size());
        verify(responseObserver).onCompleted();
    }

    @Test
    public void testCheckAvailabilityOneAttractionUniqueSlot() {
        StreamObserver<AvailabilityResponse> responseObserver = Mockito.mock(StreamObserver.class);

        List<AttractionAvailabilityResult> availabilityResults = new ArrayList<>();
        availabilityResults.add(new AttractionAvailabilityResult(ATTRACTION_NAME, TIME_FROM_LOCAL_TIME, NO_SLOT_CAPACITY, 0, 0));

        when(attractionHandler.getAvailabilityForAllAttractions(eq(VALID_DAY_OF_YEAR), any(), any()))
                .thenReturn(availabilityResults);

        bookingService.checkAttractionAvailability(AvailabilityRequest.newBuilder()
                        .setDayOfYear(VALID_DAY_OF_YEAR)
                        .setSlotFrom(TIME_FROM_STRING)
                        .setSlotTo(ParseUtils.formatTime(TIME_FROM_LOCAL_TIME.plusMinutes(SLOT_DURATION_MINUTES / 2)))
                        .build(),
                responseObserver);

        ArgumentCaptor<AvailabilityResponse> responseCaptor = ArgumentCaptor.forClass(AvailabilityResponse.class);
        Mockito.verify(responseObserver).onNext(responseCaptor.capture());
        AvailabilityResponse capturedResponse = responseCaptor.getValue();

        assertEquals(1, capturedResponse.getSlotList().size());
        verify(responseObserver).onCompleted();
    }

    @Test
    public void testCheckAvailabilityMultipleSlots() {
        StreamObserver<AvailabilityResponse> responseObserver = Mockito.mock(StreamObserver.class);

        List<AttractionAvailabilityResult> availabilityResults = new ArrayList<>();
        availabilityResults.add(new AttractionAvailabilityResult(ATTRACTION_NAME, TIME_FROM_LOCAL_TIME, NO_SLOT_CAPACITY, 0, 0));
        availabilityResults.add(new AttractionAvailabilityResult(ATTRACTION_NAME, TIME_FROM_LOCAL_TIME.plusMinutes(SLOT_DURATION_MINUTES), NO_SLOT_CAPACITY, 0, 0));

        when(attractionHandler.getAvailabilityForAllAttractions(eq(VALID_DAY_OF_YEAR), any(), any()))
                .thenReturn(availabilityResults);

        bookingService.checkAttractionAvailability(AvailabilityRequest.newBuilder()
                        .setDayOfYear(VALID_DAY_OF_YEAR)
                        .setSlotFrom("10:10")
                        .setSlotTo("11:25")
                        .build(),
                responseObserver);

        ArgumentCaptor<AvailabilityResponse> responseCaptor = ArgumentCaptor.forClass(AvailabilityResponse.class);
        Mockito.verify(responseObserver).onNext(responseCaptor.capture());
        AvailabilityResponse capturedResponse = responseCaptor.getValue();

        assertEquals(2, capturedResponse.getSlotList().size());
        verify(responseObserver).onCompleted();
    }

    @Test
    public void testCheckAvailabilityForAllAttractions() {
        StreamObserver<AvailabilityResponse> responseObserver = Mockito.mock(StreamObserver.class);

        List<AttractionAvailabilityResult> availabilityResults = new ArrayList<>();
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            availabilityResults.add(new AttractionAvailabilityResult(ANOTHER_ATTRACTION_NAME,
                    TIME_FROM_LOCAL_TIME.plusMinutes((long) i * SLOT_DURATION_MINUTES),
                    NO_SLOT_CAPACITY, 0, 0));
        }
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            availabilityResults.add(new AttractionAvailabilityResult(ATTRACTION_NAME,
                    TIME_FROM_LOCAL_TIME.plusMinutes((long) i * SLOT_DURATION_MINUTES),
                    NO_SLOT_CAPACITY, 0, 0));
        }

        when(attractionHandler.getAvailabilityForAllAttractions(eq(VALID_DAY_OF_YEAR), any(), any()))
                .thenReturn(availabilityResults);

        bookingService.checkAttractionAvailability(AvailabilityRequest.newBuilder()
                        .setDayOfYear(VALID_DAY_OF_YEAR)
                        .setSlotFrom(TIME_FROM_STRING)
                        .setSlotTo(TIME_TO_STRING)
                        .build(),
                responseObserver);

        ArgumentCaptor<AvailabilityResponse> responseCaptor = ArgumentCaptor.forClass(AvailabilityResponse.class);
        Mockito.verify(responseObserver).onNext(responseCaptor.capture());
        AvailabilityResponse capturedResponse = responseCaptor.getValue();

        assertEquals(2 * TOTAL_SLOTS, capturedResponse.getSlotList().size());
        verify(responseObserver).onCompleted();

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
        StreamObserver<AvailabilityResponse> responseObserver = Mockito.mock(StreamObserver.class);

        List<AttractionAvailabilityResult> availabilityResults = new ArrayList<>();
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            availabilityResults.add(new AttractionAvailabilityResult(ATTRACTION_NAME,
                    TIME_FROM_LOCAL_TIME.plusMinutes((long) i * SLOT_DURATION_MINUTES),
                    SLOT_CAPACITY, 1, 0));
        }

        when(attractionHandler.getAvailabilityForAllAttractions(eq(VALID_DAY_OF_YEAR), any(), any()))
                .thenReturn(availabilityResults);

        bookingService.checkAttractionAvailability(AvailabilityRequest.newBuilder()
                        .setDayOfYear(VALID_DAY_OF_YEAR)
                        .setSlotFrom(TIME_FROM_STRING)
                        .setSlotTo(TIME_TO_STRING)
                        .build(),
                responseObserver);

        ArgumentCaptor<AvailabilityResponse> responseCaptor = ArgumentCaptor.forClass(AvailabilityResponse.class);
        Mockito.verify(responseObserver).onNext(responseCaptor.capture());
        AvailabilityResponse capturedResponse = responseCaptor.getValue();

        assertEquals(TOTAL_SLOTS, capturedResponse.getSlotList().size());
        verify(responseObserver).onCompleted();

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
        StreamObserver<AvailabilityResponse> responseObserver = Mockito.mock(StreamObserver.class);

        List<AttractionAvailabilityResult> availabilityResults = new ArrayList<>();
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            availabilityResults.add(new AttractionAvailabilityResult(ATTRACTION_NAME,
                    TIME_FROM_LOCAL_TIME.plusMinutes((long) i * SLOT_DURATION_MINUTES),
                    SLOT_CAPACITY, 0, 1));
        }

        when(attractionHandler.getAvailabilityForAllAttractions(eq(VALID_DAY_OF_YEAR), any(), any()))
                .thenReturn(availabilityResults);

        bookingService.checkAttractionAvailability(AvailabilityRequest.newBuilder()
                        .setDayOfYear(VALID_DAY_OF_YEAR)
                        .setSlotFrom(TIME_FROM_STRING)
                        .setSlotTo(TIME_TO_STRING)
                        .build(),
                responseObserver);

        ArgumentCaptor<AvailabilityResponse> responseCaptor = ArgumentCaptor.forClass(AvailabilityResponse.class);
        Mockito.verify(responseObserver).onNext(responseCaptor.capture());
        AvailabilityResponse capturedResponse = responseCaptor.getValue();

        assertEquals(TOTAL_SLOTS, capturedResponse.getSlotList().size());
        verify(responseObserver).onCompleted();

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
        doThrow(new AttractionNotFoundException())
                .when(attractionHandler).confirmReservation(
                        eq(NON_EXISTING_ATTRACTION_NAME),
                        eq(VISITOR_ID),
                        eq(VALID_DAY_OF_YEAR),
                        eq(TIME_FROM_LOCAL_TIME));

        assertThrows(AttractionNotFoundException.class, () -> {
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
    public void testConfirmReservationFailureCapacityNotDefined() {
        doThrow(new CapacityNotDefinedException())
                .when(attractionHandler).confirmReservation(
                        eq(ATTRACTION_NAME),
                        eq(VISITOR_ID),
                        eq(VALID_DAY_OF_YEAR),
                        eq(TIME_FROM_LOCAL_TIME));

        assertThrows(CapacityNotDefinedException.class, () -> {
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
        doThrow(new ReservationAlreadyConfirmedException())
                .when(attractionHandler).confirmReservation(
                        eq(ATTRACTION_NAME),
                        eq(VISITOR_ID),
                        eq(VALID_DAY_OF_YEAR),
                        eq(TIME_FROM_LOCAL_TIME));

        assertThrows(ReservationAlreadyConfirmedException.class, () -> {
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
        doThrow(new ReservationNotFoundException())
                .when(attractionHandler).confirmReservation(
                        eq(ATTRACTION_NAME),
                        eq(VISITOR_ID),
                        eq(VALID_DAY_OF_YEAR),
                        eq(TIME_FROM_LOCAL_TIME));

        assertThrows(ReservationNotFoundException.class, () -> {
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
        doThrow(new InvalidSlotException())
                .when(attractionHandler).confirmReservation(
                        eq(ATTRACTION_NAME),
                        eq(VISITOR_ID),
                        eq(VALID_DAY_OF_YEAR),
                        eq(TIME_TO_LOCAL_TIME));

        assertThrows(InvalidSlotException.class, () -> {
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
        doThrow(new MissingPassException())
                .when(attractionHandler).confirmReservation(
                        eq(ATTRACTION_NAME),
                        eq(VISITOR_ID),
                        eq(VALID_DAY_OF_YEAR),
                        eq(TIME_FROM_LOCAL_TIME));

        assertThrows(MissingPassException.class, () -> {
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
        doThrow(new MissingPassException())
                .when(attractionHandler).confirmReservation(
                        eq(ATTRACTION_NAME),
                        eq(VISITOR_ID),
                        eq(VALID_DAY_OF_YEAR),
                        eq(LocalTime.parse(HALF_DAY_TIME_RESTRICTION_LIMIT)));

        assertThrows(MissingPassException.class, () -> {
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
        StreamObserver<Empty> responseObserver = Mockito.mock(StreamObserver.class);

        doNothing()
                .when(attractionHandler).confirmReservation(
                        eq(ATTRACTION_NAME),
                        eq(VISITOR_ID),
                        eq(VALID_DAY_OF_YEAR),
                        eq(TIME_FROM_LOCAL_TIME));

        bookingService.confirmReservation(BookingRequest.newBuilder()
                        .setAttractionName(ATTRACTION_NAME)
                        .setDayOfYear(VALID_DAY_OF_YEAR)
                        .setSlot(ParseUtils.formatTime(TIME_FROM_LOCAL_TIME))
                        .setVisitorId(VISITOR_ID.toString())
                        .build(),
                responseObserver);

        verify(responseObserver).onNext(any(Empty.class));
        verify(responseObserver).onCompleted();
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
        doThrow(new AttractionNotFoundException())
                .when(attractionHandler).cancelReservation(
                        eq(NON_EXISTING_ATTRACTION_NAME),
                        eq(VISITOR_ID),
                        eq(VALID_DAY_OF_YEAR),
                        eq(TIME_FROM_LOCAL_TIME));

        assertThrows(AttractionNotFoundException.class, () -> {
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
        doThrow(new InvalidSlotException())
                .when(attractionHandler).cancelReservation(
                        eq(ATTRACTION_NAME),
                        eq(VISITOR_ID),
                        eq(VALID_DAY_OF_YEAR),
                        eq(TIME_TO_LOCAL_TIME));

        assertThrows(InvalidSlotException.class, () -> {
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
        doThrow(new ReservationNotFoundException())
                .when(attractionHandler).cancelReservation(
                        eq(ATTRACTION_NAME),
                        eq(VISITOR_ID),
                        eq(VALID_DAY_OF_YEAR),
                        eq(TIME_FROM_LOCAL_TIME));

        assertThrows(ReservationNotFoundException.class, () -> {
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
        StreamObserver<Empty> responseObserver = Mockito.mock(StreamObserver.class);

        doNothing()
                .when(attractionHandler).cancelReservation(
                        eq(ATTRACTION_NAME),
                        eq(VISITOR_ID),
                        eq(VALID_DAY_OF_YEAR),
                        eq(TIME_FROM_LOCAL_TIME));

        bookingService.cancelReservation(BookingRequest.newBuilder()
                        .setAttractionName(ATTRACTION_NAME)
                        .setDayOfYear(VALID_DAY_OF_YEAR)
                        .setSlot(ParseUtils.formatTime(TIME_FROM_LOCAL_TIME))
                        .setVisitorId(VISITOR_ID.toString())
                        .build(),
                responseObserver);

        verify(responseObserver).onNext(any(Empty.class));
        verify(responseObserver).onCompleted();
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
        when(attractionHandler.makeReservation(
                eq(NON_EXISTING_ATTRACTION_NAME),
                eq(VISITOR_ID),
                eq(VALID_DAY_OF_YEAR),
                eq(TIME_FROM_LOCAL_TIME)))
                .thenThrow(new AttractionNotFoundException());

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
        when(attractionHandler.makeReservation(
                eq(ATTRACTION_NAME),
                eq(VISITOR_ID),
                eq(VALID_DAY_OF_YEAR),
                eq(TIME_TO_LOCAL_TIME)))
                .thenThrow(new InvalidSlotException());

        assertThrows(InvalidSlotException.class, () -> {
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
        when(attractionHandler.makeReservation(
                eq(ATTRACTION_NAME),
                eq(VISITOR_ID),
                eq(VALID_DAY_OF_YEAR),
                eq(TIME_FROM_LOCAL_TIME)))
                .thenThrow(new MissingPassException());

        assertThrows(MissingPassException.class, () -> {
            bookingService.reserveAttraction(BookingRequest.newBuilder()
                            .setAttractionName(ATTRACTION_NAME)
                            .setDayOfYear(VALID_DAY_OF_YEAR)
                            .setSlot(ParseUtils.formatTime(TIME_FROM_LOCAL_TIME))
                            .setVisitorId(VISITOR_ID.toString())
                            .build(),
                    Mockito.mock(StreamObserver.class));
        });
    }

    @Test
    public void testReserveAttractionFailureHalfDayPassRestriction() {
        when(attractionHandler.makeReservation(
                eq(ATTRACTION_NAME),
                eq(VISITOR_ID),
                eq(VALID_DAY_OF_YEAR),
                eq(LocalTime.parse(HALF_DAY_TIME_RESTRICTION_LIMIT))))
                .thenThrow(new MissingPassException());

        assertThrows(MissingPassException.class, () -> {
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
        when(attractionHandler.makeReservation(
                eq(ATTRACTION_NAME),
                eq(VISITOR_ID),
                eq(VALID_DAY_OF_YEAR),
                eq(TIME_FROM_LOCAL_TIME)))
                .thenThrow(new MissingPassException());

        assertThrows(MissingPassException.class, () -> {
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
        StreamObserver<ReservationResponse> responseObserver = Mockito.mock(StreamObserver.class);

        Reservation mockReservation = Mockito.mock(Reservation.class);
        when(attractionHandler.makeReservation(
                eq(ATTRACTION_NAME),
                eq(VISITOR_ID),
                eq(VALID_DAY_OF_YEAR),
                eq(TIME_FROM_LOCAL_TIME)))
                .thenReturn(new MakeReservationResult(mockReservation, false));

        bookingService.reserveAttraction(BookingRequest.newBuilder()
                        .setAttractionName(ATTRACTION_NAME)
                        .setDayOfYear(VALID_DAY_OF_YEAR)
                        .setSlot(TIME_FROM_STRING)
                        .setVisitorId(VISITOR_ID.toString())
                        .build(),
                responseObserver);

        ArgumentCaptor<ReservationResponse> responseCaptor = ArgumentCaptor.forClass(ReservationResponse.class);
        verify(responseObserver).onNext(responseCaptor.capture());
        ReservationResponse capturedResponse = responseCaptor.getValue();

        assertEquals(BookingState.RESERVATION_STATUS_PENDING, capturedResponse.getState());
        verify(responseObserver).onCompleted();
    }

    @Test
    public void testReserveAttractionSuccessCapacityDefined() {
        StreamObserver<ReservationResponse> responseObserver = Mockito.mock(StreamObserver.class);

        Reservation mockReservation = Mockito.mock(Reservation.class);
        when(attractionHandler.makeReservation(
                eq(ATTRACTION_NAME),
                eq(VISITOR_ID),
                eq(VALID_DAY_OF_YEAR),
                eq(TIME_FROM_LOCAL_TIME)))
                .thenReturn(new MakeReservationResult(mockReservation, true));

        bookingService.reserveAttraction(BookingRequest.newBuilder()
                        .setAttractionName(ATTRACTION_NAME)
                        .setDayOfYear(VALID_DAY_OF_YEAR)
                        .setSlot(TIME_FROM_STRING)
                        .setVisitorId(VISITOR_ID.toString())
                        .build(),
                responseObserver);

        ArgumentCaptor<ReservationResponse> responseCaptor = ArgumentCaptor.forClass(ReservationResponse.class);
        verify(responseObserver).onNext(responseCaptor.capture());
        ReservationResponse capturedResponse = responseCaptor.getValue();

        assertEquals(BookingState.RESERVATION_STATUS_CONFIRMED, capturedResponse.getState());
        verify(responseObserver).onCompleted();
    }

    @Test
    public void testReserveAttractionFailureMaxCapacityReached() {
        when(attractionHandler.makeReservation(
                eq(ATTRACTION_NAME),
                eq(VISITOR_ID),
                eq(VALID_DAY_OF_YEAR),
                eq(TIME_FROM_LOCAL_TIME)))
                .thenThrow(new OutOfCapacityException());

        assertThrows(OutOfCapacityException.class, () -> {
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
        when(attractionHandler.makeReservation(
                eq(ATTRACTION_NAME),
                eq(VISITOR_ID),
                eq(VALID_DAY_OF_YEAR),
                eq(TIME_FROM_LOCAL_TIME)))
                .thenThrow(new ReservationAlreadyExistsException());

        assertThrows(ReservationAlreadyExistsException.class, () -> {
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
