package edu.itba.serverdps.application.exceptions;

import edu.itba.serverdps.application.ApiStatus;

public class InvalidOpeningAndClosingTimeException extends ServerException {

    private static final ApiStatus API_STATUS = ApiStatus.INVALID_OPENING_AND_CLOSING_TIME;

    public InvalidOpeningAndClosingTimeException() {
        super(API_STATUS);
    }

    public InvalidOpeningAndClosingTimeException(Throwable cause) {
        super(API_STATUS, cause);
    }

    public InvalidOpeningAndClosingTimeException(String message) {
        super(message, API_STATUS);
    }

    public InvalidOpeningAndClosingTimeException(String message, Throwable cause) {
        super(message, cause, API_STATUS);
    }
}
