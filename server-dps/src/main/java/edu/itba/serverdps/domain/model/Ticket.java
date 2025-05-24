package edu.itba.serverdps.domain.model;

import edu.itba.serverdps.application.exceptions.MissingPassException;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.LocalTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Represents a ticket, or pass, for a visitor on a given day.
 *
 * @implNote The bookTransactional() and removeBook() methods are thread-safe and work as atomic operations
 */
@Accessors(fluent = true)
public class Ticket {
    @Getter
    private final UUID visitorId;
    @Getter
    private final int dayOfYear;
    @Getter
    private final TicketType ticketType;
    private int bookings;

    public Ticket(UUID visitorId, int dayOfYear, TicketType ticketType) {
        this.visitorId = Objects.requireNonNull(visitorId);
        this.ticketType = Objects.requireNonNull(ticketType);
        this.dayOfYear = dayOfYear;
        this.bookings = 0;
    }

    public Ticket(UUID visitorId, int dayOfYear, TicketType ticketType, int bookings) {
        this(visitorId, dayOfYear, ticketType);
        this.bookings = bookings;
    }

    /**
     * Attempts to book a new reservation, incrementing the bookings counter and running a function all as an atomic
     * operation. If the transaction function returns a non-null value, it is assumed that the reservation succeeded,
     * so the bookings counter is incremented and the transaction function's result is returned. If the transaction
     * function returns null, it is assumed that the reservation failed, so the bookings counter isn't incremented and
     * null is returned.
     *
     * @param slotTime    The time slot for the reservation.
     * @param transaction The function that makes the reservation.
     * @param <T>         The return type for the transaction.
     * @return The value returned by the transaction.
     * @throws MissingPassException If the time slot isn't allowed or the user has reached their booking limit.
     */
    public synchronized <T> T bookTransactional(LocalTime slotTime, Supplier<T> transaction) {
        if (!this.ticketType.canBook(this.bookings, slotTime))
            throw new MissingPassException();
        T result = transaction.get();

        Optional.ofNullable(result).ifPresent(r -> this.bookings++); // no if :-)

        return result;
    }

    /**
     * Decrements the bookings counter for this ticket as an atomic operation.
     */
    public synchronized void removeBook() {
        if (this.bookings <= 0)
            throw new IllegalStateException("Cannot removeBook() when bookings is not greater than zero: " + this.bookings);
        this.bookings--;
    }
}
