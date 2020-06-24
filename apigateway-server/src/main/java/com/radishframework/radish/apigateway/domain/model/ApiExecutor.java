package com.radishframework.radish.apigateway.domain.model;

import com.radishframework.radish.apigateway.interfaces.model.ApiRequest;
import com.radishframework.radish.apigateway.interfaces.model.ApiResult;
import reactor.core.publisher.Mono;

public interface ApiExecutor {
    Mono<ApiResult> execute(Mono<ApiRequest> apiRequestMono);
}
