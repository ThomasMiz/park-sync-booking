package edu.itba.serverdps.application.exceptions;

import edu.itba.serverdps.application.ApiStatus;

public class OutOfCapacityException extends ServerException {

    private static final ApiStatus API_STATUS = ApiStatus.OUT_OF_CAPACITY;

    public OutOfCapacityException() {
        super(API_STATUS);
    }

    public OutOfCapacityException(Throwable cause) {
        super(API_STATUS, cause);
    }

    public OutOfCapacityException(String message) {
        super(message, API_STATUS);
    }

    public OutOfCapacityException(String message, Throwable cause) {
        super(message, cause, API_STATUS);
    }
}