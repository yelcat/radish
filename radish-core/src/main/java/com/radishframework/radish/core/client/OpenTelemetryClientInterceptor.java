package com.radishframework.radish.core.client;

import io.grpc.*;

public class OpenTelemetryClientInterceptor implements ClientInterceptor {

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> methodDescriptor, CallOptions callOptions, Channel channel) {
        return new ForwardingClientCall.SimpleForwardingClientCall<>(
                channel.newCall(methodDescriptor, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                // Inject the request with the current context
                textFormat.inject(Context.current(), headers, setter);
                // Perform the gRPC request
                super.start(responseListener, headers);
            }
        };
    }
}
