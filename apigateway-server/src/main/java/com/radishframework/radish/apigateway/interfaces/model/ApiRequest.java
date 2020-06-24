package com.radishframework.radish.apigateway.interfaces.model;

import java.util.Map;

public class ApiRequest {
    private String id;
    private String method;
    private Map<String, Object> params;

    public String getId() {
        return id;
    }

    public String getMethod() {
        return method;
    }

    public Map<String, Object> getParams() {
        return params;
    }
}
