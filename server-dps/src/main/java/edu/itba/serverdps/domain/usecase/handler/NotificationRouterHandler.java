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
        ConcurrentMap<Attraction, ConcurrentMap<UUID, NotificationStreamObserver>> attractionMap = streamsByDay[dayOfYear];
        ConcurrentMap<UUID, NotificationStreamObserver> idMap = attractionMap.get(attraction);

        if (idMap != null)
            idMap.forEach((vid, notif) -> notif.onSlotCapacitySet(attraction, dayOfYear, slotCapacity));
    }

    @Override
    public void onCreated(Reservation reservation, LocalTime slotTime, boolean isConfirmed) {
        int dayOfYear = reservation.getDayOfYear();
        Attraction attraction = reservation.getAttraction();

        ConcurrentMap<Attraction, ConcurrentMap<UUID, NotificationStreamObserver>> attractionMap = streamsByDay[dayOfYear];
        ConcurrentMap<UUID, NotificationStreamObserver> idMap = attractionMap.get(attraction);
        NotificationStreamObserver stream;

        if (idMap != null && (stream = idMap.get(reservation.getVisitorId())) != null) {
            stream.onCreated(reservation, slotTime, isConfirmed);
            stream.onComplete();
            idMap.remove(reservation.getVisitorId());
        }
    }

    @Override
    public void onConfirmed(ConfirmedReservation reservation) {
        int dayOfYear = reservation.getDayOfYear();
        Attraction attraction = reservation.getAttraction();

        ConcurrentMap<Attraction, ConcurrentMap<UUID, NotificationStreamObserver>> attractionMap = streamsByDay[dayOfYear];
        ConcurrentMap<UUID, NotificationStreamObserver> idMap = attractionMap.get(attraction);
        NotificationStreamObserver stream;

        if (idMap != null && (stream = idMap.get(reservation.getVisitorId())) != null) {
            stream.onConfirmed(reservation);
            stream.onComplete();
            idMap.remove(reservation.getVisitorId());
        }
    }

    @Override
    public void onRelocated(Reservation reservation, LocalTime prevSlotTime, LocalTime newSlotTime) {
        int dayOfYear = reservation.getDayOfYear();
        Attraction attraction = reservation.getAttraction();

        ConcurrentMap<Attraction, ConcurrentMap<UUID, NotificationStreamObserver>> attractionMap = streamsByDay[dayOfYear];
        ConcurrentMap<UUID, NotificationStreamObserver> idMap = attractionMap.get(attraction);
        NotificationStreamObserver stream;

        if (idMap != null && (stream = idMap.get(reservation.getVisitorId())) != null)
            stream.onRelocated(reservation, prevSlotTime, newSlotTime);
    }

    @Override
    public void onCancelled(Reservation reservation, LocalTime slotTime) {
        int dayOfYear = reservation.getDayOfYear();
        Attraction attraction = reservation.getAttraction();

        ConcurrentMap<Attraction, ConcurrentMap<UUID, NotificationStreamObserver>> attractionMap = streamsByDay[dayOfYear];
        ConcurrentMap<UUID, NotificationStreamObserver> idMap = attractionMap.get(attraction);
        NotificationStreamObserver stream;

        if (idMap != null && (stream = idMap.get(reservation.getVisitorId())) != null) {
            stream.onCancelled(reservation, slotTime);
            stream.onComplete();
            idMap.remove(reservation.getVisitorId());
        }
    }

    public void subscribe(NotificationStreamObserver observer, Attraction attraction, UUID visitorId, int dayOfYear) {
        ConcurrentMap<Attraction, ConcurrentMap<UUID, NotificationStreamObserver>> attractionMap = streamsByDay[dayOfYear];
        ConcurrentMap<UUID, NotificationStreamObserver> idMap = attractionMap.computeIfAbsent(attraction, k -> new ConcurrentHashMap<>());
        boolean success = idMap.putIfAbsent(visitorId, observer) == null;

        if (!success)
            throw new AlreadyRegisteredForNotificationsException();
    }

    public void unsubscribe(Attraction attraction, UUID visitorId, int dayOfYear) {
        ConcurrentMap<Attraction, ConcurrentMap<UUID, NotificationStreamObserver>> attractionMap = streamsByDay[dayOfYear];
        ConcurrentMap<UUID, NotificationStreamObserver> idMap;
        NotificationStreamObserver stream;
        if (attractionMap == null || (idMap = attractionMap.get(attraction)) == null || (stream = idMap.remove(visitorId)) == null)
            throw new NotRegisteredForNotificationsException();

        stream.onComplete();
    }
}
