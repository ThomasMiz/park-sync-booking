package edu.itba.serverdps.application.exceptions;

import edu.itba.serverdps.application.ApiStatus;

public class InvalidSlotException extends ServerException {

    private static final ApiStatus API_STATUS = ApiStatus.INVALID_SLOT;

    public InvalidSlotException() {
        super(API_STATUS);
    }

    public InvalidSlotException(Throwable cause) {
        super(API_STATUS, cause);
    }

    public InvalidSlotException(String message) {
        super(message, API_STATUS);
    }

    public InvalidSlotException(String message, Throwable cause) {
        super(message, cause, API_STATUS);
    }
}