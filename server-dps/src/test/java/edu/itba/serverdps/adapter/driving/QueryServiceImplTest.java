package edu.itba.serverdps.adapter.driving;

import edu.itba.serverdps.application.exceptions.InvalidDayException;
import edu.itba.serverdps.domain.model.Attraction;
import edu.itba.serverdps.domain.model.ConfirmedReservation;
import edu.itba.serverdps.domain.model.Reservation;
import edu.itba.serverdps.domain.model.Ticket;
import edu.itba.serverdps.domain.model.TicketType;
import edu.itba.serverdps.domain.model.result.SuggestedCapacityResult;
import edu.itba.serverdps.domain.usecase.handler.AttractionHandler;
import edu.itba.serverdps.port.driving.grpc.ConfirmedReservationsResponse;
import edu.itba.serverdps.port.driving.grpc.DayOfYearRequest;
import edu.itba.serverdps.port.driving.grpc.SuggestedCapacitiesResponse;
import io.grpc.stub.StreamObserver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalTime;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class QueryServiceImplTest {
    private static final String ATTRACTION_NAME = "attractionName";
    private static final String ANOTHER_ATTRACTION_NAME = "anotherAttractionName";
    private static final UUID VISITOR_ID = UUID.randomUUID();
    private static final UUID ANOTHER_VISITOR_ID = UUID.randomUUID();
    private static final LocalTime SLOT_TIME = LocalTime.of(10, 30);
    private static final LocalTime ANOTHER_SLOT_TIME = LocalTime.of(14, 0);
    private static final int INVALID_DAY_OF_YEAR = 0;
    private static final int OTHER_INVALID_DAY_OF_YEAR = 366;
    private static final int VALID_DAY_OF_YEAR = 200;
    private static final int MAX_PENDING_RESERVATIONS = 10;
    private static final int ANOTHER_MAX_PENDING_RESERVATIONS = 5;
    private static final TicketType TICKET_TYPE = TicketType.FULL_DAY;

    @Mock
    private AttractionHandler attractionHandler;

    @InjectMocks
    private QueryServiceImpl queryService;

    @Test
    public void testGetSuggestedCapacitiesFailureLessThan1Day() {
        assertThrows(InvalidDayException.class, () -> {
            queryService.getSuggestedCapacities(DayOfYearRequest.newBuilder()
                            .setDayOfYear(INVALID_DAY_OF_YEAR)
                            .build(),
                    Mockito.mock(StreamObserver.class));
        });
    }

    @Test
    public void testGetSuggestedCapacitiesFailureMoreThan365Days() {
        assertThrows(InvalidDayException.class, () -> {
            queryService.getSuggestedCapacities(DayOfYearRequest.newBuilder()
                            .setDayOfYear(OTHER_INVALID_DAY_OF_YEAR)
                            .build(),
                    Mockito.mock(StreamObserver.class));
        });
    }

    @Test
    public void testGetSuggestedCapacitiesEmptyResults() {
        StreamObserver<SuggestedCapacitiesResponse> responseObserver = Mockito.mock(StreamObserver.class);

        when(attractionHandler.getSuggestedCapacities(eq(VALID_DAY_OF_YEAR)))
                .thenReturn(Collections.emptySortedSet());

        queryService.getSuggestedCapacities(DayOfYearRequest.newBuilder()
                        .setDayOfYear(VALID_DAY_OF_YEAR)
                        .build(),
                responseObserver);

        ArgumentCaptor<SuggestedCapacitiesResponse> responseCaptor = ArgumentCaptor.forClass(SuggestedCapacitiesResponse.class);
        verify(responseObserver).onNext(responseCaptor.capture());
        SuggestedCapacitiesResponse capturedResponse = responseCaptor.getValue();

        assertEquals(0, capturedResponse.getSuggestedCapacityCount());
        verify(responseObserver).onCompleted();
    }

    @Test
    public void testGetConfirmedReservationsFailureLessThan1Day() {
        assertThrows(InvalidDayException.class, () -> {
            queryService.getConfirmedReservations(DayOfYearRequest.newBuilder()
                            .setDayOfYear(INVALID_DAY_OF_YEAR)
                            .build(),
                    Mockito.mock(StreamObserver.class));
        });
    }

    @Test
    public void testGetConfirmedReservationsFailureMoreThan365Days() {
        assertThrows(InvalidDayException.class, () -> {
            queryService.getConfirmedReservations(DayOfYearRequest.newBuilder()
                            .setDayOfYear(OTHER_INVALID_DAY_OF_YEAR)
                            .build(),
                    Mockito.mock(StreamObserver.class));
        });
    }

    @Test
    public void testGetConfirmedReservationsEmptyResults() {
        StreamObserver<ConfirmedReservationsResponse> responseObserver = Mockito.mock(StreamObserver.class);

        when(attractionHandler.getConfirmedReservations(eq(VALID_DAY_OF_YEAR)))
                .thenReturn(Collections.emptySortedSet());

        queryService.getConfirmedReservations(DayOfYearRequest.newBuilder()
                        .setDayOfYear(VALID_DAY_OF_YEAR)
                        .build(),
                responseObserver);

        ArgumentCaptor<ConfirmedReservationsResponse> responseCaptor = ArgumentCaptor.forClass(ConfirmedReservationsResponse.class);
        verify(responseObserver).onNext(responseCaptor.capture());
        ConfirmedReservationsResponse capturedResponse = responseCaptor.getValue();

        assertEquals(0, capturedResponse.getConfirmedReservationCount());
        verify(responseObserver).onCompleted();
    }
}
