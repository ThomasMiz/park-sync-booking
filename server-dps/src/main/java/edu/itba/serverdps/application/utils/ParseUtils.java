package edu.itba.serverdps.application.utils;

import edu.itba.serverdps.application.exceptions.*;
import lombok.experimental.UtilityClass;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@UtilityClass
public final class ParseUtils {

    private static final DateTimeFormatter slotTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Parses a "HH:mm" string into a LocalTime.
     *
     * @throws InvalidSlotException if the string is invalid.
     */
    public static LocalTime parseTime(String time) {
        try {
            return LocalTime.parse(time, slotTimeFormatter);
        } catch (DateTimeParseException e) {
            throw new InvalidSlotException();
        }
    }

    /**
     * Parses a "HH:mm" string into a LocalTime, or null if the given time is null or blank.
     *
     * @throws InvalidSlotException if the string is invalid.
     */
    public static LocalTime parseTimeOrNull(String time) {
        try {
            return time == null || time.isBlank() ? null : LocalTime.parse(time, slotTimeFormatter);
        } catch (DateTimeParseException e) {
            throw new InvalidSlotException();
        }
    }

    public static String formatTime(LocalTime time) {
        return slotTimeFormatter.format(time);
    }

    /**
     * Parses a UUID from a string.
     *
     * @throws InvalidVisitorIdException if the string is invalid.
     */
    public static UUID parseId(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new InvalidVisitorIdException();
        }
    }

    /**
     * Verifies that the given int is a valid day of the year.
     *
     * @return The same value passed.
     * @throws InvalidDayException if not a valid day of year.
     */
    public static int checkValidDayOfYear(int dayOfYear) {
        if (dayOfYear < 1 || dayOfYear > 365)
            throw new InvalidDayException();
        return dayOfYear;
    }

    /**
     * Verifies that the given int is a valid duration.
     *
     * @return The same value passed.
     * @throws InvalidDurationException if not a valid duration.
     */
    public static int checkValidDuration(int duration) {
        if (duration <= 0)
            throw new InvalidDurationException();
        return duration;
    }

    /**
     * Verifies that the given int is a valid capacity.
     *
     * @return The same value passed.
     * @throws NegativeCapacityException if not a valid capacity.
     */
    public static int checkValidCapacity(int capacity) {
        if (capacity < 0)
            throw new NegativeCapacityException();
        return capacity;
    }

    /**
     * Verifies that the given string is valid for an attraction name. Does not check that said attraction exists.
     *
     * @return The same value passed.
     * @throws EmptyAttractionException if the attraction name is empty or blank.
     */
    public static String checkAttractionName(String attractionName) {
        if (attractionName == null || attractionName.isBlank())
            throw new EmptyAttractionException();
        return attractionName;
    }

    /**
     * Verifies that the given string is valid for an attraction name. Does not check that said attraction exists.
     *
     * @return The same value passed, or null if attractionName is blank.
     */
    public static String checkAttractionNameOrNull(String attractionName) {
        return (attractionName == null || attractionName.isBlank()) ? null : attractionName;
    }
}
