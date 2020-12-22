package com.radishframework.radish.client;

import com.radishframework.radish.common.OpenTelemetryHolder;
import io.grpc.*;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;

public class OpenTelemetryClientInterceptor implements ClientInterceptor {
    // Share context via text headers
    private final TextMapPropagator textFormat =
            OpenTelemetryHolder.getOpenTelemetry().getPropagators().getTextMapPropagator();
    // Inject context into the gRPC request metadata
    private final TextMapPropagator.Setter<Metadata> setter =
            (carrier, key, value) ->
                    carrier.put(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER), value);

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> methodDescriptor, CallOptions callOptions, Channel channel) {
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
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
