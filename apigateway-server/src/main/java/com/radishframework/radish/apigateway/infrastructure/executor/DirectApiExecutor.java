package com.radishframework.radish.apigateway.infrastructure.executor;

import com.radishframework.radish.apigateway.domain.model.ApiExecutor;
import com.radishframework.radish.apigateway.domain.model.RouteConfig;
import com.radishframework.radish.apigateway.interfaces.model.ApiRequest;
import com.radishframework.radish.apigateway.interfaces.model.ApiResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class DirectApiExecutor implements ApiExecutor {
    private final RouteConfig routeConfig;

    public DirectApiExecutor(RouteConfig routeConfig) {
        this.routeConfig = routeConfig;
    }

    @Override
    public Mono<ApiResult> execute(Mono<ApiRequest> apiRequestMono) {
        return null;
    }
}
