package edu.itba.serverdps.exceptions;

import edu.itba.serverdps.ApiStatus;
import edu.itba.serverdps.exceptions.ServerException;

public class CapacityAlreadyDefinedException extends ServerException {

    private static final ApiStatus API_STATUS = ApiStatus.CAPACITY_ALREADY_DEFINED;

    public CapacityAlreadyDefinedException() {
        super(API_STATUS);
    }

    public CapacityAlreadyDefinedException(Throwable cause) {
        super(API_STATUS, cause);
    }

    public CapacityAlreadyDefinedException(String message) {
        super(message, API_STATUS);
    }

    public CapacityAlreadyDefinedException(String message, Throwable cause) {
        super(message, cause, API_STATUS);
    }
}