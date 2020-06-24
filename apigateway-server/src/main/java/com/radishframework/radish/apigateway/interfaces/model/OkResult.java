package com.radishframework.radish.apigateway.interfaces.model;

public class OkResult<T> implements ApiResult {
    private String id;
    private T result;

    public OkResult(T result) {
        this.result = result;
    }

    public String getId() {
        return id;
    }

    public T getResult() {
        return result;
    }
}
