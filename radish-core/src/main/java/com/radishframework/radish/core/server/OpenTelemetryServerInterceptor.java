package com.radishframework.radish.core.server;

import io.grpc.*;

import java.net.InetSocketAddress;

public class OpenTelemetryServerInterceptor implements ServerInterceptor {
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        // Extract the Span Context from the metadata of the gRPC request
        Context extractedContext = textFormat.extract(Context.current(), headers, getter);
        InetSocketAddress clientInfo =
                (InetSocketAddress) call.getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
        // Build a span based on the received context
        Span span =
                tracer
                        .spanBuilder("helloworld.Greeter/SayHello")
                        .setParent(extractedContext)
                        .setSpanKind(Span.Kind.SERVER)
                        .startSpan();
        try (Scope innerScope = span.makeCurrent()) {
            span.setAttribute("component", "grpc");
            span.setAttribute("rpc.service", "Greeter");
            span.setAttribute("net.peer.ip", clientInfo.getHostString());
            span.setAttribute("net.peer.port", clientInfo.getPort());
            // Process the gRPC call normally
            return Contexts.interceptCall(io.grpc.Context.current(), call, headers, next);
        } finally {
            span.end();
        }
    }
}