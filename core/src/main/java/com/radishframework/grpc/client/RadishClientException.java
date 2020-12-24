package com.radishframework.grpc.client;

public class RadishClientException extends RuntimeException {

    public RadishClientException(Exception ex) {
        super(ex);
    }
}
