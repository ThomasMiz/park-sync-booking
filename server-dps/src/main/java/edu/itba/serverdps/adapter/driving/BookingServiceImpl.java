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
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@GrpcService
public class BookingServiceImpl extends BookingServiceGrpc.BookingServiceImplBase {

    private final AttractionHandler attractionHandler;

    public BookingServiceImpl(AttractionHandler attractionHandler) {
        this.attractionHandler = attractionHandler;
    }

    @Override
    public void getAttractions(Empty request, StreamObserver<GetAttractionsResponse> responseObserver) {
        GetAttractionsResponse response = attractionHandler.getAttractions().stream()
                .map(this::buildAttractionResponse)
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        attractions -> GetAttractionsResponse.newBuilder()
                                .addAllAttraction(attractions)
                                .build()
                ));

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void checkAttractionAvailability(AvailabilityRequest request, StreamObserver<AvailabilityResponse> responseObserver) {
        int dayOfYear = ParseUtils.checkValidDayOfYear(request.getDayOfYear());
        LocalTime slotFrom = ParseUtils.parseTime(request.getSlotFrom());
        final var attractionName = ParseUtils.parseAttractionName(request.getAttractionName());
        final var slotTo = ParseUtils.parseTimeOptional(request.getSlotTo());

        validateAvailabilityRequest(attractionName, slotTo, slotFrom);
        
        Collection<AttractionAvailabilityResult> availabilityResults = getAvailabilityResults(
                attractionName, dayOfYear, slotFrom, slotTo);

        AvailabilityResponse response = buildAvailabilityResponse(availabilityResults);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void reserveAttraction(BookingRequest request, StreamObserver<ReservationResponse> responseObserver) {
        String attractionName = ParseUtils.checkAttractionName(request.getAttractionName());
        int dayOfYear = ParseUtils.checkValidDayOfYear(request.getDayOfYear());
        LocalTime slotTime = ParseUtils.parseTime(request.getSlot());
        UUID visitorId = ParseUtils.parseId(request.getVisitorId());

        MakeReservationResult result = attractionHandler.makeReservation(
                attractionName, visitorId, dayOfYear, slotTime);
        
        BookingState bookingState = mapToBookingState(result.isConfirmed());
        ReservationResponse response = ReservationResponse.newBuilder()
                .setState(bookingState)
                .build();

        responseObserver.onNext(response);
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

    private edu.itba.serverdps.port.driving.grpc.Attraction buildAttractionResponse(Attraction attraction) {
        return edu.itba.serverdps.port.driving.grpc.Attraction.newBuilder()
                .setName(attraction.getName())
                .setClosingTime(ParseUtils.formatTime(attraction.getClosingTime()))
                .setOpeningTime(ParseUtils.formatTime(attraction.getOpeningTime()))
                .build();
    }

    private void validateAvailabilityRequest(Optional<String> attractionName, Optional<LocalTime> slotTo, LocalTime slotFrom) {
        if (attractionName.isEmpty() && slotTo.isEmpty()) {
            throw new CheckAvailabilityInvalidArgumentException();
        }
        slotTo.ifPresent(to -> {
            if (slotFrom.isAfter(to)) {
                throw new InvalidSlotException();
            }
        });
    }

    private Collection<AttractionAvailabilityResult> getAvailabilityResults(
            Optional<String> attractionName, int dayOfYear, LocalTime slotFrom, Optional<LocalTime> slotTo) {
        return attractionName.map(name -> 
                attractionHandler.getAvailabilityForAttraction(name, dayOfYear, slotFrom, slotTo.orElse(null)))
                .orElseGet(() -> attractionHandler.getAvailabilityForAllAttractions(dayOfYear, slotFrom, slotTo.orElse(null)));
    }

    private AvailabilityResponse buildAvailabilityResponse(Collection<AttractionAvailabilityResult> results) {
        return results.stream()
                .map(this::buildAvailabilitySlot)
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        slots -> AvailabilityResponse.newBuilder()
                                .addAllSlot(slots)
                                .build()
                ));
    }

    private AvailabilitySlot buildAvailabilitySlot(AttractionAvailabilityResult result) {
        return AvailabilitySlot.newBuilder()
                .setAttractionName(result.attractionName())
                .setSlot(ParseUtils.formatTime(result.slotTime()))
                .setSlotCapacity(result.slotCapacity())
                .setBookingsConfirmed(result.confirmedReservations())
                .setBookingsPending(result.pendingReservations())
                .build();
    }

    private BookingState mapToBookingState(boolean isConfirmed) {
        return isConfirmed ? 
                BookingState.RESERVATION_STATUS_CONFIRMED : 
                BookingState.RESERVATION_STATUS_PENDING;
    }
}
