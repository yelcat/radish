package com.radishframework.radish.apigateway.interfaces.model;


public class ErrResult implements ApiResult {
    private int id;
    private Error error;

    public int getId() {
        return id;
    }

    public Error getError() {
        return error;
    }


}
