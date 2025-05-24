package edu.itba.serverdps.domain.usecase.handler;

import edu.itba.serverdps.application.exceptions.AlreadyRegisteredForNotificationsException;
import edu.itba.serverdps.application.exceptions.NotRegisteredForNotificationsException;
import edu.itba.serverdps.domain.model.Attraction;
import edu.itba.serverdps.domain.model.ConfirmedReservation;
import edu.itba.serverdps.domain.model.Reservation;
import edu.itba.serverdps.domain.usecase.NotificationStreamObserver;
import edu.itba.serverdps.domain.usecase.ReservationObserver;
import edu.itba.serverdps.application.utils.Constants;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.Optional;

/**
 * An implementation of ReservationObserver that routes notifications to different NotificationStreamObserver instances
 * depending on the notification's attraction, visitor id, and day of year.
 */
@Component
public class NotificationRouterHandler implements ReservationObserver {
    private final ConcurrentMap<Attraction, ConcurrentMap<UUID, NotificationStreamObserver>>[] streamsByDay;

    public NotificationRouterHandler() {
        streamsByDay = new ConcurrentMap[Constants.DAYS_IN_YEAR];
        for (int i = 0; i < streamsByDay.length; i++)
            streamsByDay[i] = new ConcurrentHashMap<>();
    }

    @Override
    public void onSlotCapacitySet(Attraction attraction, int dayOfYear, int slotCapacity) {
        Optional.ofNullable(streamsByDay[dayOfYear])
                .map(attractionMap -> attractionMap.get(attraction))
                .ifPresent(idMap -> idMap.forEach((vid, notif) -> 
                    notif.onSlotCapacitySet(attraction, dayOfYear, slotCapacity)));
    }

    @Override
    public void onCreated(Reservation reservation, LocalTime slotTime, boolean isConfirmed) {
        notifyObserver(reservation.dayOfYear(), reservation.attraction(), reservation.visitorId(),
                stream -> {
                    stream.onCreated(reservation, slotTime, isConfirmed);
                    stream.onComplete();
                });
    }

    @Override
    public void onConfirmed(ConfirmedReservation reservation) {
        notifyObserver(reservation.dayOfYear(), reservation.attraction(), reservation.visitorId(),
                stream -> {
                    stream.onConfirmed(reservation);
                    stream.onComplete();
                });
    }

    @Override
    public void onRelocated(Reservation reservation, LocalTime prevSlotTime, LocalTime newSlotTime) {
        notifyObserver(reservation.dayOfYear(), reservation.attraction(), reservation.visitorId(),
                stream -> stream.onRelocated(reservation, prevSlotTime, newSlotTime));
    }

    @Override
    public void onCancelled(Reservation reservation, LocalTime slotTime) {
        notifyObserver(reservation.dayOfYear(), reservation.attraction(), reservation.visitorId(),
                stream -> {
                    stream.onCancelled(reservation, slotTime);
                    stream.onComplete();
                });
    }

    private void notifyObserver(int dayOfYear, Attraction attraction, UUID visitorId, 
            java.util.function.Consumer<NotificationStreamObserver> action) {
        Optional.ofNullable(streamsByDay[dayOfYear])
                .map(attractionMap -> attractionMap.get(attraction))
                .map(idMap -> idMap.remove(visitorId))
                .ifPresent(action);
    }

    public void subscribe(NotificationStreamObserver observer, Attraction attraction, UUID visitorId, int dayOfYear) {
        ConcurrentMap<Attraction, ConcurrentMap<UUID, NotificationStreamObserver>> attractionMap = streamsByDay[dayOfYear];
        ConcurrentMap<UUID, NotificationStreamObserver> idMap = attractionMap.computeIfAbsent(attraction, k -> new ConcurrentHashMap<>());
        if (idMap.putIfAbsent(visitorId, observer) != null) {
            throw new AlreadyRegisteredForNotificationsException();
        }
    }

    public void unsubscribe(Attraction attraction, UUID visitorId, int dayOfYear) {
        Optional.ofNullable(streamsByDay[dayOfYear])
                .map(attractionMap -> attractionMap.get(attraction))
                .map(idMap -> idMap.remove(visitorId))
                .ifPresentOrElse(
                    stream -> stream.onComplete(),
                    () -> { throw new NotRegisteredForNotificationsException(); }
                );
    }
}
