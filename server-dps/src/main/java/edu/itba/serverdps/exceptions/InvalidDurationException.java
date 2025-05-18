package edu.itba.serverdps.exceptions;

import edu.itba.serverdps.ApiStatus;
import edu.itba.serverdps.exceptions.ServerException;

public class InvalidDurationException extends ServerException {

    private static final ApiStatus API_STATUS = ApiStatus.INVALID_DURATION;

    public InvalidDurationException() {
        super(API_STATUS);
    }

    public InvalidDurationException(Throwable cause) {
        super(API_STATUS, cause);
    }

    public InvalidDurationException(String message) {
        super(message, API_STATUS);
    }

    public InvalidDurationException(String message, Throwable cause) {
        super(message, cause, API_STATUS);
    }
}