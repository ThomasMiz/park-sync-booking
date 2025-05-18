package edu.itba.serverdps.exceptions;

import edu.itba.serverdps.ApiStatus;
import edu.itba.serverdps.exceptions.ServerException;

public class InvalidDayException extends ServerException {

    private static final ApiStatus API_STATUS = ApiStatus.INVALID_DAY;

    public InvalidDayException() {
        super(API_STATUS);
    }

    public InvalidDayException(Throwable cause) {
        super(API_STATUS, cause);
    }

    public InvalidDayException(String message) {
        super(message, API_STATUS);
    }

    public InvalidDayException(String message, Throwable cause) {
        super(message, cause, API_STATUS);
    }
}
