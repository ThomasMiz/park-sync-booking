package edu.itba.serverdps.integration;

import com.google.protobuf.Empty;
import edu.itba.serverdps.adapter.driving.AdminServiceImpl;
import edu.itba.serverdps.adapter.driving.BookingServiceImpl;
import edu.itba.serverdps.application.Application;
import edu.itba.serverdps.application.exceptions.*;
import edu.itba.serverdps.port.driving.grpc.*;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = Application.class)
public class ErrorHandlingIntegrationTest {

    @Autowired
    private AdminServiceImpl adminService;

    @Autowired
    private BookingServiceImpl bookingService;

    @Test
    public void testInvalidAttractionName() throws Exception {
        String invalidAttractionName = "";
        String openingTime = "09:00";
        String closingTime = "18:00";
        int slotDuration = 30;

        AddAttractionRequest addAttractionRequest = AddAttractionRequest.newBuilder()
                .setName(invalidAttractionName)
                .setOpeningTime(openingTime)
                .setClosingTime(closingTime)
                .setSlotDurationMinutes(slotDuration)
                .build();

        Exception exception = assertThrows(EmptyAttractionException.class, () -> {
            adminService.addAttraction(addAttractionRequest, new StreamObserver<Empty>() {
                @Override
                public void onNext(Empty value) {}

                @Override
                public void onError(Throwable t) {}

                @Override
                public void onCompleted() {}
            });
        });

        assertNotNull(exception, "Expected EmptyAttractionException");
    }

    @Test
    public void testInvalidTimeFormat() throws Exception {
        String attractionName = "Test Attraction";
        String invalidOpeningTime = "9:00";
        String closingTime = "18:00";
        int slotDuration = 30;

        AddAttractionRequest addAttractionRequest = AddAttractionRequest.newBuilder()
                .setName(attractionName)
                .setOpeningTime(invalidOpeningTime)
                .setClosingTime(closingTime)
                .setSlotDurationMinutes(slotDuration)
                .build();

        Exception exception = assertThrows(InvalidSlotException.class, () -> {
            adminService.addAttraction(addAttractionRequest, new StreamObserver<Empty>() {
                @Override
                public void onNext(Empty value) {}

                @Override
                public void onError(Throwable t) {}

                @Override
                public void onCompleted() {}
            });
        });

        assertNotNull(exception, "Expected InvalidSlotException");
    }

    @Test
    public void testInvalidDayOfYear() throws Exception {
        String attractionName = "Test Attraction 2";
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

        int invalidDayOfYear = 366;
        int slotCapacity = 10;

        AddCapacityRequest addCapacityRequest = AddCapacityRequest.newBuilder()
                .setAttractionName(attractionName)
                .setDayOfYear(invalidDayOfYear)
                .setCapacity(slotCapacity)
                .build();

        Exception exception = assertThrows(InvalidDayException.class, () -> {
            adminService.addCapacity(addCapacityRequest, new StreamObserver<AddCapacityResponse>() {
                @Override
                public void onNext(AddCapacityResponse value) {}

                @Override
                public void onError(Throwable t) {}

                @Override
                public void onCompleted() {}
            });
        });

        assertNotNull(exception, "Expected InvalidDayException");
    }
}
