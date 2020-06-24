package com.radishframework.radish.apigateway.server;

import com.radishframework.radish.apigateway.interfaces.handler.ApiRequestHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class GatewayConfiguration {
    @Bean
    RouterFunction<ServerResponse> route(ApiRequestHandler apiRequestHandler) {
        return RouterFunctions
                .route(RequestPredicates.POST("/apis/*")
                                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON)),
                        apiRequestHandler::handleApiRequest);
    }
}
