package edu.itba.serverdps.exceptions;

import edu.itba.serverdps.ApiStatus;

public class ReservationAlreadyConfirmedException extends ServerException {

    private static final ApiStatus API_STATUS = ApiStatus.ALREADY_CONFIRMED;

    public ReservationAlreadyConfirmedException() {
        super(API_STATUS);
    }

    public ReservationAlreadyConfirmedException(Throwable cause) {
        super(API_STATUS, cause);
    }

    public ReservationAlreadyConfirmedException(String message) {
        super(message, API_STATUS);
    }

    public ReservationAlreadyConfirmedException(String message, Throwable cause) {
        super(message, cause, API_STATUS);
    }
}
