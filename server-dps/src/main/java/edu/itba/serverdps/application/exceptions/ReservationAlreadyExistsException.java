package edu.itba.serverdps.application.exceptions;

import edu.itba.serverdps.application.ApiStatus;

public class ReservationAlreadyExistsException extends ServerException {

    private static final ApiStatus API_STATUS = ApiStatus.RESERVATION_ALREADY_EXISTS;

    public ReservationAlreadyExistsException() {
        super(API_STATUS);
    }

    public ReservationAlreadyExistsException(Throwable cause) {
        super(API_STATUS, cause);
    }

    public ReservationAlreadyExistsException(String message) {
        super(message, API_STATUS);
    }

    public ReservationAlreadyExistsException(String message, Throwable cause) {
        super(message, cause, API_STATUS);
    }
}