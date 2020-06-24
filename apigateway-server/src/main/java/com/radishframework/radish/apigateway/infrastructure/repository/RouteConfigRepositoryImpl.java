package com.radishframework.radish.apigateway.infrastructure.repository;

import com.radishframework.radish.apigateway.domain.model.RouteConfig;
import com.radishframework.radish.apigateway.domain.model.RouteConfigRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class RouteConfigRepositoryImpl implements RouteConfigRepository {
    @Override
    public Mono<RouteConfig> getByKey(String apiKey) {
        return null;
    }
}
