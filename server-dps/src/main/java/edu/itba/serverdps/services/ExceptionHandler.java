package edu.itba.serverdps.services;

import edu.itba.serverdps.exceptions.ServerException;
import edu.itba.serverdps.exceptions.UnknownException;
import io.grpc.*;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
public class ExceptionHandler implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall, Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {
        ServerCall.Listener<ReqT> listener = serverCallHandler.startCall(serverCall, metadata);
        return new ExceptionHandlingServerCallListener<>(listener, serverCall, metadata);
    }

    private static class ExceptionHandlingServerCallListener<ReqT, RespT> extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {
        private final ServerCall<ReqT, RespT> serverCall;
        private final Metadata metadata;

        ExceptionHandlingServerCallListener(ServerCall.Listener<ReqT> listener, ServerCall<ReqT, RespT> serverCall, Metadata metadata) {
            super(listener);
            this.serverCall = serverCall;
            this.metadata = metadata;
        }

        @Override
        public void onHalfClose() {
            try {
                super.onHalfClose();
            } catch (ServerException ex) {
                handleException(ex, serverCall, metadata);
            } catch (RuntimeException ex) {
                handleException(new UnknownException(ex), serverCall, metadata);
            }
        }

        @Override
        public void onReady() {
            try {
                super.onReady();
            } catch (ServerException ex) {
                handleException(ex, serverCall, metadata);
            } catch (RuntimeException ex) {
                handleException(new UnknownException(ex), serverCall, metadata);
            }
        }

        private void handleException(ServerException exception, ServerCall<ReqT, RespT> serverCall, Metadata metadata) {
            serverCall.close(exception.getStatus(), metadata);
        }
    }
}
