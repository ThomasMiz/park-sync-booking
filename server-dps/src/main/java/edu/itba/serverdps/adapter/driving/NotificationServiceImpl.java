package edu.itba.serverdps.adapter.driving;

import com.google.protobuf.Empty;
import edu.itba.serverdps.application.utils.ParseUtils;
import edu.itba.serverdps.domain.model.Attraction;
import edu.itba.serverdps.domain.model.ConfirmedReservation;
import edu.itba.serverdps.domain.model.Reservation;
import edu.itba.serverdps.domain.usecase.NotificationStreamObserver;
import edu.itba.serverdps.domain.usecase.handler.AttractionHandler;
import edu.itba.serverdps.domain.usecase.handler.NotificationRouterHandler;
import edu.itba.serverdps.port.driving.grpc.AttractionNotificationServiceGrpc;
import edu.itba.serverdps.port.driving.grpc.Notification;
import edu.itba.serverdps.port.driving.grpc.NotificationRequest;
import edu.itba.serverdps.port.driving.grpc.NotificationType;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.grpc.server.service.GrpcService;

import java.time.LocalTime;
import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
public class NotificationServiceImpl extends AttractionNotificationServiceGrpc.AttractionNotificationServiceImplBase {
    private final AttractionHandler attractionHandler;
    private final NotificationRouterHandler notificationRouterHandler;

    @Override
    public void follow(NotificationRequest request, StreamObserver<Notification> responseObserver) {
        String attractionName = ParseUtils.checkAttractionName(request.getRideName());
        UUID visitorId = ParseUtils.parseId(request.getVisitorId());
        int dayOfYear = ParseUtils.checkValidDayOfYear(request.getDayOfYear());

        Attraction attraction = attractionHandler.getAttraction(attractionName);
        NotificationStreamObserverImpl notificationStream = new NotificationStreamObserverImpl(responseObserver);
        notificationRouterHandler.subscribe(notificationStream, attraction, visitorId, dayOfYear);
    }

    @Override
    public void unfollow(NotificationRequest request, StreamObserver<Empty> responseObserver) {
        String attractionName = ParseUtils.checkAttractionName(request.getRideName());
        UUID visitorId = ParseUtils.parseId(request.getVisitorId());
        int dayOfYear = ParseUtils.checkValidDayOfYear(request.getDayOfYear());

        Attraction attraction = attractionHandler.getAttraction(attractionName);
        notificationRouterHandler.unsubscribe(attraction, visitorId, dayOfYear);

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    private static class NotificationStreamObserverImpl implements NotificationStreamObserver {
        private final StreamObserver<Notification> streamObserver;
        private boolean completed = false;

        public NotificationStreamObserverImpl(StreamObserver<Notification> streamObserver) {
            this.streamObserver = streamObserver;
        }

        @Override
        public synchronized void onComplete() {
            if (completed) return;

            streamObserver.onCompleted();
            completed = true;
        }

        @Override
        public synchronized void onSlotCapacitySet(Attraction attraction, int dayOfYear, int slotCapacity) {
            if (completed) return;

            Notification notification = Notification.newBuilder()
                    .setType(NotificationType.NOTIFICATION_TYPE_BOOKING_SLOT_CAPACITY_SET)
                    .setSlotCapacity(slotCapacity)
                    .build();

            streamObserver.onNext(notification);
        }

        @Override
        public synchronized void onCreated(Reservation reservation, LocalTime slotTime, boolean isConfirmed) {
            if (completed) return;

            Notification notification = Notification.newBuilder()
                    .setType(isConfirmed ?
                            NotificationType.NOTIFICATION_TYPE_BOOKING_CREATED_CONFIRMED :
                            NotificationType.NOTIFICATION_TYPE_BOOKING_CREATED_PENDING)
                    .setSlotTime(ParseUtils.formatTime(slotTime))
                    .build();

            streamObserver.onNext(notification);
        }

        @Override
        public synchronized void onConfirmed(ConfirmedReservation reservation) {
            if (completed) return;

            Notification notification = Notification.newBuilder()
                    .setType(NotificationType.NOTIFICATION_TYPE_BOOKING_CONFIRMED)
                    .setSlotTime(ParseUtils.formatTime(reservation.slotTime()))
                    .build();

            streamObserver.onNext(notification);
        }

        @Override
        public synchronized void onRelocated(Reservation reservation, LocalTime prevSlotTime, LocalTime newSlotTime) {
            if (completed) return;

            Notification notification = Notification.newBuilder()
                    .setType(NotificationType.NOTIFICATION_TYPE_BOOKING_RELOCATED)
                    .setSlotTime(ParseUtils.formatTime(prevSlotTime))
                    .setRelocatedTo(ParseUtils.formatTime(newSlotTime))
                    .build();

            streamObserver.onNext(notification);
        }

        @Override
        public synchronized void onCancelled(Reservation reservation, LocalTime slotTime) {
            if (completed) return;

            Notification notification = Notification.newBuilder()
                    .setType(NotificationType.NOTIFICATION_TYPE_BOOKING_CANCELLED)
                    .setSlotTime(ParseUtils.formatTime(slotTime))
                    .build();

            streamObserver.onNext(notification);
        }
    }
}
