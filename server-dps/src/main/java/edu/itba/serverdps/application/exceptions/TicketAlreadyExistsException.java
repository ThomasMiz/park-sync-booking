package edu.itba.serverdps.application.exceptions;

import edu.itba.serverdps.application.ApiStatus;

public class TicketAlreadyExistsException extends ServerException {

    private static final ApiStatus API_STATUS = ApiStatus.TICKET_ALREADY_EXISTS;

    public TicketAlreadyExistsException() {
        super(API_STATUS);
    }

    public TicketAlreadyExistsException(Throwable cause) {
        super(API_STATUS, cause);
    }

    public TicketAlreadyExistsException(String message) {
        super(message, API_STATUS);
    }

    public TicketAlreadyExistsException(String message, Throwable cause) {
        super(message, cause, API_STATUS);
    }
}