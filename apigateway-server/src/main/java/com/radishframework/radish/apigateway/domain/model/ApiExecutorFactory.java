package com.radishframework.radish.apigateway.domain.model;

import com.radishframework.radish.apigateway.infrastructure.executor.DirectApiExecutor;
import reactor.core.publisher.Mono;

public class ApiExecutorFactory {

    public static ApiExecutor create(RouteConfig routeConfig) {
        return new DirectApiExecutor(routeConfig);
    }
}
