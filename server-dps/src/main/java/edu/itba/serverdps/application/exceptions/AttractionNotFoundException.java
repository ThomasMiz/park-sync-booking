package edu.itba.serverdps.application.exceptions;

import edu.itba.serverdps.application.ApiStatus;

public class AttractionNotFoundException extends ServerException {

    private static final ApiStatus API_STATUS = ApiStatus.ATTRACTION_NOT_FOUND;

    public AttractionNotFoundException() {
        super(API_STATUS);
    }

    public AttractionNotFoundException(Throwable cause) {
        super(API_STATUS, cause);
    }

    public AttractionNotFoundException(String message) {
        super(message, API_STATUS);
    }

    public AttractionNotFoundException(String message, Throwable cause) {
        super(message, cause, API_STATUS);
    }
}
