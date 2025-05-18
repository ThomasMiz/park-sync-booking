package edu.itba.serverdps.services;

import edu.itba.serverdps.grpc.*;
import edu.itba.serverdps.handlers.AttractionHandler;
import edu.itba.serverdps.models.ConfirmedReservation;
import edu.itba.serverdps.results.SuggestedCapacityResult;
import edu.itba.serverdps.utils.ParseUtils;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;

import java.util.SortedSet;

@GrpcService
public class QueryServiceImpl extends QueryServiceGrpc.QueryServiceImplBase {
    private final AttractionHandler attractionHandler;

    public QueryServiceImpl(AttractionHandler attractionHandler) {
        this.attractionHandler = attractionHandler;
    }

    @Override
    public void getSuggestedCapacities(DayOfYearRequest request, StreamObserver<SuggestedCapacitiesResponse> responseObserver) {
        int dayOfYear = ParseUtils.checkValidDayOfYear(request.getDayOfYear());

        SuggestedCapacitiesResponse.Builder responseBuilder = SuggestedCapacitiesResponse.newBuilder();

        SortedSet<SuggestedCapacityResult> results = attractionHandler.getSuggestedCapacities(dayOfYear);
        for (SuggestedCapacityResult result : results) {
            responseBuilder.addSuggestedCapacity(SuggestedCapacity.newBuilder()
                    .setAttractionName(result.attraction().getName())
                    .setMaxPendingReservations(result.maxPendingReservationCount())
                    .setSlotWithMaxReservations(ParseUtils.formatTime(result.slotTime()))
                    .build());
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getConfirmedReservations(DayOfYearRequest request, StreamObserver<ConfirmedReservationsResponse> responseObserver) {
        int dayOfYear = ParseUtils.checkValidDayOfYear(request.getDayOfYear());

        ConfirmedReservationsResponse.Builder responseBuilder = ConfirmedReservationsResponse.newBuilder();

        SortedSet<ConfirmedReservation> results = attractionHandler.getConfirmedReservations(dayOfYear);
        for (ConfirmedReservation result : results) {
            responseBuilder.addConfirmedReservation(edu.itba.serverdps.grpc.ConfirmedReservation.newBuilder()
                    .setAttractionName(result.getAttraction().getName())
                    .setVisitorId(result.getVisitorId().toString())
                    .setSlot(ParseUtils.formatTime(result.getSlotTime()))
                    .build()
            );
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }
}
