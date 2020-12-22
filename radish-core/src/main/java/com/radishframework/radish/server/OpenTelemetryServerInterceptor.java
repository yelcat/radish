package com.radishframework.radish.server;

import com.radishframework.radish.common.OpenTelemetryHolder;
import io.grpc.*;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;

import java.net.InetSocketAddress;

public class OpenTelemetryServerInterceptor implements ServerInterceptor {
    private final Tracer tracer;

    private final TextMapPropagator textFormat =
            OpenTelemetryHolder.getOpenTelemetry().getPropagators().getTextMapPropagator();

    // Extract the Distributed Context from the gRPC metadata
    private final TextMapPropagator.Getter<Metadata> getter =
            new TextMapPropagator.Getter<Metadata>() {
                @Override
                public Iterable<String> keys(Metadata carrier) {
                    return carrier.keys();
                }

                @Override
                public String get(Metadata carrier, String key) {
                    Metadata.Key<String> k = Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER);
                    if (carrier != null && carrier.containsKey(k)) {
                        return carrier.get(k);
                    }
                    return "";
                }
            };

    public OpenTelemetryServerInterceptor(final String applicationName) {
        this.tracer = OpenTelemetryHolder.getOpenTelemetry().getTracer(applicationName);
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        // Extract the Span Context from the metadata of the gRPC request
        Context extractedContext = textFormat.extract(Context.current(), headers, getter);
        InetSocketAddress clientInfo =
                (InetSocketAddress) call.getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
        final String serviceName = call.getMethodDescriptor().getServiceName();
        final String spanName = serviceName + "/" + call.getMethodDescriptor().getFullMethodName();
        // Build a span based on the received context
        final Span span =
                tracer
                        .spanBuilder(spanName)
                        .setParent(extractedContext)
                        .setSpanKind(Span.Kind.SERVER)
                        .startSpan();
        try (Scope innerScope = span.makeCurrent()) {
            span.setAttribute("component", "grpc");
            span.setAttribute("rpc.service", serviceName);
            span.setAttribute("net.peer.ip", clientInfo.getHostString());
            span.setAttribute("net.peer.port", clientInfo.getPort());
            // Process the gRPC call normally
            return Contexts.interceptCall(io.grpc.Context.current(), call, headers, next);
        } finally {
            span.end();
        }
    }
}