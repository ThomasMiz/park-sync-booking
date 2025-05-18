package edu.itba.serverdps.models;

import edu.itba.serverdps.grpc.PassType;

import java.time.LocalTime;
import java.util.Optional;

public enum TicketType {
    FULL_DAY {
        @Override
        public boolean isSlotTimeValid(LocalTime slotTime) {
            return true;
        }

        @Override
        public boolean isBookingsCountValid(int bookings) {
            return bookings < 3;
        }
    },
    HALF_DAY {
        @Override
        public boolean isSlotTimeValid(LocalTime slotTime) {
            return slotTime.isAfter(LocalTime.of(14, 0));
        }

        @Override
        public boolean isBookingsCountValid(int bookings) {
            return true;
        }
    },
    UNLIMITED {
        @Override
        public boolean isSlotTimeValid(LocalTime slotTime) {
            return true;
        }

        @Override
        public boolean isBookingsCountValid(int bookings) {
            return true;
        }
    };

    public static Optional<TicketType> fromPassType(PassType passType) {
        return switch (passType) {
            case PASS_TYPE_FULL_DAY -> Optional.of(TicketType.FULL_DAY);
            case PASS_TYPE_HALF_DAY -> Optional.of(TicketType.HALF_DAY);
            case PASS_TYPE_UNLIMITED -> Optional.of(TicketType.UNLIMITED);
            default -> Optional.empty();
        };
    }

    public static Optional<PassType> toPassType(TicketType ticket) {
        return switch (ticket) {
            case FULL_DAY -> Optional.of(PassType.PASS_TYPE_FULL_DAY);
            case HALF_DAY -> Optional.of(PassType.PASS_TYPE_HALF_DAY);
            case UNLIMITED -> Optional.of(PassType.PASS_TYPE_UNLIMITED);
            default -> Optional.empty();
        };
    }

    public abstract boolean isSlotTimeValid(LocalTime slotTime);

    public abstract boolean isBookingsCountValid(int bookings);

    public boolean canBook(int bookings, LocalTime slotTime) {
        return isBookingsCountValid(bookings) && isSlotTimeValid(slotTime);
    }
}
