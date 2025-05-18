package edu.itba.serverdps.services;

import edu.itba.serverdps.domain.model.Ticket;
import edu.itba.serverdps.application.utils.Constants;
import lombok.experimental.UtilityClass;

import java.time.LocalTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@UtilityClass
public final class TestUtils {

    public static ConcurrentMap<UUID, Ticket>[] generateTicketsByDayMaps() {
        ConcurrentMap<UUID, Ticket>[] maps = (ConcurrentMap<UUID, Ticket>[]) new ConcurrentMap[Constants.DAYS_IN_YEAR];
        for (int i = 0; i < maps.length; i++)
            maps[i] = new ConcurrentHashMap<>();
        return maps;
    }

    public static LocalTime[] generateTimeSlotsFor(LocalTime openingTime, LocalTime closingTime, int slotDuration) {
        int slotCount = ((closingTime.toSecondOfDay() - openingTime.toSecondOfDay()) / 60 + slotDuration - 1) / slotDuration;
        LocalTime[] result = new LocalTime[slotCount];
        for (int i = 0; i < slotCount; i++) {
            result[i] = LocalTime.ofSecondOfDay(openingTime.toSecondOfDay() + (long) i * slotDuration * 60);
        }

        return result;
    }
}
