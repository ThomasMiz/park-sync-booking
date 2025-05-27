package edu.itba.serverdps.integration;

import com.google.protobuf.Empty;
import edu.itba.serverdps.adapter.driving.AdminServiceImpl;
import edu.itba.serverdps.adapter.driving.BookingServiceImpl;
import edu.itba.serverdps.application.Application;
import edu.itba.serverdps.port.driving.grpc.*;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = Application.class)
public class BookingIntegrationTest {

    @Autowired
    private AdminServiceImpl adminService;

    @Autowired
    private BookingServiceImpl bookingService;

    @Test
    public void testEndToEndBookingFlow() throws Exception {
        String attractionName = "Test Attraction";
        String openingTime = "09:00";
        String closingTime = "18:00";
        int slotDuration = 30;

        AddAttractionRequest addAttractionRequest = AddAttractionRequest.newBuilder()
                .setName(attractionName)
                .setOpeningTime(openingTime)
                .setClosingTime(closingTime)
                .setSlotDurationMinutes(slotDuration)
                .build();

        CountDownLatch addAttractionLatch = new CountDownLatch(1);
        StreamObserver<Empty> addAttractionObserver = new StreamObserver<>() {
            @Override
            public void onNext(Empty value) {
            }

            @Override
            public void onError(Throwable t) {
                fail("Failed to add attraction: " + t.getMessage());
                addAttractionLatch.countDown();
            }

            @Override
            public void onCompleted() {
                addAttractionLatch.countDown();
            }
        };

        adminService.addAttraction(addAttractionRequest, addAttractionObserver);
        assertTrue(addAttractionLatch.await(5, TimeUnit.SECONDS), "Add attraction timed out");

        CountDownLatch getAttractionsLatch = new CountDownLatch(1);
        final GetAttractionsResponse[] getAttractionsResponseHolder = new GetAttractionsResponse[1];
        StreamObserver<GetAttractionsResponse> getAttractionsObserver = new StreamObserver<>() {
            @Override
            public void onNext(GetAttractionsResponse value) {
                getAttractionsResponseHolder[0] = value;
            }

            @Override
            public void onError(Throwable t) {
                fail("Failed to get attractions: " + t.getMessage());
                getAttractionsLatch.countDown();
            }

            @Override
            public void onCompleted() {
                getAttractionsLatch.countDown();
            }
        };

        bookingService.getAttractions(Empty.getDefaultInstance(), getAttractionsObserver);
        assertTrue(getAttractionsLatch.await(5, TimeUnit.SECONDS), "Get attractions timed out");

        GetAttractionsResponse getAttractionsResponse = getAttractionsResponseHolder[0];
        boolean attractionFound = false;
        for (Attraction attraction : getAttractionsResponse.getAttractionList()) {
            if (attraction.getName().equals(attractionName)) {
                attractionFound = true;
                assertEquals(openingTime, attraction.getOpeningTime());
                assertEquals(closingTime, attraction.getClosingTime());
                break;
            }
        }
        assertTrue(attractionFound, "Attraction should be found in the list");

        UUID visitorId = UUID.randomUUID();
        int dayOfYear = 100;
        PassType passType = PassType.PASS_TYPE_FULL_DAY;

        AddTicketRequest addTicketRequest = AddTicketRequest.newBuilder()
                .setVisitorId(visitorId.toString())
                .setDayOfYear(dayOfYear)
                .setPassType(passType)
                .build();

        CountDownLatch addTicketLatch = new CountDownLatch(1);
        StreamObserver<Empty> addTicketObserver = new StreamObserver<>() {
            @Override
            public void onNext(Empty value) {
            }

            @Override
            public void onError(Throwable t) {
                fail("Failed to add ticket: " + t.getMessage());
                addTicketLatch.countDown();
            }

            @Override
            public void onCompleted() {
                addTicketLatch.countDown();
            }
        };

        adminService.addTicket(addTicketRequest, addTicketObserver);
        assertTrue(addTicketLatch.await(5, TimeUnit.SECONDS), "Add ticket timed out");

        int slotCapacity = 10;
        AddCapacityRequest addCapacityRequest = AddCapacityRequest.newBuilder()
                .setAttractionName(attractionName)
                .setDayOfYear(dayOfYear)
                .setCapacity(slotCapacity)
                .build();

        CountDownLatch addCapacityLatch = new CountDownLatch(1);
        final AddCapacityResponse[] addCapacityResponseHolder = new AddCapacityResponse[1];
        StreamObserver<AddCapacityResponse> addCapacityObserver = new StreamObserver<>() {
            @Override
            public void onNext(AddCapacityResponse value) {
                addCapacityResponseHolder[0] = value;
            }

            @Override
            public void onError(Throwable t) {
                fail("Failed to add capacity: " + t.getMessage());
                addCapacityLatch.countDown();
            }

            @Override
            public void onCompleted() {
                addCapacityLatch.countDown();
            }
        };

        adminService.addCapacity(addCapacityRequest, addCapacityObserver);
        assertTrue(addCapacityLatch.await(5, TimeUnit.SECONDS), "Add capacity timed out");

        String slotTime = "10:00";
        AvailabilityRequest availabilityRequest = AvailabilityRequest.newBuilder()
                .setAttractionName(attractionName)
                .setDayOfYear(dayOfYear)
                .setSlotFrom(slotTime)
                .build();

        CountDownLatch availabilityLatch = new CountDownLatch(1);
        final AvailabilityResponse[] availabilityResponseHolder = new AvailabilityResponse[1];
        StreamObserver<AvailabilityResponse> availabilityObserver = new StreamObserver<>() {
            @Override
            public void onNext(AvailabilityResponse value) {
                availabilityResponseHolder[0] = value;
            }

            @Override
            public void onError(Throwable t) {
                fail("Failed to check availability: " + t.getMessage());
                availabilityLatch.countDown();
            }

            @Override
            public void onCompleted() {
                availabilityLatch.countDown();
            }
        };

        bookingService.checkAttractionAvailability(availabilityRequest, availabilityObserver);
        assertTrue(availabilityLatch.await(5, TimeUnit.SECONDS), "Check availability timed out");

        AvailabilityResponse availabilityResponse = availabilityResponseHolder[0];
        assertEquals(1, availabilityResponse.getSlotCount());
        AvailabilitySlot slot = availabilityResponse.getSlot(0);
        assertEquals(attractionName, slot.getAttractionName());
        assertEquals(slotTime, slot.getSlot());
        assertEquals(slotCapacity, slot.getSlotCapacity());
        assertEquals(0, slot.getBookingsConfirmed());
        assertEquals(0, slot.getBookingsPending());

        BookingRequest bookingRequest = BookingRequest.newBuilder()
                .setAttractionName(attractionName)
                .setDayOfYear(dayOfYear)
                .setVisitorId(visitorId.toString())
                .setSlot(slotTime)
                .build();

        CountDownLatch reservationLatch = new CountDownLatch(1);
        final ReservationResponse[] reservationResponseHolder = new ReservationResponse[1];
        StreamObserver<ReservationResponse> reservationObserver = new StreamObserver<>() {
            @Override
            public void onNext(ReservationResponse value) {
                reservationResponseHolder[0] = value;
            }

            @Override
            public void onError(Throwable t) {
                fail("Failed to make reservation: " + t.getMessage());
                reservationLatch.countDown();
            }

            @Override
            public void onCompleted() {
                reservationLatch.countDown();
            }
        };

        bookingService.reserveAttraction(bookingRequest, reservationObserver);
        assertTrue(reservationLatch.await(5, TimeUnit.SECONDS), "Make reservation timed out");

        ReservationResponse reservationResponse = reservationResponseHolder[0];
        assertEquals(BookingState.RESERVATION_STATUS_CONFIRMED, reservationResponse.getState());

        CountDownLatch availabilityAfterLatch = new CountDownLatch(1);
        final AvailabilityResponse[] availabilityAfterResponseHolder = new AvailabilityResponse[1];
        StreamObserver<AvailabilityResponse> availabilityAfterObserver = new StreamObserver<>() {
            @Override
            public void onNext(AvailabilityResponse value) {
                availabilityAfterResponseHolder[0] = value;
            }

            @Override
            public void onError(Throwable t) {
                fail("Failed to check availability after booking: " + t.getMessage());
                availabilityAfterLatch.countDown();
            }

            @Override
            public void onCompleted() {
                availabilityAfterLatch.countDown();
            }
        };

        bookingService.checkAttractionAvailability(availabilityRequest, availabilityAfterObserver);
        assertTrue(availabilityAfterLatch.await(5, TimeUnit.SECONDS), "Check availability after booking timed out");

        AvailabilityResponse availabilityResponseAfterBooking = availabilityAfterResponseHolder[0];
        assertEquals(1, availabilityResponseAfterBooking.getSlotCount());
        AvailabilitySlot slotAfterBooking = availabilityResponseAfterBooking.getSlot(0);
        assertEquals(1, slotAfterBooking.getBookingsConfirmed());
        assertEquals(0, slotAfterBooking.getBookingsPending());
    }
}
