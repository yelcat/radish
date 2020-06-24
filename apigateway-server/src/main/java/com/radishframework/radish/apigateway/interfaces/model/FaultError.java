package com.radishframework.radish.apigateway.interfaces.model;

public class FaultError {
    private int code;
    private String message;

    public FaultError(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
