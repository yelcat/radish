package com.radishframework.radish.apigateway.domain.model;

import reactor.core.publisher.Mono;

public interface RouteConfigRepository {
    Mono<RouteConfig> getByKey(String apiKey);
}
