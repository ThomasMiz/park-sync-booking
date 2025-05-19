package edu.itba.serverdps.adapter.driving;

import edu.itba.serverdps.application.exceptions.InvalidDayException;
import edu.itba.serverdps.application.exceptions.InvalidOpeningAndClosingTimeException;
import edu.itba.serverdps.port.driving.grpc.*;
import edu.itba.serverdps.domain.usecase.handler.AttractionHandler;
import edu.itba.serverdps.domain.model.TicketType;
import edu.itba.serverdps.domain.model.result.DefineSlotCapacityResult;
import edu.itba.serverdps.application.utils.ParseUtils;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;

import java.time.LocalTime;
import java.util.UUID;

@GrpcService
public class AdminServiceImpl extends AdminServiceGrpc.AdminServiceImplBase {

    private final AttractionHandler attractionHandler;

    public AdminServiceImpl(AttractionHandler attractionHandler) {
        this.attractionHandler = attractionHandler;
    }

    @Override
    public void addAttraction(AddAttractionRequest request, StreamObserver<Empty> responseObserver) {
        String attractionName = ParseUtils.checkAttractionName(request.getName());
        LocalTime openTime = ParseUtils.parseTime(request.getOpeningTime());
        LocalTime closeTime = ParseUtils.parseTime(request.getClosingTime());
        int slotDuration = ParseUtils.checkValidDuration(request.getSlotDurationMinutes());

        validateOpeningAndClosingTimes(openTime, closeTime);
        attractionHandler.createAttraction(attractionName, openTime, closeTime, slotDuration);

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void addTicket(AddTicketRequest request, StreamObserver<Empty> responseObserver) {
        int dayOfYear = ParseUtils.checkValidDayOfYear(request.getDayOfYear());
        TicketType ticketType = TicketType.fromPassType(request.getPassType())
                .orElseThrow(InvalidDayException::new);
        UUID visitorId = ParseUtils.parseId(request.getVisitorId());

        attractionHandler.addTicket(visitorId, dayOfYear, ticketType);
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void addCapacity(AddCapacityRequest request, StreamObserver<AddCapacityResponse> responseObserver) {
        String attractionName = ParseUtils.checkAttractionName(request.getAttractionName());
        int dayOfYear = ParseUtils.checkValidDayOfYear(request.getDayOfYear());
        int capacity = ParseUtils.checkValidCapacity(request.getCapacity());

        DefineSlotCapacityResult result = attractionHandler.setSlotCapacityForAttraction(
                attractionName, dayOfYear, capacity);

        responseObserver.onNext(AddCapacityResponse.newBuilder()
                .setCancelledBookings(result.bookingsCancelled())
                .setConfirmedBookings(result.bookingsConfirmed())
                .setRelocatedBookings(result.bookingsRelocated())
                .build());
        responseObserver.onCompleted();
    }

    private void validateOpeningAndClosingTimes(LocalTime openTime, LocalTime closeTime) {
        if (!openTime.isBefore(closeTime)) {
            throw new InvalidOpeningAndClosingTimeException();
        }
    }
}
