package edu.itba.serverdps.application.exceptions;

import edu.itba.serverdps.application.ApiStatus;

public class AttractionAlreadyExistsException extends ServerException {

    private static final ApiStatus API_STATUS = ApiStatus.ATTRACTION_ALREADY_EXISTS;

    public AttractionAlreadyExistsException() {
        super(API_STATUS);
    }

    public AttractionAlreadyExistsException(Throwable cause) {
        super(API_STATUS, cause);
    }

    public AttractionAlreadyExistsException(String message) {
        super(message, API_STATUS);
    }

    public AttractionAlreadyExistsException(String message, Throwable cause) {
        super(message, cause, API_STATUS);
    }
}
