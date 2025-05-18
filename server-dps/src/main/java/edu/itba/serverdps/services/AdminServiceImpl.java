package edu.itba.serverdps.services;

import edu.itba.serverdps.exceptions.InvalidDayException;
import edu.itba.serverdps.exceptions.InvalidOpeningAndClosingTimeException;
import edu.itba.serverdps.grpc.*;
import edu.itba.serverdps.handlers.AttractionHandler;
import edu.itba.serverdps.models.TicketType;
import edu.itba.serverdps.results.DefineSlotCapacityResult;
import edu.itba.serverdps.utils.ParseUtils;
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

        if (!openTime.isBefore(closeTime))
            throw new InvalidOpeningAndClosingTimeException();

        attractionHandler.createAttraction(attractionName, openTime, closeTime, slotDuration);


        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void addTicket(AddTicketRequest request, StreamObserver<Empty> responseObserver) {
        int dayOfYear = ParseUtils.checkValidDayOfYear(request.getDayOfYear());

        TicketType ticketType = TicketType.fromPassType(request.getPassType()).orElseThrow(InvalidDayException::new);
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

        DefineSlotCapacityResult result = attractionHandler.setSlotCapacityForAttraction(attractionName, dayOfYear, capacity);

        responseObserver.onNext(AddCapacityResponse.newBuilder()
                .setCancelledBookings(result.bookingsCancelled())
                .setConfirmedBookings(result.bookingsConfirmed())
                .setRelocatedBookings(result.bookingsRelocated())
                .build());
        responseObserver.onCompleted();
    }
}
