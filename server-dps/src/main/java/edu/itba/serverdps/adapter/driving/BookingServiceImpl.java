package edu.itba.serverdps.adapter.driving;

import edu.itba.serverdps.application.exceptions.CheckAvailabilityInvalidArgumentException;
import edu.itba.serverdps.application.exceptions.InvalidSlotException;
import edu.itba.serverdps.port.driving.grpc.*;
import edu.itba.serverdps.domain.usecase.handler.AttractionHandler;
import edu.itba.serverdps.domain.model.Attraction;
import edu.itba.serverdps.domain.model.result.AttractionAvailabilityResult;
import edu.itba.serverdps.domain.model.result.MakeReservationResult;
import edu.itba.serverdps.application.utils.ParseUtils;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;

import java.time.LocalTime;
import java.util.Collection;
import java.util.UUID;

@GrpcService
public class BookingServiceImpl extends BookingServiceGrpc.BookingServiceImplBase {

    private final AttractionHandler attractionHandler;

    public BookingServiceImpl(AttractionHandler attractionHandler) {
        this.attractionHandler = attractionHandler;
    }

    @Override
    public void getAttractions(Empty request, StreamObserver<GetAttractionsResponse> responseObserver) {
        GetAttractionsResponse.Builder responseBuilder = GetAttractionsResponse.newBuilder();

        Collection<Attraction> attractions = attractionHandler.getAttractions();
        for (Attraction attraction : attractions) {
            responseBuilder.addAttraction(edu.itba.serverdps.port.driving.grpc.Attraction.newBuilder()
                    .setName(attraction.getName())
                    .setClosingTime(ParseUtils.formatTime(attraction.getClosingTime()))
                    .setOpeningTime(ParseUtils.formatTime(attraction.getOpeningTime()))
                    .build());
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void checkAttractionAvailability(AvailabilityRequest request, StreamObserver<AvailabilityResponse> responseObserver) {
        int dayOfYear = ParseUtils.checkValidDayOfYear(request.getDayOfYear());
        LocalTime slotFrom = ParseUtils.parseTime(request.getSlotFrom());
        LocalTime slotTo;

        String attractionName = ParseUtils.checkAttractionNameOrNull(request.getAttractionName());
        slotTo = ParseUtils.parseTimeOrNull(request.getSlotTo());
        if (attractionName == null && slotTo == null)
            throw new CheckAvailabilityInvalidArgumentException();

        if (slotTo != null && slotFrom.isAfter(slotTo))
            throw new InvalidSlotException();

        Collection<AttractionAvailabilityResult> availabilityResults;
        if (attractionName != null) {
            availabilityResults = attractionHandler.getAvailabilityForAttraction(attractionName, dayOfYear, slotFrom, slotTo);
        } else {
            availabilityResults = attractionHandler.getAvailabilityForAllAttractions(dayOfYear, slotFrom, slotTo);
        }

        AvailabilityResponse.Builder responseBuilder = AvailabilityResponse.newBuilder();

        for (AttractionAvailabilityResult availabilityResult : availabilityResults) {
            responseBuilder.addSlot(AvailabilitySlot.newBuilder()
                    .setAttractionName(availabilityResult.attractionName())
                    .setSlot(ParseUtils.formatTime(availabilityResult.slotTime()))
                    .setSlotCapacity(availabilityResult.slotCapacity())
                    .setBookingsConfirmed(availabilityResult.confirmedReservations())
                    .setBookingsPending(availabilityResult.pendingReservations())
                    .build());
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void reserveAttraction(BookingRequest request, StreamObserver<ReservationResponse> responseObserver) {
        String attractionName = ParseUtils.checkAttractionName(request.getAttractionName());
        int dayOfYear = ParseUtils.checkValidDayOfYear(request.getDayOfYear());
        LocalTime slotTime = ParseUtils.parseTime(request.getSlot());
        UUID visitorId = ParseUtils.parseId(request.getVisitorId());

        BookingState bookingState;
        MakeReservationResult result = attractionHandler.makeReservation(attractionName, visitorId, dayOfYear, slotTime);
        bookingState = result.isConfirmed() ? BookingState.RESERVATION_STATUS_CONFIRMED : BookingState.RESERVATION_STATUS_PENDING;

        responseObserver.onNext(ReservationResponse.newBuilder().setState(bookingState).build());
        responseObserver.onCompleted();
    }

    @Override
    public void confirmReservation(BookingRequest request, StreamObserver<Empty> responseObserver) {
        String attractionName = ParseUtils.checkAttractionName(request.getAttractionName());
        int dayOfYear = ParseUtils.checkValidDayOfYear(request.getDayOfYear());
        LocalTime slotTime = ParseUtils.parseTime(request.getSlot());
        UUID visitorId = ParseUtils.parseId(request.getVisitorId());

        attractionHandler.confirmReservation(attractionName, visitorId, dayOfYear, slotTime);
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void cancelReservation(BookingRequest request, StreamObserver<Empty> responseObserver) {
        String attractionName = ParseUtils.checkAttractionName(request.getAttractionName());
        int dayOfYear = ParseUtils.checkValidDayOfYear(request.getDayOfYear());
        LocalTime slotTime = ParseUtils.parseTime(request.getSlot());
        UUID visitorId = ParseUtils.parseId(request.getVisitorId());

        attractionHandler.cancelReservation(attractionName, visitorId, dayOfYear, slotTime);
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }
}
