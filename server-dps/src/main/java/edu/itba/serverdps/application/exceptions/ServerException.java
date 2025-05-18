package edu.itba.serverdps.application.exceptions;

import edu.itba.serverdps.application.ApiStatus;

public class ServerException extends RuntimeException {
    private final ApiStatus apiStatus;

    public ServerException(ApiStatus apiStatus) {
        this.apiStatus = apiStatus;
    }

    public ServerException(ApiStatus apiStatus, Throwable cause) {
        super(cause);
        this.apiStatus = apiStatus;
    }

    public ServerException(String message, ApiStatus apiStatus) {
        super(message);
        this.apiStatus = apiStatus;
    }

    public ServerException(String message, Throwable cause, ApiStatus apiStatus) {
        super(message, cause);
        this.apiStatus = apiStatus;
    }

    public io.grpc.Status getStatus() {
        return apiStatus.getStatus().withDescription(apiStatus.getMessageCode());
    }
}
