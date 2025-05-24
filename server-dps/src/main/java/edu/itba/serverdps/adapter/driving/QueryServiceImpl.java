package edu.itba.serverdps.adapter.driving;

import edu.itba.serverdps.port.driving.grpc.*;
import edu.itba.serverdps.domain.usecase.handler.AttractionHandler;
import edu.itba.serverdps.domain.model.ConfirmedReservation;
import edu.itba.serverdps.domain.model.result.SuggestedCapacityResult;
import edu.itba.serverdps.application.utils.ParseUtils;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.grpc.server.service.GrpcService;

import java.util.SortedSet;

@GrpcService
@RequiredArgsConstructor
public class QueryServiceImpl extends QueryServiceGrpc.QueryServiceImplBase {
    private final AttractionHandler attractionHandler;

    @Override
    public void getSuggestedCapacities(DayOfYearRequest request, StreamObserver<SuggestedCapacitiesResponse> responseObserver) {
        int dayOfYear = ParseUtils.checkValidDayOfYear(request.getDayOfYear());
        SortedSet<SuggestedCapacityResult> results = attractionHandler.getSuggestedCapacities(dayOfYear);

        SuggestedCapacitiesResponse.Builder responseBuilder = SuggestedCapacitiesResponse.newBuilder();
        for (SuggestedCapacityResult result : results) {
            responseBuilder.addSuggestedCapacity(buildSuggestedCapacityResponse(result));
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getConfirmedReservations(DayOfYearRequest request, StreamObserver<ConfirmedReservationsResponse> responseObserver) {
        int dayOfYear = ParseUtils.checkValidDayOfYear(request.getDayOfYear());
        SortedSet<ConfirmedReservation> results = attractionHandler.getConfirmedReservations(dayOfYear);

        ConfirmedReservationsResponse.Builder responseBuilder = ConfirmedReservationsResponse.newBuilder();
        for (ConfirmedReservation result : results) {
            responseBuilder.addConfirmedReservation(buildConfirmedReservationResponse(result));
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    private SuggestedCapacity buildSuggestedCapacityResponse(SuggestedCapacityResult result) {
        return SuggestedCapacity.newBuilder()
                .setAttractionName(result.attraction().name())
                .setMaxPendingReservations(result.maxPendingReservationCount())
                .setSlotWithMaxReservations(ParseUtils.formatTime(result.slotTime()))
                .build();
    }

    private edu.itba.serverdps.port.driving.grpc.ConfirmedReservation buildConfirmedReservationResponse(ConfirmedReservation result) {
        return edu.itba.serverdps.port.driving.grpc.ConfirmedReservation.newBuilder()
                .setAttractionName(result.attraction().name())
                .setVisitorId(result.visitorId().toString())
                .setSlot(ParseUtils.formatTime(result.slotTime()))
                .build();
    }
}
