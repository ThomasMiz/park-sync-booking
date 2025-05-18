package edu.itba.serverdps.application.exceptions;

import edu.itba.serverdps.application.ApiStatus;

public class MissingPassException extends ServerException {

    private static final ApiStatus API_STATUS = ApiStatus.MISSING_PASS;

    public MissingPassException() {
        super(API_STATUS);
    }

    public MissingPassException(Throwable cause) {
        super(API_STATUS, cause);
    }

    public MissingPassException(String message) {
        super(message, API_STATUS);
    }

    public MissingPassException(String message, Throwable cause) {
        super(message, cause, API_STATUS);
    }
}