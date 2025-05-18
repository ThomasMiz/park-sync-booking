package edu.itba.serverdps.application.exceptions;

import edu.itba.serverdps.application.ApiStatus;

public class NegativeCapacityException extends ServerException {

    private static final ApiStatus API_STATUS = ApiStatus.NEGATIVE_CAPACITY;

    public NegativeCapacityException() {
        super(API_STATUS);
    }

    public NegativeCapacityException(Throwable cause) {
        super(API_STATUS, cause);
    }

    public NegativeCapacityException(String message) {
        super(message, API_STATUS);
    }

    public NegativeCapacityException(String message, Throwable cause) {
        super(message, cause, API_STATUS);
    }
}