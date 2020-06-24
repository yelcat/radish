package com.radishframework.radish.apigateway.interfaces.handler;

import com.google.common.annotations.VisibleForTesting;
import com.radishframework.radish.apigateway.domain.model.ApiExecutorFactory;
import com.radishframework.radish.apigateway.domain.model.RouteConfig;
import com.radishframework.radish.apigateway.domain.model.RouteConfigRepository;
import com.radishframework.radish.apigateway.interfaces.model.ApiRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class ApiRequestHandler {

    @Autowired
    @VisibleForTesting
    RouteConfigRepository routeConfigRepository;

    public Mono<ServerResponse> handleApiRequest(ServerRequest request) {
        final String apiKey = request.pathContainer().subPath(1).value();
        final Mono<ApiRequest> apiRequestMono = request.bodyToMono(ApiRequest.class);
        final Mono<RouteConfig> routeConfigMono = routeConfigRepository.getByKey(apiKey);

        return routeConfigMono
                .map(ApiExecutorFactory::create)
                .flatMap(apiExecutor -> apiExecutor.execute(apiRequestMono))
                .flatMap(apiResult -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(apiResult)));
    }
}
