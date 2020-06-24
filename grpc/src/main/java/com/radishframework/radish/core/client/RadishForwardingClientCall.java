package com.radishframework.radish.core.client;

import com.google.common.collect.ImmutableMap;
import com.radishframework.radish.core.common.RadishGrpcFields;
import com.radishframework.radish.core.common.RadishGrpcTags;
import io.grpc.*;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.contrib.grpc.TracingClientInterceptor;
import io.opentracing.log.Fields;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class RadishForwardingClientCall<ReqT, RespT> extends ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT> {

    private AtomicBoolean finished = new AtomicBoolean(false);
    private final Span span;
    private final Tracer tracer;
    private final boolean streaming;
    private final boolean verbose;
    private final Set<RadishTracingClientInterceptor.ClientRequestAttribute> tracedAttributes;

    public RadishForwardingClientCall(ClientCall<ReqT, RespT> delegate, Span span, Tracer tracer, boolean streaming,
                                       boolean verbose, Set<RadishTracingClientInterceptor.ClientRequestAttribute> tracedAttributes) {
        super(delegate);
        this.span = span;
        this.tracer = tracer;
        this.streaming = streaming;
        this.verbose = verbose;
        this.tracedAttributes = tracedAttributes;
    }

    @Override
    public void start(Listener responseListener, Metadata headers) {
        if (verbose) {
            span.log(ImmutableMap.<String, Object>builder()
                    .put(Fields.EVENT, RadishGrpcFields.CLIENT_CALL_START)
                    .put(Fields.MESSAGE, "Client call started")
                    .build());
        }
        if (tracedAttributes.contains(TracingClientInterceptor.ClientRequestAttribute.HEADERS)) {
            RadishGrpcTags.GRPC_HEADERS.set(span, headers);
        }
        tracer.inject(
                span.context(),
                Format.Builtin.HTTP_HEADERS,
                new TextMap() {
                    @Override
                    public void put(String key, String value) {
                        Metadata.Key<String> headerKey =
                                Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER);
                        headers.put(headerKey, value);
                    }

                    @Override
                    public Iterator<Map.Entry<String, String>> iterator() {
                        throw new UnsupportedOperationException(
                                "TextMapInjectAdapter should only be used with Tracer.inject()");
                    }
                });

        //响应监听
        Listener<RespT> tracingResponseListener =
                new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {

                    @Override
                    public void onHeaders(Metadata headers) {
                        if (verbose) {
                            span.log(
                                    ImmutableMap.<String, Object>builder()
                                            .put(Fields.EVENT, RadishGrpcFields.CLIENT_CALL_LISTENER_ON_HEADERS)
                                            .put(Fields.MESSAGE, "Client received response headers")
                                            .put(RadishGrpcFields.HEADERS, headers.toString())
                                            .build());
                        }
                        super.onHeaders(headers);
                    }

                    @Override
                    public void onClose(Status status, Metadata trailers) {
                        if (!finished.compareAndSet(false, true)) {
                            super.onClose(status, trailers);
                            return;
                        }
                        if (verbose) {
                            span.log(
                                    ImmutableMap.<String, Object>builder()
                                            .put(Fields.EVENT, RadishGrpcFields.CLIENT_CALL_LISTENER_ON_CLOSE)
                                            .put(Fields.MESSAGE, "Client call closed")
                                            .build());
                            if (!status.isOk()) {
                                RadishGrpcFields.logClientCallError(
                                        span, status.getDescription(), status.getCause());
                            }
                        }
                        RadishGrpcTags.GRPC_STATUS.set(span, status);
                        super.onClose(status, trailers);
                        span.finish();
                    }
                };
        try (Scope ignored = tracer.scopeManager().activate(span)) {
            super.start(tracingResponseListener, headers);
        }
    }

    @Override
    public void cancel(@Nullable String message, @Nullable Throwable cause) {
        if (!finished.compareAndSet(false, true)) {
            super.cancel(message, cause);
            return;
        }
        if (verbose) {
            span.log(
                    ImmutableMap.<String, Object>builder()
                            .put(Fields.EVENT, RadishGrpcFields.CLIENT_CALL_CANCEL)
                            .put(Fields.MESSAGE, "Client call canceled")
                            .build());
            RadishGrpcFields.logClientCallError(span, message, cause);
        }
        Status status = cause == null ? Status.UNKNOWN : Status.fromThrowable(cause);
        RadishGrpcTags.GRPC_STATUS.set(span, status.withDescription(message));
        try (Scope ignored = tracer.scopeManager().activate(span)) {
            super.cancel(message, cause);
        } finally {
            span.finish();
        }
    }
}
