package edu.itba.serverdps.services;

import com.google.protobuf.Empty;
import edu.itba.serverdps.adapter.driving.AdminServiceImpl;
import edu.itba.serverdps.application.exceptions.*;
import edu.itba.serverdps.domain.model.TicketType;
import edu.itba.serverdps.domain.model.result.DefineSlotCapacityResult;
import edu.itba.serverdps.domain.usecase.handler.AttractionHandler;
import edu.itba.serverdps.port.driving.grpc.*;
import io.grpc.stub.StreamObserver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalTime;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AdminServiceImplTest {
    private static final String ATTRACTION_NAME = "attractionName";
    private static final String ANOTHER_ATTRACTION_NAME = "anotherAttractionName";
    private static final String INVALID_ATTRACTION_NAME = "";
    private static final String OPENING_TIME = "10:00";
    private static final String CLOSING_TIME = "18:00";
    private static final LocalTime TIME_FROM_LOCAL_TIME = LocalTime.parse(OPENING_TIME);
    private static final LocalTime TIME_TO_LOCAL_TIME = LocalTime.parse(CLOSING_TIME);
    private static final int SLOT_GAP = 10;
    private static final int TOTAL_SLOTS = (TIME_TO_LOCAL_TIME.toSecondOfDay() - TIME_FROM_LOCAL_TIME.toSecondOfDay()) / (SLOT_GAP * 60);
    private static final String INVALID_HOURS_FROM = "25:00";
    private static final String INVALID_HOURS_TO = "18:61";
    private static final String INVALID_HOURS_FROM_FORMAT = "10:0";
    private static final String INVALID_HOURS_TO_FORMAT = "10:00:00";
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

    @Mock
    private AttractionHandler attractionHandler;

    @InjectMocks
    private AdminServiceImpl adminService;

    @Test
    public void testAddAttraction() {
        StreamObserver<Empty> responseObserver = mock(StreamObserver.class);

        // Prepare request
        final AddAttractionRequest request = AddAttractionRequest.newBuilder()
                .setName(ATTRACTION_NAME)
                .setClosingTime(CLOSING_TIME)
                .setOpeningTime(OPENING_TIME)
                .setSlotDurationMinutes(SLOT_GAP)
                .build();

        // Call the service
        adminService.addAttraction(request, responseObserver);

        // Verify that the handler was called with correct parameters
        verify(attractionHandler).createAttraction(
                eq(ATTRACTION_NAME),
                eq(LocalTime.parse(OPENING_TIME)),
                eq(LocalTime.parse(CLOSING_TIME)),
                eq(SLOT_GAP)
        );

        // Verify that the response observer was completed successfully
        verify(responseObserver).onNext(Empty.getDefaultInstance());
        verify(responseObserver).onCompleted();
    }

    @Test
    public void testAddAnotherAttraction() {
        StreamObserver<Empty> responseObserver = mock(StreamObserver.class);

        // Prepare request for a second attraction
        final AddAttractionRequest request = AddAttractionRequest.newBuilder()
                .setName(ATTRACTION_NAME)
                .setClosingTime(CLOSING_TIME)
                .setOpeningTime(OPENING_TIME)
                .setSlotDurationMinutes(SLOT_GAP)
                .build();

        // Call the service
        adminService.addAttraction(request, responseObserver);

        // Verify that the handler was called with correct parameters
        verify(attractionHandler).createAttraction(
                eq(ATTRACTION_NAME),
                eq(LocalTime.parse(OPENING_TIME)),
                eq(LocalTime.parse(CLOSING_TIME)),
                eq(SLOT_GAP)
        );

        // Verify that the response observer was completed successfully
        verify(responseObserver).onNext(Empty.getDefaultInstance());
        verify(responseObserver).onCompleted();
    }

    @Test
    public void testAddAttractionWithExistingName() {
        StreamObserver<Empty> responseObserver = mock(StreamObserver.class);

        // Prepare request
        final AddAttractionRequest request = AddAttractionRequest.newBuilder()
                .setName(ATTRACTION_NAME)
                .setOpeningTime(OPENING_TIME)
                .setClosingTime(CLOSING_TIME)
                .setSlotDurationMinutes(SLOT_GAP)
                .build();

        // Set up mock to throw exception
        doThrow(new AttractionAlreadyExistsException())
                .when(attractionHandler)
                .createAttraction(
                        eq(ATTRACTION_NAME),
                        eq(LocalTime.parse(OPENING_TIME)),
                        eq(LocalTime.parse(CLOSING_TIME)),
                        eq(SLOT_GAP)
                );

        // Verify the exception is thrown
        assertThrows(AttractionAlreadyExistsException.class,
                () -> adminService.addAttraction(request, responseObserver));

        // Verify that the observer was not completed
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
    }

    @Test
    public void testAddAttractionWithInvalidName() {
        StreamObserver<Empty> responseObserver = mock(StreamObserver.class);

        // Prepare request with invalid name
        final AddAttractionRequest request = AddAttractionRequest.newBuilder()
                .setName(INVALID_ATTRACTION_NAME)
                .setOpeningTime(OPENING_TIME)
                .setClosingTime(CLOSING_TIME)
                .setSlotDurationMinutes(SLOT_GAP)
                .build();

        // Verify the exception is thrown directly from the service
        assertThrows(EmptyAttractionException.class,
                () -> adminService.addAttraction(request, responseObserver));

        // Verify that the handler was never called
        verify(attractionHandler, never()).createAttraction(any(), any(), any(), anyInt());

        // Verify the observer was not completed
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
    }

    @Test
    public void testAddAttractionWithInvalidSlotGapNoMinutes() {
        StreamObserver<Empty> responseObserver = mock(StreamObserver.class);

        // Prepare request with invalid duration
        final AddAttractionRequest request = AddAttractionRequest.newBuilder()
                .setName(ATTRACTION_NAME)
                .setOpeningTime(OPENING_TIME)
                .setClosingTime(CLOSING_TIME)
                .setSlotDurationMinutes(NO_SLOT_GAP)
                .build();

        // Verify the exception is thrown directly from the service
        assertThrows(InvalidDurationException.class,
                () -> adminService.addAttraction(request, responseObserver));

        // Verify that the handler was never called
        verify(attractionHandler, never()).createAttraction(any(), any(), any(), anyInt());

        // Verify the observer was not completed
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
    }

    @Test
    public void testAddAttractionWithInvalidSlotGapNegativeMinutes() {
        StreamObserver<Empty> responseObserver = mock(StreamObserver.class);

        // Prepare request with negative duration
        final AddAttractionRequest request = AddAttractionRequest.newBuilder()
                .setName(ATTRACTION_NAME)
                .setOpeningTime(OPENING_TIME)
                .setClosingTime(CLOSING_TIME)
                .setSlotDurationMinutes(NEGATIVE_SLOT_GAP)
                .build();

        // Verify the exception is thrown directly from the service
        assertThrows(InvalidDurationException.class,
                () -> adminService.addAttraction(request, responseObserver));

        // Verify that the handler was never called
        verify(attractionHandler, never()).createAttraction(any(), any(), any(), anyInt());

        // Verify the observer was not completed
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
    }

    @Test
    public void testAddAttractionWithInvalidHours() {
        StreamObserver<Empty> responseObserver = mock(StreamObserver.class);

        // Prepare request with invalid hours (closing before opening)
        final AddAttractionRequest request = AddAttractionRequest.newBuilder()
                .setName(ATTRACTION_NAME)
                .setOpeningTime(CLOSING_TIME)
                .setClosingTime(OPENING_TIME)
                .setSlotDurationMinutes(SLOT_GAP)
                .build();

        // Verify the exception is thrown directly from the service
        assertThrows(InvalidOpeningAndClosingTimeException.class,
                () -> adminService.addAttraction(request, responseObserver));

        // Verify that the handler was never called
        verify(attractionHandler, never()).createAttraction(any(), any(), any(), anyInt());

        // Verify the observer was not completed
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
    }

    @Test
    public void testAddAttractionWithInvalidHourForm() {
        StreamObserver<Empty> responseObserver = mock(StreamObserver.class);

        // Prepare request with invalid opening time
        final AddAttractionRequest request = AddAttractionRequest.newBuilder()
                .setName(ATTRACTION_NAME)
                .setOpeningTime(INVALID_HOURS_FROM)
                .setClosingTime(CLOSING_TIME)
                .setSlotDurationMinutes(SLOT_GAP)
                .build();

        // Verify the exception is thrown from the time parsing
        assertThrows(InvalidSlotException.class,
                () -> adminService.addAttraction(request, responseObserver));

        // Verify that the handler was never called
        verify(attractionHandler, never()).createAttraction(any(), any(), any(), anyInt());

        // Verify the observer was not completed
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
    }

    @Test
    public void testAddAttractionWithInvalidHourTo() {
        StreamObserver<Empty> responseObserver = mock(StreamObserver.class);

        // Prepare request with invalid closing time
        final AddAttractionRequest request = AddAttractionRequest.newBuilder()
                .setName(ATTRACTION_NAME)
                .setOpeningTime(OPENING_TIME)
                .setClosingTime(INVALID_HOURS_TO)
                .setSlotDurationMinutes(SLOT_GAP)
                .build();

        // Verify the exception is thrown from the time parsing
        assertThrows(InvalidSlotException.class,
                () -> adminService.addAttraction(request, responseObserver));

        // Verify that the handler was never called
        verify(attractionHandler, never()).createAttraction(any(), any(), any(), anyInt());

        // Verify the observer was not completed
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
    }

    @Test
    public void testAddAttractionWithInvalidHourFormFormat() {
        StreamObserver<Empty> responseObserver = mock(StreamObserver.class);

        // Prepare request with invalid opening time format
        final AddAttractionRequest request = AddAttractionRequest.newBuilder()
                .setName(ATTRACTION_NAME)
                .setOpeningTime(INVALID_HOURS_FROM_FORMAT)
                .setClosingTime(CLOSING_TIME)
                .setSlotDurationMinutes(SLOT_GAP)
                .build();

        // Verify the exception is thrown from the time parsing
        assertThrows(InvalidSlotException.class,
                () -> adminService.addAttraction(request, responseObserver));

        // Verify that the handler was never called
        verify(attractionHandler, never()).createAttraction(any(), any(), any(), anyInt());

        // Verify the observer was not completed
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
    }

    @Test
    public void testAddAttractionWithInvalidHourToFormat() {
        StreamObserver<Empty> responseObserver = mock(StreamObserver.class);

        // Prepare request with invalid closing time format
        final AddAttractionRequest request = AddAttractionRequest.newBuilder()
                .setName(ATTRACTION_NAME)
                .setOpeningTime(OPENING_TIME)
                .setClosingTime(INVALID_HOURS_TO_FORMAT)
                .setSlotDurationMinutes(SLOT_GAP)
                .build();

        // Verify the exception is thrown from the time parsing
        assertThrows(InvalidSlotException.class,
                () -> adminService.addAttraction(request, responseObserver));

        // Verify that the handler was never called
        verify(attractionHandler, never()).createAttraction(any(), any(), any(), anyInt());

        // Verify the observer was not completed
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
    }

    @Test
    public void testAddTicket() {
        StreamObserver<Empty> responseObserver = mock(StreamObserver.class);

        // Prepare request
        AddTicketRequest request = AddTicketRequest.newBuilder()
                .setVisitorId(DEFAULT_VISITOR_ID_STRING)
                .setDayOfYear(VALID_DAY_OF_YEAR)
                .setPassType(PassType.PASS_TYPE_FULL_DAY)
                .build();

        // Call the service
        adminService.addTicket(request, responseObserver);

        // Verify that the handler was called with the correct arguments
        verify(attractionHandler).addTicket(
                eq(DEFAULT_VISITOR_ID_UUID),
                eq(VALID_DAY_OF_YEAR),
                eq(TicketType.FULL_DAY)
        );

        // Verify that the response observer was completed successfully
        verify(responseObserver).onNext(Empty.getDefaultInstance());
        verify(responseObserver).onCompleted();
    }

    @Test
    public void testAddTicketFailureMoreThan365Days() {
        StreamObserver<Empty> responseObserver = mock(StreamObserver.class);

        // Prepare request with invalid day
        AddTicketRequest request = AddTicketRequest.newBuilder()
                .setVisitorId(DEFAULT_VISITOR_ID_STRING)
                .setDayOfYear(INVALID_DAY_OF_YEAR)
                .setPassType(PassType.PASS_TYPE_FULL_DAY)
                .build();

        // Verify the exception is thrown directly from the service
        assertThrows(InvalidDayException.class,
                () -> adminService.addTicket(request, responseObserver));

        // Verify that the handler was never called
        verify(attractionHandler, never()).addTicket(any(), anyInt(), any());

        // Verify the observer was not completed
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
    }

    @Test
    public void testAddTicketFailureLessThan1Day() {
        StreamObserver<Empty> responseObserver = mock(StreamObserver.class);

        // Prepare request with invalid day
        AddTicketRequest request = AddTicketRequest.newBuilder()
                .setVisitorId(DEFAULT_VISITOR_ID_STRING)
                .setDayOfYear(OTHER_INVALID_DAY_OF_YEAR)
                .setPassType(PassType.PASS_TYPE_FULL_DAY)
                .build();

        // Verify the exception is thrown directly from the service
        assertThrows(InvalidDayException.class,
                () -> adminService.addTicket(request, responseObserver));

        // Verify that the handler was never called
        verify(attractionHandler, never()).addTicket(any(), anyInt(), any());

        // Verify the observer was not completed
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
    }

    @Test
    public void testAddSameTicketPassForSameDate() {
        StreamObserver<Empty> responseObserver = mock(StreamObserver.class);

        // Prepare request
        AddTicketRequest request = AddTicketRequest.newBuilder()
                .setVisitorId(DEFAULT_VISITOR_ID_STRING)
                .setDayOfYear(VALID_DAY_OF_YEAR)
                .setPassType(PassType.PASS_TYPE_FULL_DAY)
                .build();

        // Setup mock to throw exception
        doThrow(new TicketAlreadyExistsException())
                .when(attractionHandler)
                .addTicket(
                        eq(DEFAULT_VISITOR_ID_UUID),
                        eq(VALID_DAY_OF_YEAR),
                        eq(TicketType.FULL_DAY)
                );

        // Verify the exception is thrown
        assertThrows(TicketAlreadyExistsException.class,
                () -> adminService.addTicket(request, responseObserver));

        // Verify the observer was not completed
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
    }

    @Test
    public void testAddOtherPassForSameDate() {
        StreamObserver<Empty> responseObserver = mock(StreamObserver.class);

        // Prepare request with half day pass
        AddTicketRequest request = AddTicketRequest.newBuilder()
                .setVisitorId(DEFAULT_VISITOR_ID_STRING)
                .setDayOfYear(VALID_DAY_OF_YEAR)
                .setPassType(PassType.PASS_TYPE_HALF_DAY)
                .build();

        // Setup mock to throw exception
        doThrow(new TicketAlreadyExistsException())
                .when(attractionHandler)
                .addTicket(
                        eq(DEFAULT_VISITOR_ID_UUID),
                        eq(VALID_DAY_OF_YEAR),
                        eq(TicketType.HALF_DAY)
                );

        // Verify the exception is thrown
        assertThrows(TicketAlreadyExistsException.class,
                () -> adminService.addTicket(request, responseObserver));

        // Verify the observer was not completed
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
    }

    @Test
    public void testAddTicketForOtherDay() {
        StreamObserver<Empty> responseObserver = mock(StreamObserver.class);

        // Prepare request for a different day
        AddTicketRequest request = AddTicketRequest.newBuilder()
                .setVisitorId(DEFAULT_VISITOR_ID_STRING)
                .setDayOfYear(OTHER_VALID_DAY_OF_YEAR)
                .setPassType(PassType.PASS_TYPE_FULL_DAY)
                .build();

        // Setup mock to throw exception
        doThrow(new TicketAlreadyExistsException())
                .when(attractionHandler)
                .addTicket(
                        eq(DEFAULT_VISITOR_ID_UUID),
                        eq(OTHER_VALID_DAY_OF_YEAR),
                        eq(TicketType.FULL_DAY)
                );

        // Verify the exception is thrown
        assertThrows(TicketAlreadyExistsException.class,
                () -> adminService.addTicket(request, responseObserver));

        // Verify the observer was not completed
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
    }

    @Test
    public void testAddCapacityFailureMoreThan365Days() {
        StreamObserver<AddCapacityResponse> responseObserver = mock(StreamObserver.class);

        // Prepare request with invalid day
        AddCapacityRequest request = AddCapacityRequest.newBuilder()
                .setAttractionName(ATTRACTION_NAME)
                .setDayOfYear(INVALID_DAY_OF_YEAR)
                .setCapacity(VALID_CAPACITY)
                .build();

        // Verify the exception is thrown directly from the service
        assertThrows(InvalidDayException.class,
                () -> adminService.addCapacity(request, responseObserver));

        // Verify that the handler was never called
        verify(attractionHandler, never()).setSlotCapacityForAttraction(any(), anyInt(), anyInt());

        // Verify the observer was not completed
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
    }

    @Test
    public void testAddCapacityFailureLessThan1Day() {
        StreamObserver<AddCapacityResponse> responseObserver = mock(StreamObserver.class);

        // Prepare request with invalid day
        AddCapacityRequest request = AddCapacityRequest.newBuilder()
                .setAttractionName(ATTRACTION_NAME)
                .setDayOfYear(OTHER_INVALID_DAY_OF_YEAR)
                .setCapacity(VALID_CAPACITY)
                .build();

        // Verify the exception is thrown directly from the service
        assertThrows(InvalidDayException.class,
                () -> adminService.addCapacity(request, responseObserver));

        // Verify that the handler was never called
        verify(attractionHandler, never()).setSlotCapacityForAttraction(any(), anyInt(), anyInt());

        // Verify the observer was not completed
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
    }

    @Test
    public void testAddCapacityFailureNoAttractionName() {
        StreamObserver<AddCapacityResponse> responseObserver = mock(StreamObserver.class);

        // Prepare request with empty attraction name
        AddCapacityRequest request = AddCapacityRequest.newBuilder()
                .setAttractionName("")
                .setDayOfYear(VALID_DAY_OF_YEAR)
                .setCapacity(VALID_CAPACITY)
                .build();

        // Verify the exception is thrown directly from the service
        assertThrows(EmptyAttractionException.class,
                () -> adminService.addCapacity(request, responseObserver));

        // Verify that the handler was never called
        verify(attractionHandler, never()).setSlotCapacityForAttraction(any(), anyInt(), anyInt());

        // Verify the observer was not completed
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
    }

    @Test
    public void testAddCapacityFailureBlankAttractionName() {
        StreamObserver<AddCapacityResponse> responseObserver = mock(StreamObserver.class);

        // Prepare request with blank attraction name
        AddCapacityRequest request = AddCapacityRequest.newBuilder()
                .setAttractionName(" ")
                .setDayOfYear(VALID_DAY_OF_YEAR)
                .setCapacity(VALID_CAPACITY)
                .build();

        // Verify the exception is thrown directly from the service
        assertThrows(EmptyAttractionException.class,
                () -> adminService.addCapacity(request, responseObserver));

        // Verify that the handler was never called
        verify(attractionHandler, never()).setSlotCapacityForAttraction(any(), anyInt(), anyInt());

        // Verify the observer was not completed
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
    }

    @Test
    public void testAddCapacityFailureAttractionDoesNotExist() {
        StreamObserver<AddCapacityResponse> responseObserver = mock(StreamObserver.class);

        // Prepare request
        AddCapacityRequest request = AddCapacityRequest.newBuilder()
                .setAttractionName(ATTRACTION_NAME)
                .setDayOfYear(VALID_DAY_OF_YEAR)
                .setCapacity(VALID_CAPACITY)
                .build();

        // Setup mock to throw exception
        doThrow(new AttractionNotFoundException())
                .when(attractionHandler)
                .setSlotCapacityForAttraction(
                        eq(ATTRACTION_NAME),
                        eq(VALID_DAY_OF_YEAR),
                        eq(VALID_CAPACITY)
                );

        // Verify the exception is thrown
        assertThrows(AttractionNotFoundException.class,
                () -> adminService.addCapacity(request, responseObserver));

        // Verify the observer was not completed
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
    }

    @Test
    public void testAddCapacityFailureCapacityIsNegative() {
        StreamObserver<AddCapacityResponse> responseObserver = mock(StreamObserver.class);

        // Prepare request with negative capacity
        AddCapacityRequest request = AddCapacityRequest.newBuilder()
                .setAttractionName(ATTRACTION_NAME)
                .setDayOfYear(VALID_DAY_OF_YEAR)
                .setCapacity(INVALID_CAPACITY)
                .build();

        // Verify the exception is thrown directly from the service
        assertThrows(NegativeCapacityException.class,
                () -> adminService.addCapacity(request, responseObserver));

        // Verify that the handler was never called
        verify(attractionHandler, never()).setSlotCapacityForAttraction(any(), anyInt(), anyInt());

        // Verify the observer was not completed
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
    }

    @Test
    public void testAddCapacityFailureCapacityAlreadySet() {
        StreamObserver<AddCapacityResponse> responseObserver = mock(StreamObserver.class);

        // Prepare request
        AddCapacityRequest request = AddCapacityRequest.newBuilder()
                .setAttractionName(ATTRACTION_NAME)
                .setDayOfYear(VALID_DAY_OF_YEAR)
                .setCapacity(VALID_CAPACITY)
                .build();

        // Setup mock to throw exception
        doThrow(new CapacityAlreadyDefinedException())
                .when(attractionHandler)
                .setSlotCapacityForAttraction(
                        eq(ATTRACTION_NAME),
                        eq(VALID_DAY_OF_YEAR),
                        eq(VALID_CAPACITY)
                );

        // Verify the exception is thrown
        assertThrows(CapacityAlreadyDefinedException.class,
                () -> adminService.addCapacity(request, responseObserver));

        // Verify the observer was not completed
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
    }

    @Test
    public void testAddCapacitySuccess() {
        StreamObserver<AddCapacityResponse> responseObserver = mock(StreamObserver.class);

        // Prepare request
        AddCapacityRequest request = AddCapacityRequest.newBuilder()
                .setAttractionName(ATTRACTION_NAME)
                .setDayOfYear(VALID_DAY_OF_YEAR)
                .setCapacity(VALID_CAPACITY)
                .build();

        // Mock successful result with no changes
        DefineSlotCapacityResult mockResult = new DefineSlotCapacityResult(0, 0, 0);
        when(attractionHandler.setSlotCapacityForAttraction(
                eq(ATTRACTION_NAME),
                eq(VALID_DAY_OF_YEAR),
                eq(VALID_CAPACITY)
        )).thenReturn(mockResult);

        // Call the service
        adminService.addCapacity(request, responseObserver);

        // Verify handler was called with correct parameters
        verify(attractionHandler).setSlotCapacityForAttraction(
                eq(ATTRACTION_NAME),
                eq(VALID_DAY_OF_YEAR),
                eq(VALID_CAPACITY)
        );

        // Capture and verify the response
        ArgumentCaptor<AddCapacityResponse> responseCaptor = ArgumentCaptor.forClass(AddCapacityResponse.class);
        verify(responseObserver).onNext(responseCaptor.capture());
        verify(responseObserver).onCompleted();

        AddCapacityResponse capturedResponse = responseCaptor.getValue();
        assertEquals(0, capturedResponse.getConfirmedBookings());
        assertEquals(0, capturedResponse.getCancelledBookings());
        assertEquals(0, capturedResponse.getRelocatedBookings());
    }

    @Test
    public void testAddCapacityConfirmPendingRequests() {
        StreamObserver<AddCapacityResponse> responseObserver = mock(StreamObserver.class);

        // Prepare request
        AddCapacityRequest request = AddCapacityRequest.newBuilder()
                .setAttractionName(ATTRACTION_NAME)
                .setDayOfYear(VALID_DAY_OF_YEAR)
                .setCapacity(VALID_CAPACITY)
                .build();

        // Mock result with confirmed bookings
        DefineSlotCapacityResult mockResult = new DefineSlotCapacityResult(VALID_CAPACITY, 0, 0);
        when(attractionHandler.setSlotCapacityForAttraction(
                eq(ATTRACTION_NAME),
                eq(VALID_DAY_OF_YEAR),
                eq(VALID_CAPACITY)
        )).thenReturn(mockResult);

        // Call the service
        adminService.addCapacity(request, responseObserver);

        // Verify handler was called
        verify(attractionHandler).setSlotCapacityForAttraction(
                eq(ATTRACTION_NAME),
                eq(VALID_DAY_OF_YEAR),
                eq(VALID_CAPACITY)
        );

        // Capture and verify the response
        ArgumentCaptor<AddCapacityResponse> responseCaptor = ArgumentCaptor.forClass(AddCapacityResponse.class);
        verify(responseObserver).onNext(responseCaptor.capture());
        verify(responseObserver).onCompleted();

        AddCapacityResponse capturedResponse = responseCaptor.getValue();
        assertEquals(VALID_CAPACITY, capturedResponse.getConfirmedBookings());
        assertEquals(0, capturedResponse.getCancelledBookings());
        assertEquals(0, capturedResponse.getRelocatedBookings());
    }

    @Test
    public void testAddCapacityCancelPendingRequests() {
        StreamObserver<AddCapacityResponse> responseObserver = mock(StreamObserver.class);

        // Prepare request
        AddCapacityRequest request = AddCapacityRequest.newBuilder()
                .setAttractionName(ATTRACTION_NAME)
                .setDayOfYear(VALID_DAY_OF_YEAR)
                .setCapacity(VALID_CAPACITY)
                .build();

        // Mock result with confirmed and cancelled bookings
        int totalConfirmed = VALID_CAPACITY * TOTAL_SLOTS;
        DefineSlotCapacityResult mockResult = new DefineSlotCapacityResult(totalConfirmed, TOTAL_SLOTS, 0);
        when(attractionHandler.setSlotCapacityForAttraction(
                eq(ATTRACTION_NAME),
                eq(VALID_DAY_OF_YEAR),
                eq(VALID_CAPACITY)
        )).thenReturn(mockResult);

        // Call the service
        adminService.addCapacity(request, responseObserver);

        // Verify handler was called
        verify(attractionHandler).setSlotCapacityForAttraction(
                eq(ATTRACTION_NAME),
                eq(VALID_DAY_OF_YEAR),
                eq(VALID_CAPACITY)
        );

        // Capture and verify the response
        ArgumentCaptor<AddCapacityResponse> responseCaptor = ArgumentCaptor.forClass(AddCapacityResponse.class);
        verify(responseObserver).onNext(responseCaptor.capture());
        verify(responseObserver).onCompleted();

        AddCapacityResponse capturedResponse = responseCaptor.getValue();
        assertEquals(totalConfirmed, capturedResponse.getConfirmedBookings());
        assertEquals(0, capturedResponse.getCancelledBookings());
        assertEquals(TOTAL_SLOTS, capturedResponse.getRelocatedBookings());
    }

    @Test
    public void testAddCapacityRelocateBookingRequest() {
        StreamObserver<AddCapacityResponse> responseObserver = mock(StreamObserver.class);

        // Prepare request
        AddCapacityRequest request = AddCapacityRequest.newBuilder()
                .setAttractionName(ATTRACTION_NAME)
                .setDayOfYear(VALID_DAY_OF_YEAR)
                .setCapacity(VALID_CAPACITY)
                .build();

        // Mock result with confirmed and relocated bookings
        DefineSlotCapacityResult mockResult = new DefineSlotCapacityResult(VALID_CAPACITY, 0, VALID_CAPACITY);
        when(attractionHandler.setSlotCapacityForAttraction(
                eq(ATTRACTION_NAME),
                eq(VALID_DAY_OF_YEAR),
                eq(VALID_CAPACITY)
        )).thenReturn(mockResult);

        // Call the service
        adminService.addCapacity(request, responseObserver);

        // Verify handler was called
        verify(attractionHandler).setSlotCapacityForAttraction(
                eq(ATTRACTION_NAME),
                eq(VALID_DAY_OF_YEAR),
                eq(VALID_CAPACITY)
        );

        // Capture and verify the response
        ArgumentCaptor<AddCapacityResponse> responseCaptor = ArgumentCaptor.forClass(AddCapacityResponse.class);
        verify(responseObserver).onNext(responseCaptor.capture());
        verify(responseObserver).onCompleted();

        AddCapacityResponse capturedResponse = responseCaptor.getValue();
        assertEquals(VALID_CAPACITY, capturedResponse.getConfirmedBookings());
        assertEquals(VALID_CAPACITY, capturedResponse.getCancelledBookings());
        assertEquals(0, capturedResponse.getRelocatedBookings());
    }
}
